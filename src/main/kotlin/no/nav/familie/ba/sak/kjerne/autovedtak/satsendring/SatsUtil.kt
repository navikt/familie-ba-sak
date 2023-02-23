package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import java.time.YearMonth

fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.erOppdatertMedSatserTilOgMed(
    satstidspunkt: YearMonth
): Boolean {
    val atyPåSatstidspunkt = filter {
        it.stønadFom.isSameOrBefore(satstidspunkt) && it.stønadTom.isSameOrAfter(
            satstidspunkt
        )
    }

    val atySmåbarnstillegg = atyPåSatstidspunkt.filter { it.type == YtelseType.SMÅBARNSTILLEGG }
    if (atySmåbarnstillegg.isNotEmpty()) {
        val harGammelSats =
            atySmåbarnstillegg.any { it.sats != SatsService.finnSisteSatsFor(SatsType.SMA).beløp }
        if (harGammelSats) return false
    }

    val atyUtvidet = atyPåSatstidspunkt.filter { it.type == YtelseType.UTVIDET_BARNETRYGD }
    if (atyUtvidet.isNotEmpty()) {
        val harGammelSats =
            atyUtvidet.any { it.sats != SatsService.finnSisteSatsFor(SatsType.UTVIDET_BARNETRYGD).beløp }
        if (harGammelSats) return false
    }

    val atyOrdinær = atyPåSatstidspunkt.filter { it.type == YtelseType.ORDINÆR_BARNETRYGD }
    if (atyOrdinær.isNotEmpty()) {
        val satser = atyOrdinær.map { it.sats }
        val gyldigeOrdinæreSatser = listOf(
            SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
            SatsService.finnSisteSatsFor(SatsType.TILLEGG_ORBA).beløp
        )
        satser.forEach { if (!gyldigeOrdinæreSatser.contains(it)) return false }
    }
    return true
}