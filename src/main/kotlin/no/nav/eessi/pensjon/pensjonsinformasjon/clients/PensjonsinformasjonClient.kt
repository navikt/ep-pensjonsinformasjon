package no.nav.eessi.pensjon.pensjonsinformasjon


import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.pensjonsinformasjon.FinnSak.finnSak
import no.nav.eessi.pensjon.pensjonsinformasjon.clients.PensjonRequestBuilder
import no.nav.eessi.pensjon.pensjonsinformasjon.clients.simpleFormat
import no.nav.eessi.pensjon.services.pensjonsinformasjon.Pensjontype
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheConfig
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder
import java.io.StringReader
import javax.annotation.PostConstruct
import javax.xml.bind.JAXBContext
import javax.xml.transform.stream.StreamSource


@Component
@CacheConfig(cacheNames = ["PensjonsinformasjonClient"])
class PensjonsinformasjonClient(
    private val pensjoninformasjonRestTemplate: RestTemplate,
    private val pensjonRequestBuilder: PensjonRequestBuilder,
    @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper(SimpleMeterRegistry())) {

    private val logger = LoggerFactory.getLogger(PensjonsinformasjonClient::class.java)

    private lateinit var pensjoninformasjonHentKunSakType: MetricsHelper.Metric
    private lateinit var pensjoninformasjonHentAltPaaIdent: MetricsHelper.Metric
    private lateinit var pensjoninformasjonAltPaaVedtak: MetricsHelper.Metric
    private lateinit var pensjoninformasjonHentAltPaaIdentRequester: MetricsHelper.Metric
    private lateinit var pensjoninformasjonAltPaaVedtakRequester: MetricsHelper.Metric

    private enum class REQUESTPATH(val path: String) {
        FNR("/fnr"),
        VEDTAK("/vedtak"),
        AKTOR("/aktor");
    }

    @PostConstruct
    fun initMetrics() {
        pensjoninformasjonHentKunSakType = metricsHelper.init("PensjoninformasjonHentKunSakType")
        pensjoninformasjonHentAltPaaIdent = metricsHelper.init("PensjoninformasjonHentAltPaaIdent")
        pensjoninformasjonAltPaaVedtak = metricsHelper.init("PensjoninformasjonAltPaaVedtak")
        pensjoninformasjonHentAltPaaIdentRequester = metricsHelper.init("PensjoninformasjonHentAltPaaIdentRequester")
        pensjoninformasjonAltPaaVedtakRequester = metricsHelper.init("PensjoninformasjonAltPaaVedtakRequester")
    }

    fun hentKunSakType(sakId: String, aktoerid: String): Pensjontype {
        require(aktoerid.isNotBlank()) { "AktoerId kan ikke være blank/tom"}
        val sak = finnSak(sakId, hentAltPaaAktoerId(aktoerid)) ?: return Pensjontype(sakId, "")
            return Pensjontype(sakId, sak.sakType)
    }

    @Deprecated("Replace with hentAltPaaFNR")
    fun hentAltPaaAktoerId(aktoerId: String): Pensjonsinformasjon {
        require(aktoerId.isNotBlank()) { "AktoerId kan ikke være blank/tom"}

        return pensjoninformasjonHentAltPaaIdent.measure {

            val requestBody = pensjonRequestBuilder.requestBodyForSakslisteFromAString()

            logger.debug("Requestbody:\n$requestBody")
            logger.info("Henter pensjonsinformasjon for aktor: $aktoerId")

            val xmlResponse = doRequest(REQUESTPATH.AKTOR, aktoerId, requestBody, pensjoninformasjonHentAltPaaIdentRequester)
            transform(xmlResponse)
        }
    }

    fun hentAltPaaVedtak(vedtaksId: String): Pensjonsinformasjon {

        return pensjoninformasjonAltPaaVedtak.measure {

            val requestBody = pensjonRequestBuilder.requestBodyForVedtakFromAString()
            logger.debug("Requestbody:\n$requestBody")
            logger.info("Henter pensjonsinformasjon for vedtaksid: $vedtaksId")

            val xmlResponse = doRequest(REQUESTPATH.VEDTAK, vedtaksId, requestBody, pensjoninformasjonAltPaaVedtakRequester)
            transform(xmlResponse)
        }
    }

    fun hentKravDatoFraAktor(aktorId: String, saksId: String, kravId: String) : String? {
        val pensjonSak = hentAltPaaAktoerId(aktorId)
        return hentKravFraKravHistorikk(saksId, pensjonSak, kravId)
    }

    private fun hentKravFraKravHistorikk(saksId: String, pensjonSak: Pensjonsinformasjon, kravId: String ): String? {
        val sak = finnSak(saksId, pensjonSak) ?: return null

        val kravHistorikk = sak.kravHistorikkListe?.kravHistorikkListe?.filter { krav -> krav.kravId == kravId }

        if (kravHistorikk == null) {
            logger.warn("Kravhistorikk har ingen krav")
            throw PensjoninformasjonException("Mangler kravistorikk")
        } else if (kravHistorikk.size > 1) {
            logger.warn("Det forventes kun et krav for kravId: $kravId, men Kravhistorikk har ${kravHistorikk.size}  krav")
            throw PensjoninformasjonException("KravHistorikkListe med har for mange krav")
        }

        return kravHistorikk[0]?.mottattDato!!.simpleFormat()
    }

    fun hentAltPaaFNR(fnr: String): Pensjonsinformasjon {
        require(fnr.isNotBlank()) { "Fnr kan ikke være blank/tom"}

        return pensjoninformasjonHentAltPaaIdent.measure {

            val requestBody = pensjonRequestBuilder.requestBodyForSakslisteFromAString()
            logger.debug("Requestbody:\n$requestBody")

            if (fnr.length > 7) {
                val obfuscateFnr = fnr.replaceRange(7, fnr.length, "★".repeat(fnr.length-7))
                logger.info("Henter pensjonsinformasjon for fnr: $obfuscateFnr")
            } else logger.info("Henter pensjonsinformasjon for fnr")

            val xmlResponse = doRequest(REQUESTPATH.FNR, fnr,  requestBody, pensjoninformasjonHentAltPaaIdentRequester)
            transform(xmlResponse)
        }
    }

    fun transform(xmlString: String) : Pensjonsinformasjon {
        return try {

            val context = JAXBContext.newInstance(Pensjonsinformasjon::class.java)
            val unmarshaller = context.createUnmarshaller()

            logger.debug("Pensjonsinformasjon xml: $xmlString")
            val res = unmarshaller.unmarshal(StreamSource(StringReader(xmlString)), Pensjonsinformasjon::class.java)

            res.value as Pensjonsinformasjon

        } catch (ex: Exception) {
            logger.error("Feiler med xml transformering til Pensjoninformasjon")
            throw PensjoninformasjonProcessingException("Feiler med xml transformering til Pensjoninformasjon: ${ex.message}")
        }
    }

    @Throws(PensjoninformasjonException::class, HttpServerErrorException::class, HttpClientErrorException::class)
    private fun doRequest(path: REQUESTPATH, id: String, requestBody: String, metric: MetricsHelper.Metric): String {

        val headers = HttpHeaders()
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
        if (path == REQUESTPATH.FNR) {
            headers.add("fnr", id)
        }
        val requestEntity = HttpEntity(requestBody, headers)

        val uriBuilder = if (path == REQUESTPATH.FNR) {
            UriComponentsBuilder.fromPath(path.path)
        } else {
            UriComponentsBuilder.fromPath(path.path).pathSegment(id)
        }

        logger.info("Pensjoninformasjon Uri: ${uriBuilder.toUriString()}")

        return metric.measure {
            return@measure try {
                val responseEntity = pensjoninformasjonRestTemplate.exchange(
                    uriBuilder.toUriString(),
                    HttpMethod.POST,
                    requestEntity,
                    String::class.java)

                responseEntity.body!!

            } catch (hsee: HttpServerErrorException) {
                val errorBody = hsee.responseBodyAsString
                logger.error("PensjoninformasjonService feiler med HttpServerError body: $errorBody", hsee)
                throw PensjoninformasjonException("PensjoninformasjonService feiler med innhenting av pensjoninformasjon fra PESYS, prøv igjen om litt")
            } catch (hcee: HttpClientErrorException) {
                val errorBody = hcee.responseBodyAsString
                logger.error("PensjoninformasjonService feiler med HttpClientError body: $errorBody", hcee)
                throw PensjoninformasjonException("PensjoninformasjonService feiler med innhenting av pensjoninformasjon fra PESYS, prøv igjen om litt")
            } catch (ex: Exception) {
                logger.error("PensjoninformasjonService feiler med kontakt til PESYS pensjoninformajson, ${ex.message}", ex)
                throw PensjoninformasjonException("PensjoninformasjonService feiler med ukjent feil mot PESYS. melding: ${ex.message}")
            }
        }
    }
}

class PensjoninformasjonException(message: String) : ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message)
class PensjoninformasjonProcessingException(message: String) : ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message)
