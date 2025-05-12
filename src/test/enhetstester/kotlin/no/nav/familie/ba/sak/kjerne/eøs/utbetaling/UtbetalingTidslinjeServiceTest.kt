package no.nav.familie.ba.sak.kjerne.eøs.utbetaling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.skalUtbetales
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import no.nav.familie.ba.sak.kjerne.tidslinje.util.nov
import no.nav.familie.ba.sak.kjerne.tidslinje.util.somBoolskTidslinje
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.mapVerdi
import no.nav.familie.tidslinje.utvidelser.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
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
        val resultatMap =
            utbetalingTidslinjeService.hentEndredeUtbetalingsPerioderSomKreverKompetanseTidslinjer(
                behandlingId = BehandlingId(behandling.id),
                endretUtbetalingAndeler = emptyList(),
            )

        // Assert
        assertEquals(emptyMap<Aktør, Tidslinje<Boolean>>(), resultatMap)
    }

    @Test
    fun `Skal returnere emptyMap hvis det finnes utbetaling av utvidet når det ikke finnes endret utbetaling andeler`() {
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
                    kalkulertUtbetalingsbeløp = 1000,
                ),
            )

        // Act
        val resultatMap =
            utbetalingTidslinjeService.hentEndredeUtbetalingsPerioderSomKreverKompetanseTidslinjer(
                behandlingId = BehandlingId(behandling.id),
                endretUtbetalingAndeler = emptyList(),
            )

        // Assert
        assertEquals(emptyMap<Aktør, Tidslinje<Boolean>>(), resultatMap)
    }

    @ParameterizedTest
    @EnumSource(Årsak::class, names = ["ALLEREDE_UTBETALT", "ETTERBETALING_3ÅR", "DELT_BOSTED", "ETTERBETALING_3MND"], mode = EnumSource.Mode.INCLUDE)
    fun `Skal returnere true-periode hvis det finnes utbetaling utvidet når ordinær er endret til 0kr med årsak`(årsak: Årsak) {
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
                    kalkulertUtbetalingsbeløp = 1000,
                ),
            )

        val barn = lagPerson(type = PersonType.BARN)
        val endretUtbetalingAndel =
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                person = barn,
                prosent = BigDecimal.ZERO,
                årsak = årsak,
                fom = fomUtvidetOgEndring,
                tom = tomUtvidet,
            )

        // Act
        val resultatMap =
            utbetalingTidslinjeService.hentEndredeUtbetalingsPerioderSomKreverKompetanseTidslinjer(
                behandlingId = BehandlingId(behandling.id),
                endretUtbetalingAndeler = listOf(endretUtbetalingAndel),
            )

        // Assert
        assertEquals(1, resultatMap.size)

        val tidslinjeForBarn = resultatMap[barn.aktør]
        val perioderForBarn = tidslinjeForBarn?.tilPerioder() ?: emptyList()

        assertEquals(1, perioderForBarn.size)

        val periode = perioderForBarn.first()

        assertEquals(fomUtvidetOgEndring, periode.fom?.toYearMonth())
        assertEquals(tomUtvidet, periode.tom?.toYearMonth())
        assertThat(periode.verdi!!).isTrue
    }

    @ParameterizedTest
    @EnumSource(Årsak::class, names = ["ENDRE_MOTTAKER"], mode = EnumSource.Mode.INCLUDE)
    fun `Skal returnere false-periode hvis det finnes utbetaling utvidet når ordinær er endret til 0kr med årsak`(årsak: Årsak) {
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
                    kalkulertUtbetalingsbeløp = 1000,
                ),
            )

        val barn = lagPerson(type = PersonType.BARN)
        val endretUtbetalingAndel =
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                person = barn,
                prosent = BigDecimal.ZERO,
                årsak = årsak,
                fom = fomUtvidetOgEndring,
                tom = tomUtvidet,
            )

        // Act
        val resultatMap =
            utbetalingTidslinjeService.hentEndredeUtbetalingsPerioderSomKreverKompetanseTidslinjer(
                behandlingId = BehandlingId(behandling.id),
                endretUtbetalingAndeler = listOf(endretUtbetalingAndel),
            )

        // Assert
        assertEquals(1, resultatMap.size)

        val tidslinjeForBarn = resultatMap[barn.aktør]
        val perioderForBarn = tidslinjeForBarn?.tilPerioder() ?: emptyList()

        assertEquals(1, perioderForBarn.size)

        val periode = perioderForBarn.first()

        assertEquals(fomUtvidetOgEndring, periode.fom?.toYearMonth())
        assertEquals(tomUtvidet, periode.tom?.toYearMonth())
        assertThat(periode.verdi!!).isFalse
    }

    @Test
    fun `Skal returnere false-periode hvis det finnes endring hvor ordinær er satt til 0kr og det ikke betales ut utvidet`() {
        // Arrange
        val behandling = lagBehandling()

        every { beregningService.hentAndelerTilkjentYtelseForBehandling(behandling.id) } returns emptyList()

        val fomEndretUtbetaling = YearMonth.now().minusYears(2)
        val tomEndretUtbetaling = YearMonth.now()
        val barn = lagPerson(type = PersonType.BARN)
        val endretUtbetalingAndel =
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                person = barn,
                prosent = BigDecimal.ZERO,
                årsak = Årsak.ALLEREDE_UTBETALT,
                fom = fomEndretUtbetaling,
                tom = tomEndretUtbetaling,
            )

        // Act
        val resultatMap =
            utbetalingTidslinjeService.hentEndredeUtbetalingsPerioderSomKreverKompetanseTidslinjer(
                behandlingId = BehandlingId(behandling.id),
                endretUtbetalingAndeler = listOf(endretUtbetalingAndel),
            )

        // Assert
        assertEquals(1, resultatMap.size)

        val perioderForBarn = resultatMap[barn.aktør]!!.tilPerioder()

        assertEquals(2, perioderForBarn.size)

        val førstePeriode = perioderForBarn.first()

        assertEquals(fomEndretUtbetaling.minusMonths(1), førstePeriode.tom?.toYearMonth())
        assertThat(førstePeriode.verdi).isNull()

        val periodeUtenUtbetaling = perioderForBarn.last()

        assertEquals(fomEndretUtbetaling, periodeUtenUtbetaling.fom?.toYearMonth())
        assertEquals(tomEndretUtbetaling, periodeUtenUtbetaling.tom?.toYearMonth())
        assertThat(periodeUtenUtbetaling.verdi!!).isFalse
    }

    @Nested
    inner class TilBarnasSkalIkkeUtbetalesTidslinjerTest {
        @Test
        fun `lager tidslinje for ett barn med én etterbetaling`() {
            val person = tilfeldigPerson()
            val endringer =
                listOf(
                    lagEndretUtbetalingAndel(
                        person = person,
                        årsak = Årsak.ETTERBETALING_3ÅR,
                        fom = YearMonth.of(2020, 3),
                        tom = YearMonth.of(2020, 7),
                        prosent = BigDecimal.ZERO,
                    ),
                )

            val forventet =
                mapOf(
                    person.aktør to "TTTTT".somBoolskTidslinje(mar(2020)),
                )

            val faktisk =
                endringer.tilBarnasEndretUtbetalingSkalIkkeUtbetalesTidslinjer().mapValues { (_, tidslinje) ->
                    tidslinje.mapVerdi { !it.skalUtbetales() }
                }

            assertEquals(forventet, faktisk)
        }

        @Test
        fun `lager tidslinje for to barn med flere etterbetalinger`() {
            val person1 = tilfeldigPerson()
            val person2 = tilfeldigPerson()

            val endringer =
                listOf(
                    lagEndretUtbetalingAndel(
                        person = person1,
                        årsak = Årsak.ETTERBETALING_3ÅR,
                        fom = YearMonth.of(2020, 3),
                        tom = YearMonth.of(2020, 7),
                        prosent = BigDecimal.ZERO,
                    ),
                    lagEndretUtbetalingAndel(
                        person = person2,
                        årsak = Årsak.ETTERBETALING_3ÅR,
                        fom = YearMonth.of(2019, 11),
                        tom = YearMonth.of(2021, 3),
                        prosent = BigDecimal.ZERO,
                    ),
                    lagEndretUtbetalingAndel(
                        person = person1,
                        årsak = Årsak.ETTERBETALING_3ÅR,
                        fom = YearMonth.of(2021, 1),
                        tom = YearMonth.of(2021, 5),
                        prosent = BigDecimal.ZERO,
                    ),
                )

            val forventet =
                mapOf(
                    person1.aktør to "TTTTT     TTTTT".somBoolskTidslinje(mar(2020)).filtrerIkkeNull(),
                    person2.aktør to "TTTTTTTTTTTTTTTTT".somBoolskTidslinje(nov(2019)).filtrerIkkeNull(),
                )

            val faktisk =
                endringer.tilBarnasEndretUtbetalingSkalIkkeUtbetalesTidslinjer().mapValues { (_, tidslinje) ->
                    tidslinje.mapVerdi { !it.skalUtbetales() }
                }

            assertEquals(forventet, faktisk)
        }

        @Test
        fun `lager tidslinje for ett barn med allerede utbetalt`() {
            val person = tilfeldigPerson()
            val endringer =
                listOf(
                    lagEndretUtbetalingAndel(
                        person = person,
                        årsak = Årsak.ALLEREDE_UTBETALT,
                        fom = YearMonth.of(2020, 3),
                        tom = YearMonth.of(2020, 7),
                        prosent = BigDecimal.ZERO,
                    ),
                )

            val forventet =
                mapOf(
                    person.aktør to "TTTTT".somBoolskTidslinje(mar(2020)),
                )

            val faktisk =
                endringer.tilBarnasEndretUtbetalingSkalIkkeUtbetalesTidslinjer().mapValues { (_, tidslinje) ->
                    tidslinje.mapVerdi { !it.skalUtbetales() }
                }

            assertEquals(forventet, faktisk)
        }
    }
}
