package no.nav.familie.ba.sak.kjerne.endretutbetaling.domene

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import java.time.YearMonth

fun erUtvidetTilkjentYtelseMedSammeFomSomErUendret(
    andelTilkjentYtelser: List<AndelTilkjentYtelse>,
    fom: YearMonth?,
    tom: YearMonth?,
) = andelTilkjentYtelser
    .filter { it.type == YtelseType.UTVIDET_BARNETRYGD }
    .all { andelTilkjentYtelse ->
        !andelTilkjentYtelse.endretUtbetalingAndeler.any { it.fom == fom && it.tom == tom }
    }
