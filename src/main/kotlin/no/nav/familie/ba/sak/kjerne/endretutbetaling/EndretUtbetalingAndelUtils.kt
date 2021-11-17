package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import java.time.YearMonth

fun erStartPåUtvidetSammeMåned(
    andelTilkjentYtelser: List<AndelTilkjentYtelse>,
    fom: YearMonth?,
) = andelTilkjentYtelser.any { it.stønadFom == fom && it.type == YtelseType.UTVIDET_BARNETRYGD }
