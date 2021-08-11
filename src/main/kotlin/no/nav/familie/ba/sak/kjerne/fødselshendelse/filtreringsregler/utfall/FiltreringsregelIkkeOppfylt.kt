package no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.utfall

import no.nav.familie.ba.sak.kjerne.fødselshendelse.EvalueringÅrsak
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.Filtreringsregel

enum class FiltreringsregelIkkeOppfylt(val beskrivelse: String, private val filtreringsregel: Filtreringsregel) :
        EvalueringÅrsak {

    MOR_HAR_UGYLDIG_FNR("Mor har ugyldig fødselsnummer", Filtreringsregel.MOR_GYLDIG_FNR),
    BARN_HAR_UGYLDIG_FNR("Barn har ugyldig fødselsnummer", Filtreringsregel.BARN_GYLDIG_FNR),
    MOR_ER_UNDER_18_ÅR("Mor er under 18 år.", Filtreringsregel.MOR_ER_OVER_18_ÅR),
    MOR_ER_UMYNDIG("Mor er umyndig.", Filtreringsregel.MOR_HAR_IKKE_VERGE),
    MOR_LEVER_IKKE("Det er registrert dødsdato på mor.", Filtreringsregel.MOR_LEVER),
    BARNET_LEVER_IKKE("Det er registrert dødsdato på barnet.", Filtreringsregel.BARN_LEVER),
    MINDRE_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL("Det har gått mindre enn fem måneder siden forrige barn ble født.",
                                               Filtreringsregel.MER_ENN_5_MND_SIDEN_FORRIGE_BARN);

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