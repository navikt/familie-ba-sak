package no.nav.familie.ba.sak.kjerne.dokument.domene.maler

data class Avslag(
        override val mal: Brevmal = Brevmal.AVSLAG,
        override val data: AvslagData
) : Vedtaksbrev {

    constructor(
            vedtakFellesfelter: VedtakFellesfelter,
    ) :
            this(data = AvslagData(
                    delmalData = AvslagData.Delmaler(
                            signaturVedtak = SignaturVedtak(
                                    enhet = vedtakFellesfelter.enhet,
                                    saksbehandler = vedtakFellesfelter.saksbehandler,
                                    beslutter = vedtakFellesfelter.beslutter),
                            hjemmeltekst = vedtakFellesfelter.hjemmeltekst),
                    flettefelter = FlettefelterForDokumentImpl(
                            navn = vedtakFellesfelter.søkerNavn,
                            fodselsnummer = vedtakFellesfelter.søkerFødselsnummer),
                    perioder = vedtakFellesfelter.perioder)
            )
}

data class AvslagData(
        override val delmalData: Delmaler,
        override val flettefelter: FlettefelterForDokumentImpl,
        override val perioder: List<BrevPeriode>
) : VedtaksbrevData {

    data class Delmaler(
            val signaturVedtak: SignaturVedtak,
            val hjemmeltekst: Hjemmeltekst,
    )
}