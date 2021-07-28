package no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.utfall

import no.nav.familie.ba.sak.kjerne.automatiskvurdering.filtreringsregler.FiltreringsreglerResultat
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.EvalueringÅrsak

enum class FiltreringsregelIkkeOppfyltNy(val beskrivelse: String, private val filtreringsregel: FiltreringsreglerResultat) :
        EvalueringÅrsak {

    MOR_HAR_UGYLDIG_FNR("Mor har ugyldig fødselsnummer", FiltreringsreglerResultat.MOR_GYLDIG_FNR),
    BARN_HAR_UGYLDIG_FNR("Barn har ugyldig fødselsnummer", FiltreringsreglerResultat.BARN_GYLDIG_FNR),
    MOR_ER_UNDER_18_ÅR("Mor er under 18 år.", FiltreringsreglerResultat.MOR_ER_OVER_18_ÅR),
    MOR_ER_UMYNDIG("Mor er umyndig.", FiltreringsreglerResultat.MOR_HAR_IKKE_VERGE),
    MOR_LEVER_IKKE("Det er registrert dødsdato på mor.", FiltreringsreglerResultat.MOR_LEVER),
    BARNET_LEVER_IKKE("Det er registrert dødsdato på barnet.", FiltreringsreglerResultat.BARN_LEVER),
    MINDRE_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL("Det har gått mindre enn fem måneder siden forrige barn ble født.",
                                               FiltreringsreglerResultat.MER_ENN_5_MND_SIDEN_FORRIGE_BARN),
    SAKEN_MEDFØRER_ETTERBETALING("Saken medfører etterbetaling.",
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