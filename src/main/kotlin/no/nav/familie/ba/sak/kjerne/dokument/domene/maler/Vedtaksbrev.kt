package no.nav.familie.ba.sak.kjerne.dokument.domene.maler

import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.brevperioder.BrevPeriode

interface Vedtaksbrev : Brev {

    override val mal: Brevmal
    override val data: VedtaksbrevData
}

interface VedtaksbrevData : BrevData {

    val perioder: List<BrevPeriode>
}

enum class BrevPeriodeType(val apiNavn: String) {
    INNVILGELSE("innvilgelse"),
    OPPHOR("opphor"),
    AVSLAG("avslag"),
    AVSLAG_UTEN_PERIODE("avslagUtenPeriode"),
    FORTSATT_INNVILGET("fortsattInnvilget"),
}

enum class EndretUtbetalingBrevPeriodeType(val apiNavn: String) {
    ENDRET_UTBETALINGSPERIODE("endretUtbetalingsperiode"),
    ENDRET_UTBETALINGSPERIODE_DELVIS_UTBETALING("endretUtbetalingsperiodeDelvisUtbetaling"),
    ENDRET_UTBETALINGSPERIODE_INGEN_UTBETALING("endretUtbetalingsperiodeIngenUtbetaling"),
}

data class VedtakFellesfelter(
    val enhet: String,
    val saksbehandler: String,
    val beslutter: String,
    val hjemmeltekst: Hjemmeltekst,
    val søkerNavn: String,
    val søkerFødselsnummer: String,
    val perioder: List<BrevPeriode>
)
