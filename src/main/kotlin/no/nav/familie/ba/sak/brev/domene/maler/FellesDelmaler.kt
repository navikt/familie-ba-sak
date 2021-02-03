package no.nav.familie.ba.sak.brev.domene.maler

import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext

data class SignaturDelmal(
        val enhet: Flettefelt,
        val saksbehandler: Flettefelt = flettefelt(SikkerhetContext.hentSaksbehandlerNavn()),
) {
    constructor(enhet: String): this(flettefelt(enhet))
}