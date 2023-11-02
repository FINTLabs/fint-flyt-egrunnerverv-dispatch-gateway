package no.fintlabs.kafka;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.dispatch.DispatchService;
import no.fintlabs.flyt.kafka.event.InstanceFlowEventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;
import no.fintlabs.model.InstanceReceiptDispatchEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Configuration
@Slf4j
public class InstanceConsumerConfiguration {

    private final EventTopicService eventTopicService;

    private final DispatchService dispatchService;

    public InstanceConsumerConfiguration(
            EventTopicService eventTopicService,
            DispatchService dispatchService) {
        this.eventTopicService = eventTopicService;
        this.dispatchService = dispatchService;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, InstanceReceiptDispatchEntity> prepareInstanceToDispatchEventConsumer(
            InstanceFlowEventConsumerFactoryService instanceFlowEventConsumerFactoryService
    ) {
        EventTopicNameParameters topic = EventTopicNameParameters.builder()
                .eventName("instance-dispatched")
                .build();

        eventTopicService.ensureTopic(topic, 0);

        return instanceFlowEventConsumerFactoryService.createRecordFactory(
                InstanceReceiptDispatchEntity.class,
                instanceFlowConsumerRecord -> dispatchService.a(instanceFlowConsumerRecord.getInstanceFlowHeaders())
        ).createContainer(topic);
    }

}
