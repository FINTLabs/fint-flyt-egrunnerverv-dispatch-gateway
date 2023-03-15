package no.fintlabs;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.flyt.kafka.InstanceFlowConsumerRecord;
import no.fintlabs.flyt.kafka.event.InstanceFlowEventConsumerFactoryService;
import no.fintlabs.kafka.event.EventConsumerConfiguration;
import no.fintlabs.kafka.event.EventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;
import no.fintlabs.model.InstanceDispatched;
import no.fintlabs.model.SimpleCaseInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

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

    final static Logger logger = LoggerFactory.getLogger(InstanceConsumerConfiguration.class);


    private final WebClient webClient;

    public InstanceConsumerConfiguration(WebClient webClient) {
        this.webClient = webClient;
    }

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

    @Bean
    public ConcurrentMessageListenerContainer<String, InstanceDispatched> instanceDispatchedEventConsumer(
            InstanceFlowEventConsumerFactoryService instanceFlowEventConsumerFactoryService,
            SimpleCaseInstanceRepository simpleCaseInstanceRepository,
            EventTopicService eventTopicService
    ) {
        EventTopicNameParameters topic = EventTopicNameParameters.builder()
                .eventName("instance-dispatched")
                .build();

        eventTopicService.ensureTopic(topic, Duration.ofDays(retentionTimeInDays).toMillis());

        return instanceFlowEventConsumerFactoryService.createFactory(
                InstanceDispatched.class,
                instanceFlowConsumerRecord -> {
                    dispatchInstance(simpleCaseInstanceRepository, instanceFlowConsumerRecord);
                },
                new CommonLoggingErrorHandler(),
                false
        ).createContainer(topic);
    }

    private void dispatchInstance(SimpleCaseInstanceRepository simpleCaseInstanceRepository, InstanceFlowConsumerRecord<InstanceDispatched> instanceFlowConsumerRecord) {
        instanceFlowConsumerRecord.getInstanceFlowHeaders();

        if (instanceFlowConsumerRecord.getInstanceFlowHeaders().getSourceApplicationId() == EGRUNNERVERV_ID) {

            Optional<SimpleCaseInstance> simpleCaseInstance = simpleCaseInstanceRepository.get(instanceFlowConsumerRecord.getInstanceFlowHeaders().getSourceApplicationInstanceId());

            if (simpleCaseInstance.isPresent()) {

                InstanceDispatched instanceDispatched = InstanceDispatched.builder()
                        .archiveInstanceId(instanceFlowConsumerRecord.getInstanceFlowHeaders().getArchiveInstanceId())
                        .archivedTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern(EGRUNNERVERV_DATETIME_FORMAT)))
                        .build();

                String uri = UriComponentsBuilder.newInstance()
                        .pathSegment(
                                simpleCaseInstance.get().getTableName(),
                                instanceFlowConsumerRecord.getInstanceFlowHeaders().getSourceApplicationInstanceId()
                        )

                        .queryParam("sysparm_fields", "u_elements,arkivnummer,u_opprettelse_i_elements_fullfort")
                        .queryParam("sysparm_query_no_domain", "true").toUriString();

                log.debug("Patching case on uri: {}", uri);

                webClient.patch().uri(uri)
                        .body(Mono.just(instanceDispatched), InstanceDispatched.class)
                        .retrieve()
                        .bodyToMono(String.class)
                        .doOnError(error -> log.info("Error msg from webclient: " + error.getMessage()))
                        .retryWhen(
                                Retry.backoff(5, Duration.ofSeconds(1))
                                        .jitter(0.9)
                                        .doAfterRetry(retrySignal -> {
                                            logger.warn("Retrying after " + retrySignal.failure().getMessage());
                                            log.debug("Retrying after " + retrySignal.failure().getMessage());
                                        })
                        )
                        .block();
            }
        }
    }

}
