package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SatsendringServiceTest {

    val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    val andelerTilkjentYtelseOgEndreteUtbetalingerService = mockk<AndelerTilkjentYtelseOgEndreteUtbetalingerService>()

    val satsendringService = SatsendringService(
        behandlingHentOgPersisterService,
        andelerTilkjentYtelseOgEndreteUtbetalingerService
    )

    @BeforeEach
    fun init() {
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(any()) } returns lagBehandling()
    }

    @Test
    fun `Skal returnere true dersom vi har siste sats`() {
        val andelerMedSisteSats = SatsType.values()
            .filter { it != SatsType.FINN_SVAL }
            .map {
                val sisteSats = SatsService.finnSisteSatsFor(it)
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = sisteSats.gyldigFom.toYearMonth(),
                    tom = sisteSats.gyldigTom.toYearMonth(),
                    beløp = sisteSats.beløp,
                    ytelseType = it.tilYtelseType()
                )
            }

        every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(any()) } returns andelerMedSisteSats

        assertTrue(satsendringService.erFagsakOppdatertMedSisteSats(1L))
    }

    @Test
    fun `Skal returnere false dersom vi ikke har siste sats`() {
        SatsType.values()
            .filter { it != SatsType.FINN_SVAL }
            .forEach {
                val sisteSats = SatsService.finnSisteSatsFor(it)
                val andelerMedFeilSats = listOf(
                    lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                        fom = sisteSats.gyldigFom.toYearMonth(),
                        tom = sisteSats.gyldigTom.toYearMonth(),
                        beløp = sisteSats.beløp - 1,
                        ytelseType = it.tilYtelseType()
                    )
                )

                every {
                    andelerTilkjentYtelseOgEndreteUtbetalingerService
                        .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(any())
                } returns andelerMedFeilSats

                assertFalse(satsendringService.erFagsakOppdatertMedSisteSats(1L))
            }
    }

    @Test
    fun `Skal ignorere andeler som ikke overlapper siste sats`() {
        SatsType.values()
            .filter { it != SatsType.FINN_SVAL }
            .forEach {
                val sisteSats = SatsService.finnSisteSatsFor(it)
                val andelerSomErFørSisteSats = listOf(
                    lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                        fom = sisteSats.gyldigFom.toYearMonth().minusMonths(100),
                        tom = sisteSats.gyldigFom.toYearMonth().minusMonths(1),
                        beløp = sisteSats.beløp - 1,
                        ytelseType = it.tilYtelseType()
                    )
                )

                every {
                    andelerTilkjentYtelseOgEndreteUtbetalingerService
                        .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(any())
                } returns andelerSomErFørSisteSats

                assertTrue(satsendringService.erFagsakOppdatertMedSisteSats(1L))
            }
    }
}
