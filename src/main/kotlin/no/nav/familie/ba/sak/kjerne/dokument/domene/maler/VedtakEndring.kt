package no.nav.familie.ba.sak.kjerne.dokument.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class VedtakEndring(
        override val type: Vedtaksbrevtype = Vedtaksbrevtype.VEDTAK_ENDRING,
        override val data: EndringVedtakData
) : Vedtaksbrev {

    constructor(
            vedtakFellesfelter: VedtakFellesfelter,
            etterbetaling: Etterbetaling?,
            erFeilutbetalingPåBehandling: Boolean,
            erKlage: Boolean
    ) :
            this(data = EndringVedtakData(
                    delmalData = EndringVedtakData.Delmaler(
                            signaturVedtak = SignaturVedtak(
                                    enhet = vedtakFellesfelter.enhet,
                                    saksbehandler = vedtakFellesfelter.saksbehandler,
                                    beslutter = vedtakFellesfelter.beslutter),
                            etterbetaling = etterbetaling,
                            hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                            klage = erKlage,
                            feilutbetaling = erFeilutbetalingPåBehandling),
                    flettefelter = EndringVedtakData.Flettefelter(
                            navn = vedtakFellesfelter.søkerNavn,
                            fodselsnummer = vedtakFellesfelter.søkerFødselsnummer),
                    perioder = vedtakFellesfelter.perioder)
            )
}

data class EndringVedtakData(
        override val delmalData: Delmaler,
        override val flettefelter: Flettefelter,
        override val perioder: List<BrevPeriode>
) : VedtaksbrevData {

    data class Flettefelter(
            val navn: Flettefelt,
            val fodselsnummer: Flettefelt,
            val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
            // TODO: Fjern etter at brevOpprettetDato er lagt til i familie brev. dato -> brevOpprettetDato
            val dato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
    ) {

        constructor(navn: String,
                    fodselsnummer: String) : this(navn = flettefelt(navn),
                                                  fodselsnummer = flettefelt(fodselsnummer))
    }

    data class Delmaler(
            val signaturVedtak: SignaturVedtak,
            val etterbetaling: Etterbetaling?,
            val feilutbetaling: Boolean,
            val hjemmeltekst: Hjemmeltekst,
            val klage: Boolean,
    )
}