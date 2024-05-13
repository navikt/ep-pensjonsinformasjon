package no.nav.eessi.pensjon.pensjonsinformasjon.models

enum class PenKravtype {
    REVURD,        //Revurdering
    F_BH_MED_UTL,  //Førstegangsbehandling Norge/utland
    F_BH_BO_UTL,   //Førstegangsbehandling bosatt utland
    F_BH_KUN_UTL,  //Førstegangsbehandling kun utland
}

enum class Kravstatus {
    TIL_BEHANDLING,
    AVSL
}

//https://confluence.adeo.no/pages/viewpage.action?pageId=338181301
enum class KravArsak {
    GJNL_SKAL_VURD,
    TILST_DOD,
    NY_SOKNAD,
    `Ingen status`
    //GJENLEVENDERETT
    //GJENLEVENDETILLEGG
}

enum class EPSaktype {
    ALDER,
    UFOREP,
    BARNEP,
    GJENLEV,
    OMSORG,
    GENRL
    ;
}

enum class Sakstatus(val value: String) {
    INNV("INNV"),
    AVSL("AVSL"),
    INGEN_STATUS("Ingen status"),
    UKJENT("Ukjent"),
    TIL_BEHANDLING("TIL_BEHANDLING");

    companion object {
        fun byValue(value: String): Sakstatus {
            return values().first { sakstatus -> sakstatus.value == value }
        }
    }
}