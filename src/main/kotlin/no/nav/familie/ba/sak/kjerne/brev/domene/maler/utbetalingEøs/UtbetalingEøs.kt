package no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs

data class AndelUpbOgValutakurs(
    val andelTilkjentYtelse: AndelTilkjentYtelse,
    val utenlandskPeriodebeløp: UtenlandskPeriodebeløp?,
    val valutakurs: Valutakurs?,
)

data class UtbetalingMndEøs(
    val utbetalinger: List<UtbetalingEøs>,
    val oppsummering: UtbetalingMndEøsOppsummering,
)

class UtbetalingMndEøsOppsummering(
    val summertSatsINorge: Int,
    val summertUtbetaltFraAnnetLand: Int?,
    val summertUtbetaltFraNorge: Int,
)

data class UtbetalingEøs(
    val fnr: String,
    val ytelseType: YtelseType,
    val satsINorge: Int,
    val utbetaltFraAnnetLand: UtbetaltFraAnnetLand?,
    val utbetaltFraNorge: Int,
)

data class UtbetaltFraAnnetLand(
    val beløp: Int,
    val valutakode: String,
    val beløpINok: Int,
)
