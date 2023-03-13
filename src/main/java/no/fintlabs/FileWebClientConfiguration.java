package no.fintlabs;

import io.netty.channel.ChannelOption;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "fint.flyt.egrunnerverv")
public class FileWebClientConfiguration {


    private String baseUrl;
    private String username;
    private String password;
    private String registrationId;


    @Bean
    public ClientHttpConnector clientHttpConnector() {
        return new ReactorClientHttpConnector(HttpClient.create(
                        ConnectionProvider
                                .builder("laidback")
                                .maxLifeTime(Duration.ofMinutes(30))
                                .maxIdleTime(Duration.ofMinutes(5))
                                .build())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 300000)
                .responseTimeout(Duration.ofMinutes(5)
                ));
    }

    @Bean
    public ReactiveOAuth2AuthorizedClientManager fileAuthorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ReactiveOAuth2AuthorizedClientService authorizedClientService
    ) {
        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                        clientRegistrationRepository,
                        authorizedClientService
                );
        authorizedClientManager.setAuthorizedClientProvider(
                ReactiveOAuth2AuthorizedClientProviderBuilder
                        .builder()
                        .password()
                        .refreshToken()
                        .build()
        );

        authorizedClientManager.setContextAttributesMapper(oAuth2AuthorizeRequest -> Mono.just(Map.of(
                OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME, username,
                OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME, password
        )));

        return authorizedClientManager;
    }

    @Bean
    public WebClient fileWebClient(
            @Qualifier("fileAuthorizedClientManager") Optional<ReactiveOAuth2AuthorizedClientManager> authorizedClientManager,
            ClientHttpConnector clientHttpConnector
    ) {
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(-1))
                .build();

        WebClient.Builder webClientBuilder = WebClient.builder();

        authorizedClientManager.ifPresent(presentAuthorizedClientManager -> {
            ServerOAuth2AuthorizedClientExchangeFilterFunction authorizedClientExchangeFilterFunction =
                    new ServerOAuth2AuthorizedClientExchangeFilterFunction(presentAuthorizedClientManager);
            authorizedClientExchangeFilterFunction.setDefaultClientRegistrationId("egrunnerverv");
            webClientBuilder.filter(authorizedClientExchangeFilterFunction);
        });


        return webClientBuilder
                .clientConnector(clientHttpConnector)
                .baseUrl(baseUrl + "/x_nvas_grunnerverv_grunnerverv/496f973f47cad510f9f8e7e8036d4357?sysparm_fields=u_elements%2Carkivnummer%2Cu_opprettelse_i_elements_fullfort&sysparm_query_no_domain=true")
                .exchangeStrategies(exchangeStrategies)
                .build();
    }

}
