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
    UTBETALING("utbetaling"),
    INGEN_UTBETALING("ingenUtbetaling"),
    INGEN_UTBETALING_UTEN_PERIODE("ingenUtbetalingUtenPeriode"),
    FORTSATT_INNVILGET("fortsattInnvilget"),
    IKKE_RELEVANT("ikkeRelevant"),

    @Deprecated("Skal renames til FORTSATT_INNVILGET når det gamle implementasjonen er fjernet")
    FORTSATT_INNVILGET_NY("fortsattInnvilgetNy"),

    @Deprecated("Kun UTBETALING, INGEN_UTBETALING, INGEN_UTBETALING_UTEN_PERIODE, FORTSATT_INNVILGET skal brukes")
    INNVILGELSE("innvilgelse"),

    @Deprecated("Kun UTBETALING, INGEN_UTBETALING, INGEN_UTBETALING_UTEN_PERIODE, FORTSATT_INNVILGET skal brukes")
    AVSLAG("avslag"),
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
