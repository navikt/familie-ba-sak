package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType

fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.erOppdatertMedSisteSatsForAlleSatstyper(): Boolean =
    SatsType.values()
        .filter { it != SatsType.FINN_SVAL }
        .all { this.erOppdatertFor(it) }

private fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.erOppdatertFor(satstype: SatsType): Boolean {
    val sisteSatsForSatstype = SatsService.finnSisteSatsFor(satstype)
    val fomSisteSatsForSatstype = sisteSatsForSatstype.gyldigFom.toYearMonth()

    val satsTyperMedTilsvarendeYtelsestype = satstype
        .tilYtelseType()
        .hentSatsTyper()

    return this.filter { it.stønadTom.isSameOrAfter(fomSisteSatsForSatstype) }
        .filter { it.type == satstype.tilYtelseType() }
        .all { andelTilkjentYtelse ->
            satsTyperMedTilsvarendeYtelsestype
                .any { andelTilkjentYtelse.sats == SatsService.finnSisteSatsFor(it).beløp }
        }
}
