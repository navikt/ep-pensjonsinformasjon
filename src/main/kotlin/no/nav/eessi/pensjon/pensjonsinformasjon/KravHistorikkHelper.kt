package no.nav.eessi.pensjon.pensjonsinformasjon

import no.nav.eessi.pensjon.pensjonsinformasjon.models.*
import no.nav.eessi.pensjon.pensjonsinformasjon.models.PenKravtype.*
import no.nav.pensjon.v1.kravhistorikk.V1KravHistorikk
import no.nav.pensjon.v1.kravhistorikkliste.V1KravHistorikkListe
import no.nav.pensjon.v1.sak.V1Sak
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object KravHistorikkHelper {
    private val logger: Logger by lazy { LoggerFactory.getLogger(KravHistorikkHelper::class.java) }

    private fun sortertKravHistorikk(kravHistorikkListe: V1KravHistorikkListe?): List<V1KravHistorikk>? {
        return kravHistorikkListe?.kravHistorikkListe?.sortedBy { it.mottattDato.toGregorianCalendar() }
    }

    fun hentKravHistorikkForsteGangsBehandlingUtlandEllerForsteGang(kravHistorikkListe: V1KravHistorikkListe?): V1KravHistorikk =
        hentKravHistorikkMedKravType(listOf(F_BH_MED_UTL, F_BH_KUN_UTL, REVURD, F_BH_BO_UTL, SLUTT_BH_UTL), kravHistorikkListe)

    fun finnKravHistorikk(kravType: PenKravtype, kravHistorikkListe: V1KravHistorikkListe?): List<V1KravHistorikk>? {
        return sortertKravHistorikk(kravHistorikkListe)?.filter { it.kravType == kravType.name }
    }

    private fun hentKravHistorikkMedKravType(kravType: List<PenKravtype>, kravHistorikkListe: V1KravHistorikkListe?): V1KravHistorikk {
        val sortList = sortertKravHistorikk(kravHistorikkListe)
        val sortListFraKravIndex = sortList?.sortedBy { kravType.indexOfFirst { type -> type.name == it.kravType } }

        if (sortListFraKravIndex != null && sortListFraKravIndex.size > 1) {
            logger.warn("Listen med krav er større enn én. Krav: {${sortList.size}")
        }

        sortListFraKravIndex?.forEach { kravHistorikk ->
            if (kravHistorikk.kravType in kravType.map { it.name } ) {
                logger.info("Fant ${kravHistorikk.kravType} med virkningstidspunkt: ${kravHistorikk.virkningstidspunkt}")
                return kravHistorikk
            }
        }
        logger.warn("Fant ikke noe Kravhistorikk. med $kravType. Grunnet utsending kun utland mangler vilkårprøving/vedtak. følger ikke normal behandling")
        return V1KravHistorikk()
    }

    fun hentKravhistorikkForGjenlevende(kravHistorikkListe: V1KravHistorikkListe?): V1KravHistorikk? {
            val kravHistorikk = kravHistorikkListe?.kravHistorikkListe?.filter { krav -> krav.kravArsak == KravArsak.GJNL_SKAL_VURD.name || krav.kravArsak == KravArsak.TILST_DOD.name }
            if (kravHistorikk?.isNotEmpty() == true) {
                return kravHistorikk.first()
            }
            logger.warn("Fant ikke Kravhistorikk med bruk av kravårsak: ${KravArsak.GJNL_SKAL_VURD.name} eller ${KravArsak.TILST_DOD.name} ")
            return null
    }

    fun hentKravhistorikkForGjenlevendeOgNySoknad(kravHistorikkListe: V1KravHistorikkListe?): V1KravHistorikk? {
        val kravHistorikk = kravHistorikkListe?.kravHistorikkListe?.filter { krav -> krav.kravArsak == KravArsak.GJNL_SKAL_VURD.name || krav.kravArsak == KravArsak.TILST_DOD.name || krav.kravArsak == KravArsak.NY_SOKNAD.name }
        if (kravHistorikk?.isNotEmpty() == true) {
            return kravHistorikk.first()
        }
        logger.warn("Fant ikke Kravhistorikk med bruk av kravårsak: ${KravArsak.GJNL_SKAL_VURD.name} , ${KravArsak.TILST_DOD.name} eller ${KravArsak.NY_SOKNAD.name} fra kravliste: \n${kravHistorikkListe.toString()}")
        return null
    }

    fun hentKravHistorikkMedKravStatusTilBehandling(kravHistorikkListe: V1KravHistorikkListe?): V1KravHistorikk {
        val sortList = sortertKravHistorikk(kravHistorikkListe)
        sortList?.forEach {
            logger.debug("leter etter Krav status med ${Kravstatus.TIL_BEHANDLING}, fant ${it.kravType} med virkningstidspunkt dato : ${it.virkningstidspunkt}")
            if (Kravstatus.TIL_BEHANDLING.name == it.status) {
                logger.debug("Fant Kravhistorikk med ${it.status}")
                return it
            }
        }
        logger.error("Fant ikke noe Kravhistorikk..${Kravstatus.TIL_BEHANDLING}. Mangler vilkårsprlving/vedtak. følger ikke normal behandling")
        return V1KravHistorikk()
    }

    fun hentKravHistorikkMedKravStatusAvslag(kravHistorikkListe: V1KravHistorikkListe?): V1KravHistorikk {
        val sortList = sortertKravHistorikk(kravHistorikkListe)
        sortList?.forEach {
            logger.debug("leter etter Krav status med ${Kravstatus.AVSL}, fant ${it.kravType} med virkningstidspunkt dato : ${it.virkningstidspunkt}")
            if (Kravstatus.AVSL.name == it.status) {
                logger.debug("Fant Kravhistorikk med ${it.status}")
                return it
            }
        }
        logger.error("Fant ikke noe Kravhistorikk..${Kravstatus.AVSL}. Mangler vilkårsprøving. følger ikke normal behandling")
        return V1KravHistorikk()
    }

    fun hentKravHistorikkMedValgtKravType(kravHistorikkListe: V1KravHistorikkListe?, penKravtype: PenKravtype): V1KravHistorikk? {
        val sortList = sortertKravHistorikk(kravHistorikkListe)
        if (sortList == null || sortList.size > 1) return null
        logger.debug("leter etter kravtype: $penKravtype")
        return sortList.firstOrNull { kravhist -> kravhist.kravType == penKravtype.name}
            .also { logger.debug("fant ${it?.kravType} med kravÅrsak: ${it?.kravArsak} med virkningstidspunkt dato : ${it?.virkningstidspunkt}") }

    }

    fun finnKravHistorikkForDato(pensak: V1Sak?): V1KravHistorikk {
        try {
            val gjenLevKravarsak = hentKravhistorikkForGjenlevende(pensak?.kravHistorikkListe)
            if (gjenLevKravarsak != null) return gjenLevKravarsak

            val kravKunUtland = hentKravHistorikkMedValgtKravType(pensak?.kravHistorikkListe, F_BH_KUN_UTL)
            if (kravKunUtland != null) return  kravKunUtland

            logger.info("Sakstatus: ${pensak?.status},sakstype: ${pensak?.sakType}")
            return when (Sakstatus.byValue(pensak?.status!!)) {
                Sakstatus.TIL_BEHANDLING -> hentKravHistorikkMedKravStatusTilBehandling(pensak.kravHistorikkListe)
                Sakstatus.AVSL -> hentKravHistorikkMedKravStatusAvslag(pensak.kravHistorikkListe)
                else -> hentKravHistorikkForsteGangsBehandlingUtlandEllerForsteGang(pensak.kravHistorikkListe)
            }

        } catch (ex: Exception) {
            logger.warn("Fant ingen gyldig kravdato: $ex")
            return V1KravHistorikk()
        }
    }

}
