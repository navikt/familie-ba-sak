package no.nav.familie.ba.sak.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class Innvilgelsesvedtak(
        override val type: BrevType = BrevType.VEDTAK_INNVILGELSE, override val data: InnvilgelsesvedtakData
) : Vedtaksbrev

data class InnvilgelsesvedtakData(
        override val delmalData: Delmaler,
        override val flettefelter: Flettefelter,
        override val perioder: Perioder
) : VedtaksbrevData {

    data class Flettefelter(
            val navn: Flettefelt,
            val fodselsnummer: Flettefelt,
            val dato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
            val hjemler: Flettefelt,
    ) {

        constructor(navn: String,
                    fodselsnummer: String,
                    hjemler: String) : this(navn = flettefelt(navn),
                                            fodselsnummer = flettefelt(fodselsnummer),
                                            hjemler = flettefelt(hjemler))
    }

    data class Delmaler(
            val signaturVedtak: SignaturVedtatk,
            val etterbetaling: Etterbetaling?,
    )
}