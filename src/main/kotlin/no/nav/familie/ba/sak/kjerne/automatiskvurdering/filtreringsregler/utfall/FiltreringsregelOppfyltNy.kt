package no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.utfall

import no.nav.familie.ba.sak.kjerne.automatiskvurdering.filtreringsregler.FiltreringsreglerResultat
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.EvalueringÅrsak

enum class FiltreringsregelOppfyltNy(val beskrivelse: String, private val filtreringsregel: FiltreringsreglerResultat) :
        EvalueringÅrsak {

    MOR_HAR_GYLDIG_FNR("Mor har gyldig fødselsnummer", FiltreringsreglerResultat.MOR_GYLDIG_FNR),
    BARN_HAR_GYLDIG_FNR("Barn har gyldig fødselsnummer", FiltreringsreglerResultat.BARN_GYLDIG_FNR),
    MOR_ER_OVER_18_ÅR("Mor er over 18 år.", FiltreringsreglerResultat.MOR_ER_OVER_18_ÅR),
    MOR_ER_MYNDIG("Mor er myndig.", FiltreringsreglerResultat.MOR_HAR_IKKE_VERGE),
    MOR_LEVER("Det er ikke registrert dødsdato på mor.", FiltreringsreglerResultat.MOR_LEVER),
    BARNET_LEVER("Det er ikke registrert dødsdato på barnet.", FiltreringsreglerResultat.BARN_LEVER),
    MER_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL("Det har gått mer enn fem måneder siden forrige barn ble født.",
                                            FiltreringsreglerResultat.MER_ENN_5_MND_SIDEN_FORRIGE_BARN),
    SAKEN_MEDFØRER_IKKE_ETTERBETALING("Saken medfører ikke etterbetaling.",
                                      FiltreringsreglerResultat.BARNETS_FØDSELSDATO_TRIGGER_IKKE_ETTERBETALING);

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