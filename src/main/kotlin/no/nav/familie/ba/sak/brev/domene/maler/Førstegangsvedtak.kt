package no.nav.familie.ba.sak.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class Førstegangsvedtak(
        override val type: Vedtaksbrevtype = Vedtaksbrevtype.FØRSTEGANGSVEDTAK,
        override val data: FørstegangsvedtakData
) : Vedtaksbrev {

    constructor(
            vedtakFellesFelter: VedtakFellesFelter,
            etterbetalingsbeløp: String?,
    ) :
            this(data = FørstegangsvedtakData(
                    delmalData = FørstegangsvedtakData.Delmaler(
                            signaturVedtak = SignaturVedtatk(
                                    enhet = vedtakFellesFelter.enhet,
                                    saksbehandler = vedtakFellesFelter.saksbehandler,
                                    beslutter = vedtakFellesFelter.beslutter),
                            etterbetaling = if (!etterbetalingsbeløp.isNullOrBlank()) {
                                Etterbetaling(etterbetalingsbeløp = etterbetalingsbeløp)
                            } else {
                                null
                            },
                            hjemmeltekst = Hjemmeltekst(
                                    hjemler = vedtakFellesFelter.hjemler)),
                    flettefelter = FørstegangsvedtakData.Flettefelter(
                            navn = vedtakFellesFelter.søkerNavn,
                            fodselsnummer = vedtakFellesFelter.søkerFødselsnummer),
                    perioder = vedtakFellesFelter.perioder)
            )

}

data class FørstegangsvedtakData(
        override val delmalData: Delmaler,
        override val flettefelter: Flettefelter,
        override val perioder: Perioder
) : VedtaksbrevData {

    data class Flettefelter(
            val navn: Flettefelt,
            val fodselsnummer: Flettefelt,
            val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
            // TODO: Fjern etter at brevOpprettetDato er lagt til i familie brev. dato -> brevOpprettetDato
            val dato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
    ) {

        constructor(navn: String,
                    fodselsnummer: String) : this(navn = flettefelt(navn),
                                                  fodselsnummer = flettefelt(fodselsnummer))
    }

    data class Delmaler(
            val signaturVedtak: SignaturVedtatk,
            val etterbetaling: Etterbetaling?,
            val hjemmeltekst: Hjemmeltekst
    )
}