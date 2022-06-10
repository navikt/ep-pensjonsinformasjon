package no.nav.eessi.pensjon.pensjonsinformasjon.clients

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.eessi.pensjon.pensjonsinformasjon.PensjonsinformasjonClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.ResourceUtils
import org.springframework.web.client.RestTemplate

internal class PensjonsinformasjonClientTest{

    private var mockrestTemplate: RestTemplate = mockk()
    private lateinit var pensjonsinformasjonClient: PensjonsinformasjonClient

    @BeforeEach
    fun setup() {
        pensjonsinformasjonClient = PensjonsinformasjonClient(mockrestTemplate, PensjonRequestBuilder())
        pensjonsinformasjonClient.initMetrics()
    }

    @Test
    fun `Gitt et fnr og aktør id så skal det genereres en xml ved hentAltPaaFNR`() {

        val mockResponseEntity = createResponseEntityFromJsonFile("classpath:full-generated-response.xml")
        every { mockrestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns mockResponseEntity

        val response  = pensjonsinformasjonClient.hentAltPaaFNR("126842669854")

        assert(response.avdod != null)
        verify (exactly = 1) { mockrestTemplate.exchange("/fnr", HttpMethod.POST, any(), String::class.java) }
    }

    private fun createResponseEntityFromJsonFile(filePath: String, httpStatus: HttpStatus = HttpStatus.OK): ResponseEntity<String?> {
        val mockResponseString = ResourceUtils.getFile(filePath).readText()
        return ResponseEntity(mockResponseString, httpStatus)
    }
}

