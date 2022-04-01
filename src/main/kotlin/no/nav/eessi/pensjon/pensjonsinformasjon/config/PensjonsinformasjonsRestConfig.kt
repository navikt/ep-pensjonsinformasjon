package no.nav.eessi.pensjon.pensjonsinformasjon.config

import no.nav.eessi.pensjon.logging.RequestResponseLoggerInterceptor
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

/**
 * Rest template for PESYS pensjonsinformasjon
 */

@Configuration
class PensjonsinformasjonsRestConfig {

//    @Value("\${pensjonsinformasjon.url}")
//    lateinit var pensjonUrl: String

    @Bean
    @Profile("!Prod","!Test")
    fun pensjoninformasjonRestTemplate(): RestTemplate {
        return RestTemplateBuilder()
                .rootUri("localhost:8080")
                .additionalInterceptors(
                        RequestResponseLoggerInterceptor()
                )
               .build().apply {
                    requestFactory = BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory())
                }
    }

}
