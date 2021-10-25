package no.nav.familie.ba.sak.kjerne.dokument.domene.maler

import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.brevperioder.BrevPeriode

data class OpphørMedEndring(
    override val mal: Brevmal = Brevmal.VEDTAK_OPPHØR_MED_ENDRING,
    override val data: OpphørMedEndringData
) : Vedtaksbrev {

    constructor(
        vedtakFellesfelter: VedtakFellesfelter,

        etterbetaling: Etterbetaling?,
        erFeilutbetalingPåBehandling: Boolean,
    ) :
        this(
            data = OpphørMedEndringData(
                delmalData = OpphørMedEndringData.Delmaler(
                    signaturVedtak = SignaturVedtak(
                        enhet = vedtakFellesfelter.enhet,
                        saksbehandler = vedtakFellesfelter.saksbehandler,
                        beslutter = vedtakFellesfelter.beslutter
                    ),
                    hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                    feilutbetaling = erFeilutbetalingPåBehandling,
                    etterbetaling = etterbetaling,
                ),
                flettefelter = FlettefelterForDokumentImpl(
                    navn = vedtakFellesfelter.søkerNavn,
                    fodselsnummer = vedtakFellesfelter.søkerFødselsnummer,
                ),
                perioder = vedtakFellesfelter.perioder,
            )
        )
}

data class OpphørMedEndringData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentImpl,
    override val perioder: List<BrevPeriode>
) : VedtaksbrevData {
    data class Delmaler(
        val signaturVedtak: SignaturVedtak,
        val feilutbetaling: Boolean,
        val hjemmeltekst: Hjemmeltekst,
        val etterbetaling: Etterbetaling?,
    )
}
