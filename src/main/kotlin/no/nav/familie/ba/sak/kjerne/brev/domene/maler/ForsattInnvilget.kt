package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode

data class ForsattInnvilget(
    override val mal: Brevmal = Brevmal.VEDTAK_FORTSATT_INNVILGET,
    override val data: ForsattInnvilgetData
) : Vedtaksbrev {

    constructor(
        vedtakFellesfelter: VedtakFellesfelter
    ) :
        this(
            data = ForsattInnvilgetData(
                delmalData = ForsattInnvilgetData.Delmaler(
                    signaturVedtak = SignaturVedtak(
                        enhet = vedtakFellesfelter.enhet,
                        saksbehandler = vedtakFellesfelter.saksbehandler,
                        beslutter = vedtakFellesfelter.beslutter
                    ),
                    hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                ),
                flettefelter = FlettefelterForDokumentImpl(
                    navn = vedtakFellesfelter.søkerNavn,
                    fodselsnummer = vedtakFellesfelter.søkerFødselsnummer,
                ),
                perioder = vedtakFellesfelter.perioder,
            )
        )
}

data class ForsattInnvilgetData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentImpl,
    override val perioder: List<BrevPeriode>
) : VedtaksbrevData {

    data class Delmaler(
        val signaturVedtak: SignaturVedtak,
        val hjemmeltekst: Hjemmeltekst
    )
}
