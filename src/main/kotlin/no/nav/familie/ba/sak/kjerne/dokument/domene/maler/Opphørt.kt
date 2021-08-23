package no.nav.familie.ba.sak.kjerne.dokument.domene.maler

data class Opphørt(
        override val mal: Brevmal = Brevmal.VEDTAK_OPPHØRT,
        override val data: OpphørtData
) : Vedtaksbrev {

    constructor(
            vedtakFellesfelter: VedtakFellesfelter,

            erFeilutbetalingPåBehandling: Boolean,
    ) :
            this(data = OpphørtData(
                    delmalData = OpphørtData.Delmaler(
                            signaturVedtak = SignaturVedtak(
                                    enhet = vedtakFellesfelter.enhet,
                                    saksbehandler = vedtakFellesfelter.saksbehandler,
                                    beslutter = vedtakFellesfelter.beslutter),
                            hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                            feilutbetaling = erFeilutbetalingPåBehandling),
                    flettefelter = FlettefelterForDokumentImpl(
                            navn = vedtakFellesfelter.søkerNavn,
                            fodselsnummer = vedtakFellesfelter.søkerFødselsnummer),
                    perioder = vedtakFellesfelter.perioder)
            )
}

data class OpphørtData(
        override val delmalData: Delmaler,
        override val flettefelter: FlettefelterForDokumentImpl,
        override val perioder: List<BrevPeriode>
) : VedtaksbrevData {

    data class Delmaler(
            val signaturVedtak: SignaturVedtak,
            val feilutbetaling: Boolean,
            val hjemmeltekst: Hjemmeltekst,
    )
}