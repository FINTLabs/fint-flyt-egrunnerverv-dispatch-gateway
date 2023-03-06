package no.fintlabs;

import no.fintlabs.kafka.event.EventConsumerConfiguration;
import no.fintlabs.kafka.event.EventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.time.Duration;

@Configuration
public class InstanceConsumerConfiguration {
    @Value("${fint.flyt.egrunnerverv.retentionTimeInDays:30}")
    private Long retentionTimeInDays;

    @Bean
    public ConcurrentMessageListenerContainer<String, SimpleCaseInstance> simpleCaseReceivedEventConsumer(
            EventConsumerFactoryService eventConsumerFactoryService,
            SimpleCaseInstanceRepository simpleCaseInstanceRepository,
            EventTopicService eventTopicService
    ) {
        EventTopicNameParameters topic = EventTopicNameParameters.builder()
                .eventName("egrunnerverv-case-instance")
                .build();

        eventTopicService.ensureTopic(topic, Duration.ofDays(retentionTimeInDays).toMillis());

        return eventConsumerFactoryService.createFactory(
                SimpleCaseInstance.class,
                consumerRecord -> simpleCaseInstanceRepository.put(consumerRecord.value()),
                EventConsumerConfiguration
                        .builder()
                        .seekingOffsetResetOnAssignment(true)
                        .build()
        ).createContainer(topic);
    }
}
