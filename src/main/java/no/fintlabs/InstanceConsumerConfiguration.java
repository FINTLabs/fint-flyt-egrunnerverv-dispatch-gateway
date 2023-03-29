package no.fintlabs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.flyt.kafka.event.InstanceFlowEventConsumerFactoryService;
import no.fintlabs.kafka.event.EventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;
import no.fintlabs.model.EgrunnervervSakInstanceToDispatch;
import no.fintlabs.model.InstanceToDispatchEntity;
import no.fintlabs.model.EgrunnervervSimpleInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
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

    private final WebClientRequestService webClientRequestService;

    public InstanceConsumerConfiguration(
            EgrunnervervSimpleInstanceRepository egrunnervervSimpleInstanceRepository,
            InstanceToDispatchEntityRepository instanceToDispatchEntityRepository,
            EventTopicService eventTopicService,
            WebClientRequestService webClientRequestService
    ) {
        this.egrunnervervSimpleInstanceRepository = egrunnervervSimpleInstanceRepository;
        this.instanceToDispatchEntityRepository = instanceToDispatchEntityRepository;
        this.eventTopicService = eventTopicService;
        this.webClientRequestService = webClientRequestService;
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
                consumerRecord -> egrunnervervSimpleInstanceRepository.put(consumerRecord.value())
        ).createContainer(topic);
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
                consumerRecord -> egrunnervervSimpleInstanceRepository.put(consumerRecord.value())
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
                        Optional<EgrunnervervSimpleInstance> simpleCaseInstance = egrunnervervSimpleInstanceRepository.get(sourceApplicationInstanceId);
                        if (simpleCaseInstance.isPresent()) {
                            try {
                                Optional<InstanceToDispatchEntity> instanceToDispatchEntity =
                                        storeInstanceToDispatch(
                                                simpleCaseInstance.get().getTableName(),
                                                sourceApplicationInstanceId,
                                                instanceFlowConsumerRecord.getInstanceFlowHeaders().getArchiveInstanceId()
                                        );
                                instanceToDispatchEntity.ifPresent(webClientRequestService::dispatchInstance);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                },
                new CommonLoggingErrorHandler(),
                false
        ).createContainer(topic);
    }


    private Optional<InstanceToDispatchEntity> storeInstanceToDispatch(
            String tableName,
            String sourceApplicationInstanceId,
            String archiveInstanceId
    ) throws JsonProcessingException
    {
        EgrunnervervSakInstanceToDispatch egrunnervervSakInstanceToDispatch = EgrunnervervSakInstanceToDispatch.builder()
                .archiveInstanceId(archiveInstanceId)
                .archivedTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern(EGRUNNERVERV_DATETIME_FORMAT)))
                .build();

        String uri = UriComponentsBuilder.newInstance()
                .pathSegment(
                        tableName,
                        sourceApplicationInstanceId
                )
                .queryParam("sysparm_fields", "u_elements,arkivnummer,u_opprettelse_i_elements_fullfort")
                .queryParam("sysparm_query_no_domain", "true").toUriString();

        ObjectMapper objectMapper = new ObjectMapper();

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
