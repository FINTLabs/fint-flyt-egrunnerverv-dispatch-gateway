package no.fintlabs;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.model.InstanceDispatched;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class FileClient {

    private final WebClient fileWebClient;

    public FileClient(@Qualifier("fileWebClient") WebClient fileWebClient) {
        this.fileWebClient = fileWebClient;
    }

    public Mono<String> patch(InstanceDispatched instanceDispatched) {
        return fileWebClient
                .patch()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(instanceDispatched)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(1, Duration.ofSeconds(1)))
                .doOnSuccess(s -> log.info("Model patched successfully"))
                .doOnError(e -> log.error("Could not patch model=" + instanceDispatched, e));
    }

}
