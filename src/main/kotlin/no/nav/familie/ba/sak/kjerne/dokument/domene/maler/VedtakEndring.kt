package no.nav.familie.ba.sak.kjerne.dokument.domene.maler

data class VedtakEndring(
        override val mal: Brevmal = Brevmal.VEDTAK_ENDRING,
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
                    flettefelter = FlettefelterForDokumentImpl(
                            navn = vedtakFellesfelter.søkerNavn,
                            fodselsnummer = vedtakFellesfelter.søkerFødselsnummer),
                    perioder = vedtakFellesfelter.perioder)
            )
}

data class EndringVedtakData(
        override val delmalData: Delmaler,
        override val flettefelter: FlettefelterForDokumentImpl,
        override val perioder: List<BrevPeriode>
) : VedtaksbrevData {

    data class Delmaler(
            val signaturVedtak: SignaturVedtak,
            val etterbetaling: Etterbetaling?,
            val feilutbetaling: Boolean,
            val hjemmeltekst: Hjemmeltekst,
            val klage: Boolean,
    )
}