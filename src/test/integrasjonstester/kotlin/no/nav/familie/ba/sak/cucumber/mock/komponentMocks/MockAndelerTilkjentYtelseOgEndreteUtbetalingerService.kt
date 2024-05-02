package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.BegrunnelseTeksterStepDefinition
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilAndelerTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilEndretUtbetalingAndelMedAndelerTilkjentYtelse

fun mockAndelerTilkjentYtelseOgEndreteUtbetalingerService(dataFraCucumber: BegrunnelseTeksterStepDefinition): AndelerTilkjentYtelseOgEndreteUtbetalingerService {
    val andelerTilkjentYtelseOgEndreteUtbetalingerService = mockk<AndelerTilkjentYtelseOgEndreteUtbetalingerService>()
    every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(any()) } answers {
        val behandlingId = firstArg<Long>()
        val andelerTilkjentYtelse = dataFraCucumber.tilkjenteYtelser[behandlingId]?.andelerTilkjentYtelse?.toList() ?: emptyList()
        val endredeUtbetalinger = dataFraCucumber.endredeUtbetalinger[behandlingId] ?: emptyList()

        andelerTilkjentYtelse.tilAndelerTilkjentYtelseMedEndreteUtbetalinger(endredeUtbetalinger)
    }
    every {
        andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(any())
    } answers {
        val behandlingId = firstArg<Long>()
        val andelerTilkjentYtelse = dataFraCucumber.tilkjenteYtelser[behandlingId]?.andelerTilkjentYtelse?.toList() ?: emptyList()
        val endredeUtbetalinger = dataFraCucumber.endredeUtbetalinger[behandlingId] ?: emptyList()

        endredeUtbetalinger.tilEndretUtbetalingAndelMedAndelerTilkjentYtelse(andelerTilkjentYtelse)
    }
    return andelerTilkjentYtelseOgEndreteUtbetalingerService
}
