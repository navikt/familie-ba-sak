package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene

import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet

data class Arbeidsfordelingsenhet(
    val enhetId: String,
    val enhetNavn: String,
) {
    companion object {
        fun opprettFra(enhet: BarnetrygdEnhet): Arbeidsfordelingsenhet =
            Arbeidsfordelingsenhet(
                enhetId = enhet.enhetsnummer,
                enhetNavn = enhet.enhetsnavn,
            )
    }
}
