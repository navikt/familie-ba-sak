package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.utfall

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.EvalueringÅrsak
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.Filtreringsregel

enum class FiltreringsregelOppfylt(
    val beskrivelse: String,
    private val identifikator: Filtreringsregel.Identifikator,
) : EvalueringÅrsak {
    MOR_HAR_GYLDIG_FNR(
        "Mor har gyldig fødselsnummer",
        Filtreringsregel.Identifikator.MOR_GYLDIG_FNR
    ),
    BARN_HAR_GYLDIG_FNR(
        "Barn har gyldig fødselsnummer",
        Filtreringsregel.Identifikator.BARN_GYLDIG_FNR
    ),
    MOR_ER_OVER_18_ÅR(
        "Mor er over 18 år.",
        Filtreringsregel.Identifikator.MOR_ER_OVER_18_ÅR
    ),
    MOR_ER_MYNDIG(
        "Mor er myndig.",
        Filtreringsregel.Identifikator.MOR_HAR_IKKE_VERGE
    ),
    MOR_MOTTAR_IKKE_LØPENDE_UTVIDET(
        "Mor mottar ikke utvidet barnetrygd.",
        Filtreringsregel.Identifikator.MOR_MOTTAR_IKKE_LØPENDE_UTVIDET,
    ),
    MOR_LEVER(
        "Det er ikke registrert dødsdato på mor.",
        Filtreringsregel.Identifikator.MOR_LEVER
    ),
    BARNET_LEVER(
        "Det er ikke registrert dødsdato på barnet.",
        Filtreringsregel.Identifikator.BARN_LEVER
    ),
    MER_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL(
        "Det har gått mer enn fem måneder siden forrige barn ble født.",
        Filtreringsregel.Identifikator.MER_ENN_5_MND_SIDEN_FORRIGE_BARN,
    ),
    FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT(
        "Fagsaken har ikke blitt migrert fra infotrygd etter barn ble født.",
        Filtreringsregel.Identifikator.FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT,
    ),
    LØPER_IKKE_BARNETRYGD_FOR_BARNET(
        "Det løper ikke barnetrygd for barnet på annen forelder",
        Filtreringsregel.Identifikator.LØPER_IKKE_BARNETRYGD_FOR_BARNET,
    ),
    MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD(
        "Mor har ikke løpende EØS-barnetrygd",
        Filtreringsregel.Identifikator.MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD,
    ),
    MOR_OPPFYLLER_IKKE_VILKÅR_FOR_UTVIDET_BARNETRYGD_VED_FØDSELSDATO(
        "Mor oppfyller ikke vilkår for utvidet barnetrygd",
        Filtreringsregel.Identifikator.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO,
    ),
    MOR_HAR_IKKE_OPPHØRT_BARNETRYGD(
        "Mor har ikke opphørt barnetrygd",
        Filtreringsregel.Identifikator.MOR_HAR_IKKE_OPPHØRT_BARNETRYGD,
    ),
    ;

    override fun hentBeskrivelse(): String = beskrivelse

    override fun hentMetrikkBeskrivelse(): String = beskrivelse

    override fun hentIdentifikator(): String = identifikator.name

    override fun hentNavn(): String = name
}
