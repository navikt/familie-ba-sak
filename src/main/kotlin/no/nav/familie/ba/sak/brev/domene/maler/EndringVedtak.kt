package no.nav.familie.ba.sak.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class EndringVedtak(
        override val type: BrevType = BrevType.ENDRING_VEDTAK,
        override val data: EndringVedtakData
) : Vedtaksbrev {

    constructor(
            enhet: String,
            saksbehandler: String,
            beslutter: String,
            etterbetalingsbeløp: String?,
            hjemlter: String,
            søkerNavn: String,
            søkerFødselsnummer: String,
            perioder: Perioder,
            feilutbetaling: Boolean,
            klage: Boolean
    ) :
            this(data = EndringVedtakData(
                    delmalData = EndringVedtakData.Delmaler(
                            signaturVedtak = SignaturVedtatk(
                                    enhet = enhet,
                                    saksbehandler = saksbehandler,
                                    beslutter = beslutter),
                            etterbetaling = if (!etterbetalingsbeløp.isNullOrBlank()) {
                                Etterbetaling(etterbetalingsbeløp = etterbetalingsbeløp)
                            } else {
                                null
                            },
                            hjemmeltekst = Hjemmeltekst(
                                    hjemler = hjemlter),
                            klage = klage,
                            feilutbetaling = feilutbetaling),
                    flettefelter = EndringVedtakData.Flettefelter(
                            navn = søkerNavn,
                            fodselsnummer = søkerFødselsnummer),
                    perioder = perioder)
            )
}

data class EndringVedtakData(
        override val delmalData: Delmaler,
        override val flettefelter: Flettefelter,
        override val perioder: Perioder
) : VedtaksbrevData {

    data class Flettefelter(
            val navn: Flettefelt,
            val fodselsnummer: Flettefelt,
            val dato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
    ) {

        constructor(navn: String,
                    fodselsnummer: String) : this(navn = flettefelt(navn),
                                                  fodselsnummer = flettefelt(fodselsnummer))
    }

    data class Delmaler(
            val signaturVedtak: SignaturVedtatk,
            val etterbetaling: Etterbetaling?,
            val feilutbetaling: Boolean,
            val hjemmeltekst: Hjemmeltekst,
            val klage: Boolean,
    )
}