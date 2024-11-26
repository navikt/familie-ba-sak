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
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class UtbetalingTidslinjeServiceTest {
    val beregningService: BeregningService = mockk()
    val utbetalingTidslinjeService = UtbetalingTidslinjeService(beregningService)

    @Test
    fun `Skal returnere emptyMap hvis det ikke finnes noen endringer eller utbetaling utvidet`() {
        val behandling = lagBehandling()
        every { beregningService.hentAndelerTilkjentYtelseForBehandling(behandling.id) } returns emptyList()
        val resultat = utbetalingTidslinjeService.hentUtbetalesIkkeOrdinærEllerUtvidetTidslinjer(BehandlingId(behandling.id), emptyList())
        assertEquals(emptyMap<Aktør, Tidslinje<Boolean, Måned>>(), resultat)
    }

    @Test
    fun `Test blabla`() {
        val behandling = lagBehandling()
        val fom = YearMonth.now().minusYears(2)
        every { beregningService.hentAndelerTilkjentYtelseForBehandling(behandling.id) } returns
            listOf(
                lagAndelTilkjentYtelse(
                    fom = fom,
                    tom = YearMonth.now().plusYears(1),
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ),
            )
        val barn = lagPerson(type = PersonType.BARN)
        val endretUtbetalingsAndel = lagEndretUtbetalingAndel(behandlingId = behandling.id, person= barn, prosent = BigDecimal.ZERO, årsak = Årsak.ALLEREDE_UTBETALT, fom= fom, tom= YearMonth.now())
        val resultat = utbetalingTidslinjeService.hentUtbetalesIkkeOrdinærEllerUtvidetTidslinjer(BehandlingId(behandling.id), listOf(endretUtbetalingsAndel))
        assertEquals(1, resultat.size)
        val tidslinje = resultat[barn.aktør]
        val perioder = tidslinje?.perioder() ?: emptyList()
        assertEquals(1, perioder.size)
        val periode = perioder.first()
        assertEquals(fom, periode.fraOgMed.tilYearMonth())
    }
}
