package no.nav.eessi.pensjon.pensjonsinformasjon

import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sak.V1Sak
import org.slf4j.LoggerFactory

object FinnSak {
        private val logger = LoggerFactory.getLogger(FinnSak::class.java)

        fun finnSak(sakId: String, pendata: Pensjonsinformasjon): V1Sak? {
            logger.info("Søker brukersSakerListe etter sakId: $sakId")
            val v1saklist = pendata.brukersSakerListe.brukersSakerListe

            return v1saklist.firstOrNull { sak -> "${sak.sakId}" == sakId  }
        }

}