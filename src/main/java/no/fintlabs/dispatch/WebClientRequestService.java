package no.fintlabs.dispatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.model.InstanceReceiptDispatchEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class WebClientRequestService {
    private final WebClient webClient;

    public WebClientRequestService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<InstanceReceiptDispatchEntity> dispatchInstance(
            InstanceReceiptDispatchEntity instanceReceiptDispatchEntity
    ) {
        ObjectMapper objectMapper = new ObjectMapper();
        Object instanceToDispatch;
        try {
            instanceToDispatch = objectMapper.readValue(
                    instanceReceiptDispatchEntity.getInstanceReceipt(), instanceReceiptDispatchEntity.getClassType()
            );
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }

        return webClient.patch()
                .uri(instanceReceiptDispatchEntity.getUri())
                .body(Mono.just(instanceToDispatch), instanceReceiptDispatchEntity.getClassType())
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("success {}", response))
                .doOnError(error -> log.error("Error msg from webclient: " + error.getMessage()))
                .thenReturn(instanceReceiptDispatchEntity);
    }


}
