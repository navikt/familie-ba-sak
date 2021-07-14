package no.nav.familie.ba.sak.kjerne.dokument.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class AutovedtakNyfødtBarnFraFør(
        override val type: Vedtaksbrevtype = Vedtaksbrevtype.AUTOVEDTAK_NYFØDT_FØRSTE_BARN,
        override val data: AutovedtakNyfødtBarnFraFørData
) : Vedtaksbrev {

    constructor(
            vedtakFellesfelter: VedtakFellesfelter,
            etterbetaling: Etterbetaling?,
    ) :
            this(data = AutovedtakNyfødtBarnFraFørData(
                    delmalData = AutovedtakNyfødtBarnFraFørData.Delmaler(
                            etterbetaling = etterbetaling,
                            hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                            medVennilgHilsen = MedVennilgHilsen(vedtakFellesfelter.enhet)
                    ),
                    flettefelter = AutovedtakNyfødtBarnFraFørData.Flettefelter(
                            navn = vedtakFellesfelter.søkerNavn,
                            fodselsnummer = vedtakFellesfelter.søkerFødselsnummer),
                    perioder = vedtakFellesfelter.perioder)
            )

}

data class AutovedtakNyfødtBarnFraFørData(
        override val delmalData: Delmaler,
        override val flettefelter: Flettefelter,
        override val perioder: List<BrevPeriode>
) : VedtaksbrevData {

    data class Flettefelter(
            val navn: Flettefelt,
            val fodselsnummer: Flettefelt,
            val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
    ) {

        constructor(navn: String,
                    fodselsnummer: String) : this(navn = flettefelt(navn),
                                                  fodselsnummer = flettefelt(fodselsnummer))
    }

    data class Delmaler(
            val etterbetaling: Etterbetaling?,
            val hjemmeltekst: Hjemmeltekst,
            val medVennilgHilsen: MedVennilgHilsen
    )
}