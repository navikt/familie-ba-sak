package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.utfall

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.EvalueringÅrsak
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.Filtreringsregel

enum class FiltreringsregelIkkeOppfylt(val beskrivelse: String, private val filtreringsregel: Filtreringsregel) :
    EvalueringÅrsak {
    MOR_HAR_UGYLDIG_FNR("Mor har ugyldig fødselsnummer", Filtreringsregel.MOR_GYLDIG_FNR),
    BARN_HAR_UGYLDIG_FNR("Barn har ugyldig fødselsnummer", Filtreringsregel.BARN_GYLDIG_FNR),
    MOR_ER_UNDER_18_ÅR("Mor er under 18 år.", Filtreringsregel.MOR_ER_OVER_18_ÅR),
    MOR_ER_UNDER_VERGEMÅL("Mor er under vergemål.", Filtreringsregel.MOR_HAR_IKKE_VERGE),
    MOR_MOTTAR_LØPENDE_UTVIDET("Mor mottar utvidet barnetrygd.", Filtreringsregel.MOR_MOTTAR_IKKE_LØPENDE_UTVIDET),
    MOR_LEVER_IKKE("Det er registrert dødsdato på mor.", Filtreringsregel.MOR_LEVER),
    BARNET_LEVER_IKKE("Det er registrert dødsdato på barnet.", Filtreringsregel.BARN_LEVER),
    MINDRE_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL(
        "Det har gått mindre enn fem måneder siden forrige barn ble født.",
        Filtreringsregel.MER_ENN_5_MND_SIDEN_FORRIGE_BARN,
    ),
    FAGSAK_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT(
        "Fagsaken ble migrert fra infotrygd etter barn ble født.",
        Filtreringsregel.FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT,
    ),
    LØPER_ALLEREDE_FOR_ANNEN_FORELDER(
        "Annen mottaker har barnetrygd for barnet",
        Filtreringsregel.LØPER_IKKE_BARNETRYGD_FOR_BARNET,
    ),
    MOR_HAR_LØPENDE_EØS_BARNETRYGD(
        "Mor har EØS-barnetrygd",
        Filtreringsregel.MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD,
    ),
    MOR_OPPFYLLER_VILKÅR_FOR_UTVIDET_BARNETRYGD_VED_FØDSELSDATO(
        "Mor oppfyller vilkår for utvidet barnetrygd",
        Filtreringsregel.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO,
    ),
    MOR_HAR_OPPHØRT_BARNETRYGD(
        "Mor har vedtak om opphørt barnetrygd.",
        Filtreringsregel.MOR_HAR_IKKE_OPPHØRT_BARNETRYGD,
    ),
    ;

    override fun hentBeskrivelse(): String {
        return beskrivelse
    }

    override fun hentMetrikkBeskrivelse(): String {
        return beskrivelse
    }

    override fun hentIdentifikator(): String {
        return filtreringsregel.name
    }
}
