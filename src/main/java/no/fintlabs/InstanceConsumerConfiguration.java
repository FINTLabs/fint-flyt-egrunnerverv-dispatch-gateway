package no.fintlabs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.arkiv.noark.SakResource;
import no.fintlabs.flyt.kafka.event.InstanceFlowEventConsumerFactoryService;
import no.fintlabs.kafka.CaseRequestService;
import no.fintlabs.kafka.event.EventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;
import no.fintlabs.model.EgrunnervervJournalpostInstanceToDispatch;
import no.fintlabs.model.EgrunnervervSakInstanceToDispatch;
import no.fintlabs.model.EgrunnervervSimpleInstance;
import no.fintlabs.model.InstanceToDispatchEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Configuration
@Slf4j
public class InstanceConsumerConfiguration {
    public static final int EGRUNNERVERV_ID = 2;
    public static final String EGRUNNERVERV_DATETIME_FORMAT = "dd-MM-yyyy HH:mm:ss";
    @Value("${fint.flyt.egrunnerverv.retentionTimeInDays:30}")
    private Long retentionTimeInDays;

    private final EgrunnervervSimpleInstanceRepository egrunnervervSimpleInstanceRepository;
    private final InstanceToDispatchEntityRepository instanceToDispatchEntityRepository;
    private final EventTopicService eventTopicService;

    private final CaseRequestService caseRequestService;
    private final EgrunnervervJournalpostInstanceToDispatchMappingService journalpostInstanceToDispatchMappingService;

    private final WebClientRequestService webClientRequestService;

    private final ObjectMapper objectMapper;

    public InstanceConsumerConfiguration(
            EgrunnervervSimpleInstanceRepository egrunnervervSimpleInstanceRepository,
            InstanceToDispatchEntityRepository instanceToDispatchEntityRepository,
            EventTopicService eventTopicService,
            CaseRequestService caseRequestService,
            EgrunnervervJournalpostInstanceToDispatchMappingService journalpostInstanceToDispatchMappingService,
            WebClientRequestService webClientRequestService
    ) {
        this.egrunnervervSimpleInstanceRepository = egrunnervervSimpleInstanceRepository;
        this.instanceToDispatchEntityRepository = instanceToDispatchEntityRepository;
        this.eventTopicService = eventTopicService;
        this.caseRequestService = caseRequestService;
        this.journalpostInstanceToDispatchMappingService = journalpostInstanceToDispatchMappingService;
        this.webClientRequestService = webClientRequestService;
        this.objectMapper = new ObjectMapper();
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, EgrunnervervSimpleInstance> simpleSakReceivedEventConsumer(
            EventConsumerFactoryService eventConsumerFactoryService
    ) {
        EventTopicNameParameters topic = EventTopicNameParameters.builder()
                .eventName("egrunnerverv-sak-instance")
                .build();

        eventTopicService.ensureTopic(topic, Duration.ofDays(retentionTimeInDays).toMillis());

        return eventConsumerFactoryService.createFactory(
                EgrunnervervSimpleInstance.class,
                consumerRecord -> egrunnervervSimpleInstanceRepository.put(
                        consumerRecord.value()
                                .toBuilder()
                                .type(EgrunnervervSimpleInstance.Type.SAK)
                                .build()
                )).createContainer(topic);
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, EgrunnervervSimpleInstance> simpleJournalpostReceivedEventConsumer(
            EventConsumerFactoryService eventConsumerFactoryService
    ) {
        EventTopicNameParameters topic = EventTopicNameParameters.builder()
                .eventName("egrunnerverv-journalpost-instance")
                .build();

        eventTopicService.ensureTopic(topic, Duration.ofDays(retentionTimeInDays).toMillis());

        return eventConsumerFactoryService.createFactory(
                EgrunnervervSimpleInstance.class,
                consumerRecord -> egrunnervervSimpleInstanceRepository.put(
                        consumerRecord.value()
                                .toBuilder()
                                .type(EgrunnervervSimpleInstance.Type.JOURNALPOST)
                                .build()
                )
        ).createContainer(topic);
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, InstanceToDispatchEntity> instanceToDispatchEventConsumer(
            InstanceFlowEventConsumerFactoryService instanceFlowEventConsumerFactoryService
    ) {
        EventTopicNameParameters topic = EventTopicNameParameters.builder()
                .eventName("instance-dispatched")
                .build();

        eventTopicService.ensureTopic(topic, 0);

        return instanceFlowEventConsumerFactoryService.createFactory(
                InstanceToDispatchEntity.class,
                instanceFlowConsumerRecord -> {
                    Long sourceApplicationId = instanceFlowConsumerRecord.getInstanceFlowHeaders().getSourceApplicationId();

                    if (sourceApplicationId == EGRUNNERVERV_ID) {
                        String sourceApplicationInstanceId = instanceFlowConsumerRecord.getInstanceFlowHeaders().getSourceApplicationInstanceId();
                        egrunnervervSimpleInstanceRepository.get(sourceApplicationInstanceId).ifPresent(simpleInstance -> {
                            try {
                                Optional<InstanceToDispatchEntity> instanceToDispatchEntity =
                                        switch (simpleInstance.getType()) {
                                            case SAK -> storeSakInstanceToDispatch(
                                                    simpleInstance.getTableName(),
                                                    sourceApplicationInstanceId,
                                                    instanceFlowConsumerRecord.getInstanceFlowHeaders().getArchiveInstanceId()
                                            );
                                            case JOURNALPOST -> storeJournalpostInstanceToDispatch(
                                                    simpleInstance.getTableName(),
                                                    sourceApplicationInstanceId,
                                                    instanceFlowConsumerRecord.getInstanceFlowHeaders().getArchiveInstanceId()
                                            );
                                        };
                                instanceToDispatchEntity.ifPresent(webClientRequestService::dispatchInstance);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        });

                    }
                },
                new CommonLoggingErrorHandler(),
                false
        ).createContainer(topic);
    }

    private Optional<InstanceToDispatchEntity> storeJournalpostInstanceToDispatch(
            String tableName,
            String sourceApplicationInstanceId,
            String archiveInstanceId
    ) throws JsonProcessingException {
        String[] splitArchiveInstanceId = archiveInstanceId.split("-");
        String caseId = splitArchiveInstanceId[0];
        Long journalpostNummer = Long.parseLong(
                splitArchiveInstanceId[1]
                        .replace("[", "")
                        .replace("]", "")
        );
        SakResource sakResource = caseRequestService.getByMappeId(caseId).orElseThrow();
        EgrunnervervJournalpostInstanceToDispatch egrunnervervJournalpostInstanceToDispatch =
                journalpostInstanceToDispatchMappingService.map(sakResource, journalpostNummer);

        String uri = UriComponentsBuilder.newInstance()
                .pathSegment(
                        tableName,
                        sourceApplicationInstanceId
                )
                .queryParam("sysparm_query_no_domain", "true")
                .toUriString();

        InstanceToDispatchEntity instanceToDispatchEntity = InstanceToDispatchEntity.builder()
                .sourceApplicationInstanceId(sourceApplicationInstanceId)
                .instanceToDispatch(objectMapper.writeValueAsString(egrunnervervJournalpostInstanceToDispatch))
                .classType(EgrunnervervJournalpostInstanceToDispatch.class)
                .uri(uri)
                .build();

        instanceToDispatchEntityRepository.save(instanceToDispatchEntity);
        return Optional.of(instanceToDispatchEntity);
    }

    private Optional<InstanceToDispatchEntity> storeSakInstanceToDispatch(
            String tableName,
            String sourceApplicationInstanceId,
            String archiveInstanceId
    ) throws JsonProcessingException {
        SakResource sakResource = caseRequestService.getByMappeId(archiveInstanceId).orElseThrow();

        EgrunnervervSakInstanceToDispatch egrunnervervSakInstanceToDispatch = EgrunnervervSakInstanceToDispatch.builder()
                .arkivnummer(archiveInstanceId)
                .opprettelse_i_elements_fullfort(
                        sakResource
                                .getOpprettetDato()
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
                                .format(DateTimeFormatter.ofPattern(EGRUNNERVERV_DATETIME_FORMAT))
                )
                .build();

        String uri = UriComponentsBuilder.newInstance()
                .pathSegment(
                        tableName,
                        sourceApplicationInstanceId
                )
                .queryParam("sysparm_query_no_domain", "true")
                .toUriString();

        InstanceToDispatchEntity instanceToDispatchEntity = InstanceToDispatchEntity.builder()
                .sourceApplicationInstanceId(sourceApplicationInstanceId)
                .instanceToDispatch(objectMapper.writeValueAsString(egrunnervervSakInstanceToDispatch))
                .classType(EgrunnervervSakInstanceToDispatch.class)
                .uri(uri)
                .build();

        instanceToDispatchEntityRepository.save(instanceToDispatchEntity);
        return Optional.of(instanceToDispatchEntity);
    }

}
