package no.fintlabs.dispatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.model.InstanceReceiptDispatchEntity;
import no.fintlabs.repositories.InstanceReceiptDispatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collection;

@Service
@Slf4j
public class WebClientRequestService {
    private final InstanceReceiptDispatchRepository instanceReceiptDispatchRepository;
    private final WebClient webClient;

    public WebClientRequestService(InstanceReceiptDispatchRepository instanceReceiptDispatchRepository, WebClient webClient) {
        this.instanceReceiptDispatchRepository = instanceReceiptDispatchRepository;
        this.webClient = webClient;
    }

    public Collection<InstanceReceiptDispatchEntity> dispatchInstances(Collection<InstanceReceiptDispatchEntity> instanceReceiptDispatchEntities) {
        return Flux.fromIterable(instanceReceiptDispatchEntities)
                .flatMap(this::dispatchInstance)
                .onErrorContinue()
                .concatMap();
    }

    public Mono<InstanceReceiptDispatchEntity> dispatchInstance(InstanceReceiptDispatchEntity instanceReceiptDispatchEntity) {
        ObjectMapper objectMapper = new ObjectMapper();
        Object instanceToDispatch;
        try {
            instanceToDispatch = objectMapper.readValue(instanceReceiptDispatchEntity.getInstanceReceipt(), instanceReceiptDispatchEntity.getClassType());
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
        log.debug("instanceToDispatch=" + instanceReceiptDispatchEntity.getInstanceReceipt());

        return webClient.patch()
                .uri(instanceReceiptDispatchEntity.getUri())
                .body(Mono.just(instanceToDispatch), instanceReceiptDispatchEntity.getClassType())
                .retrieve()
                .bodyToMono(String.class)
                .publishOn(Schedulers.boundedElastic())// TODO: 02/11/2023 do we need this?
                .doOnSuccess(response -> log.info("success {}", response))
                .retryWhen(
                        Retry.backoff(5, Duration.ofSeconds(1))
                                .jitter(0.9)
                                .doAfterRetry(retrySignal -> log.warn("Retrying after " + retrySignal.failure().getMessage()))
                )
                .doOnError(error -> log.error("Error msg from webclient: " + error.getMessage()))
                .thenReturn(instanceReceiptDispatchEntity);
    }


}
