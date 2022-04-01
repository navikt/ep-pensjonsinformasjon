package no.nav.eessi.pensjon.pensjonsinformasjon.clients

import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.io.StringReader
import javax.xml.bind.JAXBContext
import javax.xml.transform.stream.StreamSource

internal object PensjonsinformasjonTransformation {

    private val logger = LoggerFactory.getLogger(PensjonsinformasjonTransformation::class.java)

    //transform xmlString til Pensjoninformasjon object
    fun transform(xmlString: String): Pensjonsinformasjon {
        return try {

            val context = JAXBContext.newInstance(Pensjonsinformasjon::class.java)
            val unmarshaller = context.createUnmarshaller()

            logger.debug("Pensjonsinformasjon xml: $xmlString")
            val res = unmarshaller.unmarshal(StreamSource(StringReader(xmlString)), Pensjonsinformasjon::class.java)

            val pensjon = res.value as Pensjonsinformasjon

            pensjon

        } catch (ex: Exception) {
            logger.error("Feiler med xml transformering til Pensjoninformasjon", ex)
            throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Feiler med xml transformering til Pensjoninformasjon: ${ex.message}")
        }
    }

}