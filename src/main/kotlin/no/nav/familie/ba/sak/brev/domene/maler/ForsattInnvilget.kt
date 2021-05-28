package no.nav.familie.ba.sak.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class ForsattInnvilget(
        override val type: Vedtaksbrevtype = Vedtaksbrevtype.FORTSATT_INNVILGET,
        override val data: ForsattInnvilgetData
) : Vedtaksbrev {

    constructor(
            vedtakFellesfelter: VedtakFellesfelter,
    ) :
            this(data = ForsattInnvilgetData(
                    delmalData = ForsattInnvilgetData.Delmaler(
                            signaturVedtak = SignaturVedtak(
                                    enhet = vedtakFellesfelter.enhet,
                                    saksbehandler = vedtakFellesfelter.saksbehandler,
                                    beslutter = vedtakFellesfelter.beslutter),
                            hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                    ),
                    flettefelter = ForsattInnvilgetData.Flettefelter(
                            navn = vedtakFellesfelter.søkerNavn,
                            fodselsnummer = vedtakFellesfelter.søkerFødselsnummer,
                    ),
                    perioder = vedtakFellesfelter.perioder,
            ))
}

data class ForsattInnvilgetData(
        override val delmalData: Delmaler,
        override val flettefelter: Flettefelter,
        override val perioder: Perioder
) : VedtaksbrevData {

    data class Flettefelter(
            val navn: Flettefelt,
            val fodselsnummer: Flettefelt,
            val dato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
    ) {

        constructor(
                navn: String,
                fodselsnummer: String,
        ) : this(
                navn = flettefelt(navn),
                fodselsnummer = flettefelt(fodselsnummer),
        )
    }

    data class Delmaler(
            val signaturVedtak: SignaturVedtak,
            val hjemmeltekst: Hjemmeltekst,
    )
}
