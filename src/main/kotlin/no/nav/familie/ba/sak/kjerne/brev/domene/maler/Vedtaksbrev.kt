package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.UtbetalingMndEøs

interface Vedtaksbrev : Brev {
    override val mal: Brevmal
    override val data: VedtaksbrevData
}

interface VedtaksbrevData : BrevData

interface VedtaksbrevStandardData : VedtaksbrevData {
    val perioder: List<BrevPeriode>?
    val utbetalingerPerMndEøs: Map<String, UtbetalingMndEøs>?
}

interface VedtaksbrevSammensattKontrollsak : VedtaksbrevData {
    val utbetalingerPerMndEøs: Map<String, UtbetalingMndEøs>?
    val sammensattKontrollsakFritekst: String
}

enum class BrevPeriodeType(
    val apiNavn: String,
) {
    UTBETALING("utbetaling"),
    INGEN_UTBETALING("ingenUtbetaling"),
    INGEN_UTBETALING_UTEN_PERIODE("ingenUtbetalingUtenPeriode"),
    FORTSATT_INNVILGET("fortsattInnvilgetNy"),
    IKKE_RELEVANT("ikkeRelevant"),
}

data class VedtakFellesfelter(
    val enhet: String,
    val saksbehandler: String,
    val beslutter: String,
    val hjemmeltekst: Hjemmeltekst,
    val søkerNavn: String,
    val søkerFødselsnummer: String,
    val perioder: List<BrevPeriode>,
    val utbetalingerPerMndEøs: Map<String, UtbetalingMndEøs>?,
    val organisasjonsnummer: String? = null,
    val gjelder: String? = null,
    val korrigertVedtakData: KorrigertVedtakData? = null,
)

data class VedtakFellesfelterSammensattKontrollsak(
    val enhet: String,
    val saksbehandler: String,
    val beslutter: String,
    val søkerNavn: String,
    val søkerFødselsnummer: String,
    val sammensattKontrollsakFritekst: String,
    val utbetalingerPerMndEøs: Map<String, UtbetalingMndEøs>?,
    val organisasjonsnummer: String? = null,
    val gjelder: String? = null,
    val korrigertVedtakData: KorrigertVedtakData? = null,
)
