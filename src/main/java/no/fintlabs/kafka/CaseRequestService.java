package no.fintlabs.kafka;

import no.fint.model.resource.arkiv.noark.SakResource;
import no.fintlabs.kafka.common.topic.TopicCleanupPolicyParameters;
import no.fintlabs.kafka.requestreply.RequestProducer;
import no.fintlabs.kafka.requestreply.RequestProducerConfiguration;
import no.fintlabs.kafka.requestreply.RequestProducerFactory;
import no.fintlabs.kafka.requestreply.RequestProducerRecord;
import no.fintlabs.kafka.requestreply.topic.ReplyTopicNameParameters;
import no.fintlabs.kafka.requestreply.topic.ReplyTopicService;
import no.fintlabs.kafka.requestreply.topic.RequestTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class CaseRequestService {

    private final RequestProducer<String, SakResource> requestProducer;
    private final RequestTopicNameParameters requestTopicNameParameters;

    public CaseRequestService(
            @Value("${fint.kafka.application-id}") String applicationId,
            ReplyTopicService replyTopicService,
            RequestProducerFactory requestProducerFactory
    ) {
        requestTopicNameParameters = RequestTopicNameParameters.builder()
                .resource("arkiv.noark.sak")
                .parameterName("mappeid")
                .build();

        ReplyTopicNameParameters replyTopicNameParameters = ReplyTopicNameParameters.builder()
                .applicationId(applicationId)
                .resource("arkiv.noark.sak")
                .build();

        replyTopicService.ensureTopic(replyTopicNameParameters, 0, TopicCleanupPolicyParameters.builder().build());

        this.requestProducer = requestProducerFactory.createProducer(
                replyTopicNameParameters,
                String.class,
                SakResource.class,
                RequestProducerConfiguration
                        .builder()
                        .defaultReplyTimeout(Duration.ofSeconds(60))
                        .build()
        );
    }

    public Optional<SakResource> getByMappeId(String mappeId) {
        return requestProducer.requestAndReceive(
                RequestProducerRecord.<String>builder()
                        .topicNameParameters(requestTopicNameParameters)
                        .value(mappeId)
                        .build()
        ).map(ConsumerRecord::value);
    }

}
