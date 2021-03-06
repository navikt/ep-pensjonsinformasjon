package no.nav.eessi.pensjon.pensjonsinformasjon

import no.nav.eessi.pensjon.pensjonsinformasjon.models.EPSaktype
import no.nav.eessi.pensjon.pensjonsinformasjon.models.KravArsak
import no.nav.eessi.pensjon.pensjonsinformasjon.models.Kravstatus
import no.nav.eessi.pensjon.pensjonsinformasjon.models.Kravtype
import no.nav.eessi.pensjon.pensjonsinformasjon.models.Sakstatus
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

    fun hentKravHistorikkForsteGangsBehandlingUtlandEllerForsteGang(kravHistorikkListe: V1KravHistorikkListe?, saktype: String?): V1KravHistorikk {
        //F_BH_MED_UTL
        if (EPSaktype.BARNEP.name == saktype) {
            return hentKravHistorikkMedKravType(listOf(Kravtype.F_BH_MED_UTL.name, Kravtype.FORSTEG_BH.name, Kravtype.F_BH_BO_UTL.name), kravHistorikkListe)
        }
        return hentKravHistorikkMedKravType(listOf(Kravtype.F_BH_MED_UTL.name, Kravtype.FORSTEG_BH.name, Kravtype.F_BH_KUN_UTL.name), kravHistorikkListe)
    }

    fun finnKravHistorikk(kravType: String, kravHistorikkListe: V1KravHistorikkListe?): List<V1KravHistorikk>? {
        return sortertKravHistorikk(kravHistorikkListe)?.filter { kravType == it.kravType }
    }

    private fun hentKravHistorikkMedKravType(kravType: List<String>, kravHistorikkListe: V1KravHistorikkListe?): V1KravHistorikk {
        val sortList = sortertKravHistorikk(kravHistorikkListe)
        sortList?.forEach { kravHistorikk ->
            logger.info("leter etter Kravtype: $kravType, fant ${kravHistorikk.kravType} med dato i ${kravHistorikk.virkningstidspunkt}")
            if (kravType.contains(kravHistorikk.kravType)) {
                logger.info("Fant Kravhistorikk med $kravType")
                return kravHistorikk
            }
        }
        logger.warn("Fant ikke noe Kravhistorikk. med $kravType. Grunnet utsending kun utland mangler vilk??rpr??ving/vedtak. f??lger ikke normal behandling")
        return V1KravHistorikk()
    }

    fun hentKravhistorikkForGjenlevende(kravHistorikkListe: V1KravHistorikkListe?): V1KravHistorikk? {
            val kravHistorikk = kravHistorikkListe?.kravHistorikkListe?.filter { krav -> krav.kravArsak == KravArsak.GJNL_SKAL_VURD.name || krav.kravArsak == KravArsak.TILST_DOD.name }
            if (kravHistorikk?.isNotEmpty() == true) {
                return kravHistorikk.first()
            }
            logger.warn("Fant ikke Kravhistorikk med bruk av krav??rsak: ${KravArsak.GJNL_SKAL_VURD.name} eller ${KravArsak.TILST_DOD.name} ")
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
        logger.error("Fant ikke noe Kravhistorikk..${Kravstatus.TIL_BEHANDLING}. Mangler vilk??rsprlving/vedtak. f??lger ikke normal behandling")
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
        logger.error("Fant ikke noe Kravhistorikk..${Kravstatus.AVSL}. Mangler vilk??rspr??ving. f??lger ikke normal behandling")
        return V1KravHistorikk()
    }

    fun hentKravHistorikkMedValgtKravType(kravHistorikkListe: V1KravHistorikkListe?, kravtype: Kravtype): V1KravHistorikk? {
        val sortList = sortertKravHistorikk(kravHistorikkListe)
        if (sortList == null || sortList.size > 1) return null
        logger.debug("leter etter kravtype: $kravtype")
        return sortList.firstOrNull { kravhist -> kravhist.kravType == kravtype.name}
            .also { logger.debug("fant ${it?.kravType} med krav??rsak: ${it?.kravArsak} med virkningstidspunkt dato : ${it?.virkningstidspunkt}") }

    }

    fun finnKravHistorikkForDato(pensak: V1Sak?): V1KravHistorikk {
        try {
            val gjenLevKravarsak = hentKravhistorikkForGjenlevende(pensak?.kravHistorikkListe)
            if (gjenLevKravarsak != null) return gjenLevKravarsak

            val kravKunUtland = hentKravHistorikkMedValgtKravType(pensak?.kravHistorikkListe, Kravtype.F_BH_KUN_UTL)
            if (kravKunUtland != null) return  kravKunUtland

            logger.info("Sakstatus: ${pensak?.status},sakstype: ${pensak?.sakType}")
            return when (Sakstatus.byValue(pensak?.status!!)) {
                Sakstatus.TIL_BEHANDLING -> hentKravHistorikkMedKravStatusTilBehandling(pensak.kravHistorikkListe)
                Sakstatus.AVSL -> hentKravHistorikkMedKravStatusAvslag(pensak.kravHistorikkListe)
                else -> hentKravHistorikkForsteGangsBehandlingUtlandEllerForsteGang(pensak.kravHistorikkListe, pensak.sakType)
            }

        } catch (ex: Exception) {
            logger.warn("Fant ingen gyldig kravdato: $ex")
            return V1KravHistorikk()
        }
    }

}
