package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode

interface Vedtaksbrev : Brev {

    override val mal: Brevmal
    override val data: VedtaksbrevData
}

interface VedtaksbrevData : BrevData {

    val perioder: List<BrevPeriode>
}

enum class BrevPeriodeType(val apiNavn: String) {
    INNVILGELSE("innvilgelse"),
    INNVILGELSE_INGEN_UTBETALING("innvilgelseIngenUtbetaling"),
    INNVILGELSE_KUN_UTBETALING_PÅ_SØKER("innvilgelseKunUtbetalingPaSoker"),
    OPPHOR("opphor"),
    AVSLAG("avslag"),
    AVSLAG_UTEN_PERIODE("avslagUtenPeriode"),
    FORTSATT_INNVILGET("fortsattInnvilget"),
    INNVILGELSE_INSTITUSJON("innvilgelseInstitusjon"),
    OPPHOR_INSTITUSJON("opphorInstitusjon"),
    AVSLAG_INSTITUSJON("avslagInstitusjon"),
    AVSLAG_UTEN_PERIODE_INSTITUSJON("avslagUtenPeriodeInstitusjon"),
    FORTSATT_INNVILGET_INSTITUSJON("fortsattInnvilgetInstitusjon"),
    UTBETALING("utbetaling"),
    FORTSATT_INNVILGET_NY("fortsattInnvilgetNy"),
    INGEN_UTBETALING("ingenUtbetaling"),
    INGEN_UTBETALING_UTEN_PERIODE("ingenUtbetalingUtenPeriode"),
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
    val perioder: List<BrevPeriode>,
    val organisasjonsnummer: String? = null,
    val gjelder: String? = null,
    val korrigertVedtakData: KorrigertVedtakData? = null,
)
