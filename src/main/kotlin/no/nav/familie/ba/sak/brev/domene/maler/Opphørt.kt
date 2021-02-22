package no.nav.familie.ba.sak.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class Opphørt(
        override val type: Vedtaksbrevtype = Vedtaksbrevtype.OPPHØRT,
        override val data: OpphørtData
) : Vedtaksbrev {

    constructor(
            vedtakFellesfelter: VedtakFellesfelter,

            erFeilutbetalingPåBehandling: Boolean,
    ) :
            this(data = OpphørtData(
                    delmalData = OpphørtData.Delmaler(
                            signaturVedtak = SignaturVedtatk(
                                    enhet = vedtakFellesfelter.enhet,
                                    saksbehandler = vedtakFellesfelter.saksbehandler,
                                    beslutter = vedtakFellesfelter.beslutter),
                            hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                            feilutbetaling = erFeilutbetalingPåBehandling),
                    flettefelter = OpphørtData.Flettefelter(
                            navn = vedtakFellesfelter.søkerNavn,
                            fodselsnummer = vedtakFellesfelter.søkerFødselsnummer),
                    perioder = vedtakFellesfelter.perioder)
            )
}

data class OpphørtData(
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
            val feilutbetaling: Boolean,
            val hjemmeltekst: Hjemmeltekst,
    )
}