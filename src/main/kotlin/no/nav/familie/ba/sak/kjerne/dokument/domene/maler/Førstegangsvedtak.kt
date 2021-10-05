package no.nav.familie.ba.sak.kjerne.dokument.domene.maler

data class Førstegangsvedtak(
    override val mal: Brevmal = Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
    override val data: FørstegangsvedtakData
) : Vedtaksbrev {

    constructor(
        vedtakFellesfelter: VedtakFellesfelter,
        etterbetaling: Etterbetaling?,
    ) :
        this(
            data = FørstegangsvedtakData(
                delmalData = FørstegangsvedtakData.Delmaler(
                    signaturVedtak = SignaturVedtak(
                        enhet = vedtakFellesfelter.enhet,
                        saksbehandler = vedtakFellesfelter.saksbehandler,
                        beslutter = vedtakFellesfelter.beslutter
                    ),
                    etterbetaling = etterbetaling,
                    hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                ),
                flettefelter = FlettefelterForDokumentImpl(
                    navn = vedtakFellesfelter.søkerNavn,
                    fodselsnummer = vedtakFellesfelter.søkerFødselsnummer
                ),
                perioder = vedtakFellesfelter.perioder
            )
        )
}

data class FørstegangsvedtakData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentImpl,
    override val perioder: List<BrevPeriode>
) : VedtaksbrevData {

    data class Delmaler(
        val signaturVedtak: SignaturVedtak,
        val etterbetaling: Etterbetaling?,
        val hjemmeltekst: Hjemmeltekst
    )
}
