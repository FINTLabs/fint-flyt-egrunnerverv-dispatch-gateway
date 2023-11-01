package no.fintlabs;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.flyt.kafka.event.InstanceFlowEventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;
import no.fintlabs.model.InstanceToDispatchEntity;
import no.fintlabs.model.PrepareInstanceToDispatchEntity;
import no.fintlabs.repositories.PrepareInstanceToDispatchRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Configuration
@Slf4j
public class InstanceConsumerConfiguration {
    public static final int EGRUNNERVERV_SOURCE_APPLICATION_ID = 2;
    private final EventTopicService eventTopicService;
    private final PrepareInstanceToDispatchService prepareInstanceToDispatchService;
    private final PrepareInstanceToDispatchRepository prepareInstanceToDispatchRepository;


    public InstanceConsumerConfiguration(
            EventTopicService eventTopicService,
            PrepareInstanceToDispatchService prepareInstanceToDispatchService,
            PrepareInstanceToDispatchRepository prepareInstanceToDispatchRepository
    ) {
        this.eventTopicService = eventTopicService;
        this.prepareInstanceToDispatchService = prepareInstanceToDispatchService;
        this.prepareInstanceToDispatchRepository = prepareInstanceToDispatchRepository;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, InstanceToDispatchEntity> prepareInstanceToDispatchEventConsumer(
            InstanceFlowEventConsumerFactoryService instanceFlowEventConsumerFactoryService
    ) {
        EventTopicNameParameters topic = EventTopicNameParameters.builder()
                .eventName("instance-dispatched")
                .build();

        eventTopicService.ensureTopic(topic, 0);

        return instanceFlowEventConsumerFactoryService.createRecordFactory(
                InstanceToDispatchEntity.class,
                instanceFlowConsumerRecord -> {
                    Long sourceApplicationId = instanceFlowConsumerRecord.getInstanceFlowHeaders().getSourceApplicationId();

                    if (sourceApplicationId == EGRUNNERVERV_SOURCE_APPLICATION_ID) {
                        PrepareInstanceToDispatchEntity prepareInstanceToDispatchEntity = PrepareInstanceToDispatchEntity
                                .builder()
                                .sourceApplicationIntegrationId(instanceFlowConsumerRecord.getInstanceFlowHeaders().getSourceApplicationIntegrationId())
                                .sourceApplicationInstanceId(instanceFlowConsumerRecord.getInstanceFlowHeaders().getSourceApplicationInstanceId())
                                .archiveInstanceId(instanceFlowConsumerRecord.getInstanceFlowHeaders().getArchiveInstanceId())
                                .build();

                        prepareInstanceToDispatchRepository.save(prepareInstanceToDispatchEntity);
                        prepareInstanceToDispatchService.prepareInstanceToDispatch(prepareInstanceToDispatchEntity);
                    }
                }
        ).createContainer(topic);
    }

}
