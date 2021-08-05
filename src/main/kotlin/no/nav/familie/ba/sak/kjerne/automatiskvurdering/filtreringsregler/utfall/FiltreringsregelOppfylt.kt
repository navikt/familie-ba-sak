package no.nav.familie.ba.sak.kjerne.automatiskvurdering.filtreringsregler.utfall

import no.nav.familie.ba.sak.kjerne.automatiskvurdering.filtreringsregler.Filtreringsregel
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.EvalueringÅrsak

enum class FiltreringsregelOppfylt(val beskrivelse: String, private val filtreringsregel: Filtreringsregel) :
        EvalueringÅrsak {

    MOR_HAR_GYLDIG_FNR("Mor har gyldig fødselsnummer", Filtreringsregel.MOR_GYLDIG_FNR),
    BARN_HAR_GYLDIG_FNR("Barn har gyldig fødselsnummer", Filtreringsregel.BARN_GYLDIG_FNR),
    MOR_ER_OVER_18_ÅR("Mor er over 18 år.", Filtreringsregel.MOR_ER_OVER_18_ÅR),
    MOR_ER_MYNDIG("Mor er myndig.", Filtreringsregel.MOR_HAR_IKKE_VERGE),
    MOR_LEVER("Det er ikke registrert dødsdato på mor.", Filtreringsregel.MOR_LEVER),
    BARNET_LEVER("Det er ikke registrert dødsdato på barnet.", Filtreringsregel.BARN_LEVER),
    MER_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL("Det har gått mer enn fem måneder siden forrige barn ble født.",
                                            Filtreringsregel.MER_ENN_5_MND_SIDEN_FORRIGE_BARN),
    SAKEN_MEDFØRER_IKKE_ETTERBETALING("Saken medfører ikke etterbetaling.",
                                      Filtreringsregel.BARNETS_FØDSELSDATO_TRIGGER_IKKE_ETTERBETALING);

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