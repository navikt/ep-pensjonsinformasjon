package no.nav.eessi.pensjon.services.pensjonsinformasjon

import no.nav.eessi.pensjon.logging.RequestResponseLoggerInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

/**
 * Rest template for PESYS pensjonsinformasjon
 */

@Configuration
class PensjonsinformasjonRestTemplate {

    @Value("\${pensjonsinformasjon.url}")
    lateinit var pensjonUrl: String

    @Bean
    fun pensjoninformasjonRestTemplate(): RestTemplate {
        return RestTemplateBuilder()
                .rootUri(pensjonUrl)
                .additionalInterceptors(
                        RequestResponseLoggerInterceptor()
                )
               .build().apply {
                    requestFactory = BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory())
                }
    }
}
