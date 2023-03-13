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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import reactor.core.publisher.Mono;

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


    private final FileClient webClient;

    public InstanceConsumerConfiguration(FileClient webClient) {
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

            log.info(String.valueOf(instanceFlowConsumerRecord.getInstanceFlowHeaders().getSourceApplicationId()));
            log.info(String.valueOf(instanceFlowConsumerRecord.getInstanceFlowHeaders().getSourceApplicationInstanceId()));
            log.info(String.valueOf(instanceFlowConsumerRecord.getInstanceFlowHeaders().getArchiveInstanceId()));


            Optional<SimpleCaseInstance> simpleCaseInstance = simpleCaseInstanceRepository.get(instanceFlowConsumerRecord.getInstanceFlowHeaders().getSourceApplicationInstanceId());


            if (simpleCaseInstance.isPresent()) {


                InstanceDispatched instanceDispatched = InstanceDispatched.builder()
                        .archiveInstanceId(instanceFlowConsumerRecord.getInstanceFlowHeaders().getArchiveInstanceId())
                        .archivedTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern(EGRUNNERVERV_DATETIME_FORMAT)))
                        .build();

log.info("test");
                webClient.patch(instanceDispatched).block();

//                String block = webClient.patch().uri("/x_nvas_grunnerverv_grunnerverv/496f973f47cad510f9f8e7e8036d4357?sysparm_fields=u_elements%2Carkivnummer%2Cu_opprettelse_i_elements_fullfort&sysparm_query_no_domain=true")
//                        .body(Mono.just(instanceDispatched), InstanceDispatched.class)
//                        .retrieve()
//                        .bodyToMono(String.class)
//                        .doOnError(error -> log.info("Error msg from webclient: " + error.getMessage()))
//                        .block();
//

//                {
//                    "arkivnummer":"2022/179",
//                        "u_elements":"https://prod01.elementscloud.no/Elements/rm/915488099_PROD-915488099/#nav=/cases/5099/registryEntries",
//                        "u_opprettelse_i_elements_fullfort":"08-12-2022 11:50:36"
//                }

                //Mono<String> result = instancePostRequestService.doPatch(instanceDispatched.toString(), simpleCaseInstance.get().getTableName(), instanceDispatched.getArchiveInstanceId());
                // instancePostRequestService.doPatch();


                //Mono<String> result = instancePostRequestService.getToken();

                // log.info(String.valueOf(result.block()));

                //result.doOnSuccess(success -> log.info("Success " + success));

                // String result2 = result.block();

                //System.out.println("Result: " + result2);


            }

            //https://vigoikstest.service-now.com/api/now/table/x_nvas_grunnerverv_grunnerverv/496f973f47cad510f9f8e7e8036d4357?sysparm_fields=u_elements%2Carkivnummer%2Cu_opprettelse_i_elements_fullfort&sysparm_query_no_domain=true

        }
    }

}
