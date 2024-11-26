package no.nav.familie.ba.sak.kjerne.eøs.utbetaling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.simulering.lagBehandling
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class UtbetalingTidslinjeServiceTest {
    val beregningService: BeregningService = mockk()
    val utbetalingTidslinjeService = UtbetalingTidslinjeService(beregningService)

    @Test
    fun `Skal returnere emptyMap hvis det ikke finnes noen endringer eller utbetaling av utvidet`() {
        // Arrange
        val behandling = lagBehandling()
        every { beregningService.hentAndelerTilkjentYtelseForBehandling(behandling.id) } returns emptyList()

        // Act
        val resultatMap = utbetalingTidslinjeService.hentUtbetalesIkkeOrdinærEllerUtvidetTidslinjer(behandlingId = BehandlingId(behandling.id), endretUtbetalingAndeler = emptyList())

        // Assert
        assertEquals(emptyMap<Aktør, Tidslinje<Boolean, Måned>>(), resultatMap)
    }

    @Test
    fun `Skal returnere false-periode hvis det finnes utbetaling av utvidet når ordinær er endret til 0kr`() {
        // Arrange
        val behandling = lagBehandling()
        val fomUtvidetOgEndring = YearMonth.now().minusYears(2)
        val tomUtvidet = YearMonth.now().plusYears(1)
        every { beregningService.hentAndelerTilkjentYtelseForBehandling(behandling.id) } returns
            listOf(
                lagAndelTilkjentYtelse(
                    fom = fomUtvidetOgEndring,
                    tom = tomUtvidet,
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ),
            )
        val barn = lagPerson(type = PersonType.BARN)
        val endretUtbetalingAndel = lagEndretUtbetalingAndel(behandlingId = behandling.id, person = barn, prosent = BigDecimal.ZERO, årsak = Årsak.ALLEREDE_UTBETALT, fom = fomUtvidetOgEndring, tom = YearMonth.now())

        // Act
        val resultatMap = utbetalingTidslinjeService.hentUtbetalesIkkeOrdinærEllerUtvidetTidslinjer(behandlingId = BehandlingId(behandling.id), endretUtbetalingAndeler = listOf(endretUtbetalingAndel))

        // Assert
        assertEquals(1, resultatMap.size)

        val tidslinjeForBarn = resultatMap[barn.aktør]
        val perioderForBarn = tidslinjeForBarn?.perioder() ?: emptyList()

        assertEquals(1, perioderForBarn.size)

        val periode = perioderForBarn.first()

        assertEquals(fomUtvidetOgEndring, periode.fraOgMed.tilYearMonth())
        assertEquals(tomUtvidet, periode.tilOgMed.tilYearMonth())
        assertFalse(periode.innhold!!)
    }
}
