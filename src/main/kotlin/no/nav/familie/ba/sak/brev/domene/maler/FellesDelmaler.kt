package no.nav.familie.ba.sak.brev.domene.maler

import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext

data class SignaturDelmal(
        val enhet: Flettefelt,
        val saksbehandler: Flettefelt = flettefelt(SikkerhetContext.hentSaksbehandlerNavn()),
) {

    constructor(enhet: String) : this(flettefelt(enhet))
}

data class SignaturVedtatk(
        val enhet: Flettefelt,
        val saksbehandler: Flettefelt,
        val beslutter: Flettefelt,
) {

    constructor(enhet: String, saksbehandler: String, beslutter: String) : this(
            flettefelt(enhet),
            flettefelt(saksbehandler),
            flettefelt(beslutter)
    )
}

data class Etterbetaling(
        val etterbetalingsbelop: Flettefelt,
) {

    constructor(etterbetalingsbeløp: String) : this(
            flettefelt(etterbetalingsbeløp),
    )
}