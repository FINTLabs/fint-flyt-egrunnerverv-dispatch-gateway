package no.fintlabs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.model.InstanceToDispatchEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
@Slf4j
public class WebClientRequestService {
    private final InstanceToDispatchEntityRepository instanceToDispatchEntityRepository;
    private final WebClient webClient;

    public WebClientRequestService(InstanceToDispatchEntityRepository instanceToDispatchEntityRepository, WebClient webClient) {
        this.instanceToDispatchEntityRepository = instanceToDispatchEntityRepository;
        this.webClient = webClient;
    }

    @Scheduled(cron = "${fint.flyt.egrunnerverv.instance-dispatch-interval-cron}")
    public void dispatchInstances() {

        instanceToDispatchEntityRepository.findAll()
                .forEach(this::dispatchInstance);
    }

    public boolean dispatchInstance(InstanceToDispatchEntity instanceToDispatchEntity) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Object instanceToDispatch;
            instanceToDispatch = objectMapper.readValue(instanceToDispatchEntity.getInstanceToDispatch(), instanceToDispatchEntity.getClassType());

            webClient.patch()
                    .uri(instanceToDispatchEntity.getUri())
                    .body(instanceToDispatch, instanceToDispatchEntity.getClassType())
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(success -> log.info("success {}", success))
                    .doOnError(error -> {
                        log.info("Error msg from webclient: " + error.getMessage());
                    })
                    .retryWhen(
                            Retry.backoff(5, Duration.ofSeconds(1))
                                    .jitter(0.9)
                                    .doAfterRetry(retrySignal -> {
                                        log.debug("Retrying after " + retrySignal.failure().getMessage());
                                    })
                    )
                    .block();

        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);


            return false;
        }

        return true;
    }


}
