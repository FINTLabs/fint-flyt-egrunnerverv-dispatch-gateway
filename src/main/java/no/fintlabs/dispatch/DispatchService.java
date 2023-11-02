package no.fintlabs.dispatch;

import no.fintlabs.flyt.kafka.headers.InstanceFlowHeaders;
import no.fintlabs.mapping.InstanceFlowHeadersToInstanceHeadersEntityMappinService;
import no.fintlabs.model.InstanceHeadersEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DispatchService {

    public static final int EGRUNNERVERV_SOURCE_APPLICATION_ID = 2;
    private final InstanceReceiptService instanceReceiptService;
    private final WebClientRequestService webClientRequestService;
    private final InstanceFlowHeadersToInstanceHeadersEntityMappinService instanceFlowHeadersToInstanceHeadersEntityMappinService;

    public DispatchService(InstanceReceiptService instanceReceiptService, WebClientRequestService webClientRequestService, InstanceFlowHeadersToInstanceHeadersEntityMappinService instanceFlowHeadersToInstanceHeadersEntityMappinService) {
        this.instanceReceiptService = instanceReceiptService;
        this.webClientRequestService = webClientRequestService;
        this.instanceFlowHeadersToInstanceHeadersEntityMappinService = instanceFlowHeadersToInstanceHeadersEntityMappinService;
    }

    public synchronized void a(InstanceFlowHeaders instanceFlowHeaders) {
        if (instanceFlowHeaders.getSourceApplicationId() != EGRUNNERVERV_SOURCE_APPLICATION_ID) {
            return;
        }

        InstanceHeadersEntity instanceHeadersEntity = instanceFlowHeadersToInstanceHeadersEntityMappinService.map(instanceFlowHeaders);

        instanceReceiptService.handleNewInstanceToDispatch(instanceHeadersEntity);


        instanceReceiptService.delete(instanceHeadersEntity);
    }

    @Scheduled(
            initialDelayString = "${fint.flyt.egrunnerverv.instance-dispatch-initial-delay}",
            fixedDelayString = "${fint.flyt.egrunnerverv.instance-dispatch-fixed-delay}"
    )
    private synchronized void b() {
        instanceReceiptService.prepareInstancesToDispatch();
        webClientRequestService.dispatchInstances();
    }

}
