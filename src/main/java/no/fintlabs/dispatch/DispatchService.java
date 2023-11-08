package no.fintlabs.dispatch;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.converting.InstanceFlowHeadersToInstanceHeadersEntityConvertingService;
import no.fintlabs.converting.InstanceHeadersEntityToInstanceReceiptDispatchEntityConvertingService;
import no.fintlabs.flyt.kafka.headers.InstanceFlowHeaders;
import no.fintlabs.model.InstanceHeadersEntity;
import no.fintlabs.model.InstanceReceiptDispatchEntity;
import no.fintlabs.repositories.InstanceHeadersRepository;
import no.fintlabs.repositories.InstanceReceiptDispatchRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class DispatchService {

    public static final int EGRUNNERVERV_SOURCE_APPLICATION_ID = 2;
    private final WebClientRequestService webClientRequestService;
    private final InstanceFlowHeadersToInstanceHeadersEntityConvertingService
            instanceFlowHeadersToInstanceHeadersEntityConvertingService;
    private final InstanceHeadersEntityToInstanceReceiptDispatchEntityConvertingService
            instanceHeadersEntityToInstanceReceiptDispatchEntityConvertingService;
    private final InstanceHeadersRepository instanceHeadersRepository;
    private final InstanceReceiptDispatchRepository instanceReceiptDispatchRepository;
    private final Sinks.Many<InstanceHeadersEntity> newInstanceHeadersEntitySink;

    public DispatchService(
            WebClientRequestService webClientRequestService,
            InstanceFlowHeadersToInstanceHeadersEntityConvertingService instanceFlowHeadersToInstanceHeadersEntityConvertingService,
            InstanceHeadersEntityToInstanceReceiptDispatchEntityConvertingService instanceHeadersEntityToInstanceReceiptDispatchEntityConvertingService,
            InstanceHeadersRepository instanceHeadersRepository,
            InstanceReceiptDispatchRepository instanceReceiptDispatchRepository
    ) {
        this.webClientRequestService = webClientRequestService;
        this.instanceFlowHeadersToInstanceHeadersEntityConvertingService =
                instanceFlowHeadersToInstanceHeadersEntityConvertingService;
        this.instanceHeadersEntityToInstanceReceiptDispatchEntityConvertingService =
                instanceHeadersEntityToInstanceReceiptDispatchEntityConvertingService;
        this.instanceHeadersRepository = instanceHeadersRepository;
        this.instanceReceiptDispatchRepository = instanceReceiptDispatchRepository;
        newInstanceHeadersEntitySink = Sinks.many().unicast().onBackpressureBuffer();
        newInstanceHeadersEntitySink.asFlux().subscribe(this::dispatch);
    }


    public void handleNewInstance(InstanceFlowHeaders instanceFlowHeaders) {
        if (instanceFlowHeaders.getSourceApplicationId() != EGRUNNERVERV_SOURCE_APPLICATION_ID) {
            return;
        }

        InstanceHeadersEntity instanceHeadersEntity = saveInstanceHeaders(instanceFlowHeaders);
        newInstanceHeadersEntitySink.tryEmitNext(instanceHeadersEntity);
    }

    public synchronized void dispatch(InstanceHeadersEntity instanceHeadersEntity) {
        convertAndTransferToInstanceReceiptDispatch(instanceHeadersEntity)
                .ifPresent(this::dispatchInstanceReceipt);
    }

    @Scheduled(
            initialDelayString = "${fint.flyt.egrunnerverv.instance-dispatch-initial-delay}",
            fixedDelayString = "${fint.flyt.egrunnerverv.instance-dispatch-fixed-delay}"
    )
    private synchronized void dispatchAll() {

        List<InstanceHeadersEntity> instanceHeadersEntities = instanceHeadersRepository.findAll();

        if (!instanceHeadersEntities.isEmpty()) {
            log.info("Converting and transferring all instance header entities to instance receipt dispatch entities");
            instanceHeadersRepository.findAll().forEach(
                    this::convertAndTransferToInstanceReceiptDispatch
            );
        }

        List<InstanceReceiptDispatchEntity> instanceReceipts = instanceReceiptDispatchRepository.findAll();

        if (!instanceReceipts.isEmpty()) {
            log.info("Dispatching all instance receipt dispatch entities");
            instanceReceipts.forEach(
                    this::dispatchInstanceReceipt
            );
        }
    }

    private InstanceHeadersEntity saveInstanceHeaders(InstanceFlowHeaders instanceFlowHeaders) {
        InstanceHeadersEntity instanceHeadersEntity = instanceFlowHeadersToInstanceHeadersEntityConvertingService
                .convert(instanceFlowHeaders);
        log.info(
                "Saving InstanceHeadersEntity for sourceApplicationInstanceId={}",
                instanceFlowHeaders.getSourceApplicationInstanceId()
        );
        instanceHeadersRepository.save(instanceHeadersEntity);
        return instanceHeadersEntity;
    }

    private Optional<InstanceReceiptDispatchEntity> convertAndTransferToInstanceReceiptDispatch(
            InstanceHeadersEntity instanceHeadersEntity
    ) {

        String sourceApplicationInstanceId = instanceHeadersEntity.getSourceApplicationInstanceId();

        log.info(
                "Converting InstanceHeadersEntity to InstanceReceiptDispatchEntity for sourceApplicationInstanceId={}",
                sourceApplicationInstanceId
        );

        try {
            Optional<InstanceReceiptDispatchEntity> instanceReceiptDispatchEntity =
                    instanceHeadersEntityToInstanceReceiptDispatchEntityConvertingService.convert(instanceHeadersEntity)
                            .map(entity -> {
                                log.info(
                                        "Saving InstanceReceiptDispatchEntity for sourceApplicationInstanceId={}",
                                        sourceApplicationInstanceId
                                );
                                return instanceReceiptDispatchRepository.save(entity);
                            });

            if (instanceReceiptDispatchEntity.isPresent()) {
                log.info(
                        "Deleting InstanceHeadersEntity for sourceApplicationInstanceId={}",
                        sourceApplicationInstanceId
                );
                instanceHeadersRepository.delete(instanceHeadersEntity);
                log.info(
                        "Successfully converted and transferred InstanceHeadersEntity to" +
                                " InstanceReceiptDispatchEntity for sourceApplicationInstanceId={}",
                        sourceApplicationInstanceId
                );
            }

            return instanceReceiptDispatchEntity;
        } catch (RuntimeException e) {
            log.error("Converting and transferring of InstanceHeadersEntity to InstanceReceiptDispatchEntity failed" +
                    " for sourceApplicationInstanceId={}", sourceApplicationInstanceId, e);
        }

        return Optional.empty();
    }

    private void dispatchInstanceReceipt(InstanceReceiptDispatchEntity instanceReceiptDispatchEntity) {

        String sourceApplicationInstanceId = instanceReceiptDispatchEntity.getSourceApplicationInstanceId();

        log.info(
                "Dispatching InstanceReceiptDispatchEntity for sourceApplicationInstanceId={}",
                sourceApplicationInstanceId
        );
        log.debug("Instance receipt={}", instanceReceiptDispatchEntity.getInstanceReceipt());
        webClientRequestService
                .dispatchInstance(instanceReceiptDispatchEntity)
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(entity -> {
                            log.info(
                                    "Dispatch request accepted by eGrunnerverv for sourceApplicationInstanceId={}",
                                    sourceApplicationInstanceId
                            );
                            log.info(
                                    "Deleting InstanceReceiptDispatchEntity for sourceApplicationInstanceId={}",
                                    sourceApplicationInstanceId
                            );
                            instanceReceiptDispatchRepository.delete(entity);
                        }
                )
                .doOnError(e -> log.warn(
                        "Dispatch request failed for sourceApplicationInstanceId={}",
                        sourceApplicationInstanceId, e
                ))
                .onErrorComplete()
                .block();
    }
}
