package no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler.utfall

import no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler.Filtreringsregler
import no.nav.familie.ba.sak.nare.EvalueringÅrsak

enum class FiltreringsregelIkkeOppfylt(val beskrivelse: String, val filtreringsregel: Filtreringsregler) : EvalueringÅrsak {
    MOR_ER_UNDER_18_ÅR("Mor er under 18 år.", Filtreringsregler.MOR_ER_OVER_18_AAR),
    MOR_ER_UMYNDIG("Mor er umyndig.", Filtreringsregler.MOR_HAR_IKKE_VERGE),
    MOR_LEVER_IKKE("Det er registrert dødsdato på mor.", Filtreringsregler.MOR_LEVER),
    BARNET_LEVER_IKKE("Det er registrert dødsdato på barnet.", Filtreringsregler.BARNET_LEVER),
    MINDRE_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL("Det har gått mindre enn fem måneder siden forrige barn ble født.",
                                               Filtreringsregler.MER_ENN_5_MND_SIDEN_FORRIGE_BARN),
    SAKEN_MEDFØRER_ETTERBETALING("Saken medfører etterbetaling.",
                                 Filtreringsregler.BARNETS_FØDSELSDATO_TRIGGER_IKKE_ETTERBETALING);

    override fun hentBeskrivelse(): String {
        return beskrivelse
    }

    override fun hentMetrikkBeskrivelse(): String {
        return beskrivelse
    }

    override fun hentIdentifikator(): String {
        return filtreringsregel.spesifikasjon.identifikator
    }
}