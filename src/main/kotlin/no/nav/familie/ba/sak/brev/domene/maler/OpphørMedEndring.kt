package no.nav.familie.ba.sak.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class OpphørMedEndring(
        override val type: Vedtaksbrevtype = Vedtaksbrevtype.OPPHØRT_ENDRING,
        override val data: OpphørMedEndringData
) : Vedtaksbrev {

    constructor(
            vedtakFellesfelter: VedtakFellesfelter,

            etterbetaling: Etterbetaling?,
            erFeilutbetalingPåBehandling: Boolean,
    ) :
            this(data = OpphørMedEndringData(
                    delmalData = OpphørMedEndringData.Delmaler(
                            signaturVedtak = SignaturVedtak(
                                    enhet = vedtakFellesfelter.enhet,
                                    saksbehandler = vedtakFellesfelter.saksbehandler,
                                    beslutter = vedtakFellesfelter.beslutter),
                            hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                            feilutbetaling = erFeilutbetalingPåBehandling,
                            etterbetaling = etterbetaling,
                    ),
                    flettefelter = OpphørMedEndringData.Flettefelter(
                            navn = vedtakFellesfelter.søkerNavn,
                            fodselsnummer = vedtakFellesfelter.søkerFødselsnummer,
                    ),
                    perioder = vedtakFellesfelter.perioder,
            ))
}

data class OpphørMedEndringData(
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
            val signaturVedtak: SignaturVedtak,
            val feilutbetaling: Boolean,
            val hjemmeltekst: Hjemmeltekst,
            val etterbetaling: Etterbetaling?,
    )
}