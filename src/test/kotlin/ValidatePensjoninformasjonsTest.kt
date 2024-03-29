import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.pensjonsinformasjon.FinnSak.finnSak
import no.nav.eessi.pensjon.pensjonsinformasjon.clients.PensjonRequestBuilder
import no.nav.eessi.pensjon.pensjonsinformasjon.clients.PensjonsinformasjonClient
import no.nav.eessi.pensjon.pensjonsinformasjon.simpleFormat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.ResourceUtils
import org.springframework.web.client.RestTemplate

class ValidatePensjoninformasjonsTest {

    private var mockrestTemplate: RestTemplate = mockk()

    private lateinit var pensjonsinformasjonClient: PensjonsinformasjonClient
    @BeforeEach
    fun setup() {
        pensjonsinformasjonClient = PensjonsinformasjonClient(mockrestTemplate, PensjonRequestBuilder())
        pensjonsinformasjonClient.initMetrics()
    }
    @Test
    fun `Sjekker om pensjoninformasjon XmlCalendar kan være satt eller null sette simpleFormat`() {
        val mockResponseEntity = createResponseEntityFromJsonFile("classpath:full-generated-response.xml")

        every { mockrestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns mockResponseEntity
        val data = pensjonsinformasjonClient.hentAltPaaVedtak("1243")

        var result = data.ytelsePerMaanedListe.ytelsePerMaanedListe.first()

       assertEquals("2008-02-06", result.fom.simpleFormat())
       assertEquals("2015-08-04", result.tom?.simpleFormat())

        result = data.ytelsePerMaanedListe.ytelsePerMaanedListe.getOrNull(1)

        Assertions.assertNotNull(result)
        Assertions.assertNotNull(result.fom)

        assertEquals("2008-02-06", result.fom.simpleFormat())
        assertEquals(null, result.tom?.simpleFormat())
    }
    @Test
    fun `Sjekker om pensjoninformasjon plukk ut sak fra en liste over brukersaker`() {
        val mockResponseEntity = createResponseEntityFromJsonFile("classpath:P2100-GJENLEV-REVURDERING-M-KRAVID-INNV.xml")

        every { mockrestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns mockResponseEntity
        val data = pensjonsinformasjonClient.hentAltPaaFNR("1243")

        val sak = finnSak("22915550", data)

        assertEquals("22915550", sak?.sakId.toString())
        assertEquals("UFOREP", sak?.sakType)
        assertEquals(1, sak?.ytelsePerMaanedListe?.ytelsePerMaanedListe?.size)
        assertEquals(3, sak?.kravHistorikkListe?.kravHistorikkListe?.size)

    }

    @Test
    fun `Sjekker pensjonsinformasjon kunsaktype validerer`() {
        val mockResponseEntity = javaClass.getResource("P2100-GJENLEV-REVURDERING-M-KRAVID-INNV.xml")!!.readText()

        every { mockrestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns ResponseEntity(mockResponseEntity, HttpStatus.OK)
        val sakType = pensjonsinformasjonClient.hentKunSakTypeForFnr("22915550", "1232")

        assertEquals("22915550", sakType.sakId)
        assertEquals("UFOREP", sakType.sakType)
    }
    private fun mockAnyRequest(kravLokasjon : String) {
        val mockResponseEntity = createResponseEntityFromJsonFile(kravLokasjon)
        every { mockrestTemplate.exchange(any<String>(), any(), any(), eq(String::class.java)) } returns mockResponseEntity
    }
    private fun createResponseEntityFromJsonFile(filePath: String, httpStatus: HttpStatus = HttpStatus.OK): ResponseEntity<String?> {
        val mockResponseString = ResourceUtils.getFile(filePath).readText()
        return ResponseEntity(mockResponseString, httpStatus)
    }
}