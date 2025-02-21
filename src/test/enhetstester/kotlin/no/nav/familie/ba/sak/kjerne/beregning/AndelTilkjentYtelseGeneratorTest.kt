package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseGenerator.oppdaterAndelerMedEndretUtbetalingAndeler
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseGenerator.slåSammenEtterfølgende0krAndelerPgaSammeEndretAndel
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseGenerator.tilAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseGenerator.tilAndelerTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class AndelTilkjentYtelseGeneratorTest {
    @Test
    fun `endret utbetalingsandel skal overstyre andel`() {
        val person = lagPerson()
        val behandling = lagBehandling()
        val fom = YearMonth.of(2018, 1)
        val tom = YearMonth.of(2019, 1)
        val utbetalingsandeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = fom,
                    tom = tom,
                    person = person,
                    behandling = behandling,
                ),
            )

        val endretProsent = BigDecimal.ZERO

        val endretUtbetalingAndeler =
            listOf(
                lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                    person = person,
                    fom = fom,
                    tom = tom,
                    prosent = endretProsent,
                    behandlingId = behandling.id,
                ),
            )

        val andelerTIlkjentYtelse =
            oppdaterAndelerMedEndretUtbetalingAndeler(
                utbetalingsandeler,
                endretUtbetalingAndeler,
                utbetalingsandeler.first().tilkjentYtelse,
            )

        assertThat(andelerTIlkjentYtelse.size).isEqualTo(1)
        assertThat(andelerTIlkjentYtelse.single().prosent).isEqualTo(endretProsent)
        assertThat(andelerTIlkjentYtelse.single().endreteUtbetalinger.size).isEqualTo(1)
    }

    @Test
    fun `endret utbetalingsandel koble endrede andeler til riktig endret utbetalingandel`() {
        val person = lagPerson()
        val behandling = lagBehandling()
        val fom1 = YearMonth.of(2018, 1)
        val tom1 = YearMonth.of(2018, 11)

        val fom2 = YearMonth.of(2019, 1)
        val tom2 = YearMonth.of(2019, 11)

        val utbetalingsandeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = fom1,
                    tom = tom1,
                    person = person,
                    behandling = behandling,
                ),
                lagAndelTilkjentYtelse(
                    fom = fom2,
                    tom = tom2,
                    person = person,
                    behandling = behandling,
                ),
            )

        val endretProsent = BigDecimal.ZERO

        val endretUtbetalingAndel =
            lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                person = person,
                fom = fom1,
                tom = tom2,
                prosent = endretProsent,
                behandlingId = behandling.id,
            )

        val endretUtbetalingAndeler =
            listOf(
                endretUtbetalingAndel,
                lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                    person = person,
                    fom = tom2.nesteMåned(),
                    prosent = endretProsent,
                    behandlingId = behandling.id,
                ),
            )

        val andelerTIlkjentYtelse =
            oppdaterAndelerMedEndretUtbetalingAndeler(
                utbetalingsandeler,
                endretUtbetalingAndeler,
                utbetalingsandeler.first().tilkjentYtelse,
            )

        assertThat(andelerTIlkjentYtelse.size).isEqualTo(2)
        andelerTIlkjentYtelse.forEach { assertThat(it.prosent).isEqualTo(endretProsent) }
        andelerTIlkjentYtelse.forEach { assertThat(it.endreteUtbetalinger.size).isEqualTo(1) }
        andelerTIlkjentYtelse.forEach {
            assertThat(
                it.endreteUtbetalinger.single().id,
            ).isEqualTo(endretUtbetalingAndel.id)
        }
    }

    @Nested
    inner class SlåSammenEtterfølgendeAndelerTest {
        @Test
        fun `skal ikke slå sammen etterfølgende 0kr-andeler hvis de ikke skyldes samme endret utbetaling andel`() {
            // Arrange
            val barn = lagPerson(type = PersonType.BARN)
            val andeler =
                listOf(
                    Periode(
                        fom = LocalDate.now().minusMonths(9).førsteDagIInneværendeMåned(),
                        tom = LocalDate.now().minusMonths(5).sisteDagIMåned(),
                        verdi =
                            AndelTilkjentYtelseGenerator.AndelMedEndretUtbetalingForTidslinje(
                                aktør = barn.aktør,
                                beløp = 0,
                                sats = 1054,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                prosent = BigDecimal.ZERO,
                                endretUtbetalingAndel =
                                    EndretUtbetalingAndelMedAndelerTilkjentYtelse(
                                        andeler = emptyList(),
                                        endretUtbetalingAndel =
                                            lagEndretUtbetalingAndel(
                                                person = barn,
                                                prosent = BigDecimal.ZERO,
                                                årsak = Årsak.ETTERBETALING_3MND,
                                            ),
                                    ),
                            ),
                    ),
                    Periode(
                        fom = LocalDate.now().minusMonths(4).førsteDagIInneværendeMåned(),
                        tom = LocalDate.now().sisteDagIMåned(),
                        verdi =
                            AndelTilkjentYtelseGenerator.AndelMedEndretUtbetalingForTidslinje(
                                aktør = barn.aktør,
                                beløp = 0,
                                sats = 1054,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                prosent = BigDecimal.ZERO,
                                endretUtbetalingAndel =
                                    EndretUtbetalingAndelMedAndelerTilkjentYtelse(
                                        andeler = emptyList(),
                                        endretUtbetalingAndel =
                                            lagEndretUtbetalingAndel(
                                                person = barn,
                                                prosent = BigDecimal.ZERO,
                                                årsak = Årsak.ALLEREDE_UTBETALT,
                                            ),
                                    ),
                            ),
                    ),
                )

            // Act
            val perioderEtterSammenslåing =
                andeler.tilTidslinje().slåSammenEtterfølgende0krAndelerPgaSammeEndretAndel().tilPerioderIkkeNull()

            // Assert
            assertThat(perioderEtterSammenslåing.size).isEqualTo(2)
        }

        @Test
        fun `skal ikke slå sammen 0kr-andeler som har tom periode mellom seg`() {
            // Arrange
            val barn = lagPerson(type = PersonType.BARN)
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(person = barn, prosent = BigDecimal.ZERO, årsak = Årsak.ALLEREDE_UTBETALT)
            val andeler =
                listOf(
                    Periode(
                        fom = LocalDate.now().minusMonths(9).førsteDagIInneværendeMåned(),
                        tom = LocalDate.now().minusMonths(5).sisteDagIMåned(),
                        verdi =
                            AndelTilkjentYtelseGenerator.AndelMedEndretUtbetalingForTidslinje(
                                aktør = barn.aktør,
                                beløp = 0,
                                sats = 1054,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                prosent = BigDecimal.ZERO,
                                endretUtbetalingAndel =
                                    EndretUtbetalingAndelMedAndelerTilkjentYtelse(
                                        andeler = emptyList(),
                                        endretUtbetalingAndel = endretUtbetalingAndel,
                                    ),
                            ),
                    ),
                    Periode(
                        fom = LocalDate.now().minusMonths(2).førsteDagIInneværendeMåned(),
                        tom = LocalDate.now().sisteDagIMåned(),
                        verdi =
                            AndelTilkjentYtelseGenerator.AndelMedEndretUtbetalingForTidslinje(
                                aktør = barn.aktør,
                                beløp = 0,
                                sats = 1054,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                prosent = BigDecimal.ZERO,
                                endretUtbetalingAndel =
                                    EndretUtbetalingAndelMedAndelerTilkjentYtelse(
                                        andeler = emptyList(),
                                        endretUtbetalingAndel = endretUtbetalingAndel,
                                    ),
                            ),
                    ),
                )

            // Act
            val perioderEtterSammenslåing =
                andeler.tilTidslinje().slåSammenEtterfølgende0krAndelerPgaSammeEndretAndel().tilPerioderIkkeNull()

            // Assert
            assertThat(perioderEtterSammenslåing.size).isEqualTo(2)
        }

        @Test
        fun `skal ikke slå sammen etterfølgende andeler med 100 prosent utbetaling av ulik sats`() {
            // Arrange
            val barn = lagPerson(type = PersonType.BARN)
            val andeler =
                listOf(
                    Periode(
                        fom = LocalDate.now().minusMonths(9).førsteDagIInneværendeMåned(),
                        tom = LocalDate.now().minusMonths(5).sisteDagIMåned(),
                        verdi =
                            AndelTilkjentYtelseGenerator.AndelMedEndretUtbetalingForTidslinje(
                                aktør = barn.aktør,
                                beløp = 1054,
                                sats = 1054,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                prosent = BigDecimal(100),
                                endretUtbetalingAndel = null,
                            ),
                    ),
                    Periode(
                        fom = LocalDate.now().minusMonths(4).førsteDagIInneværendeMåned(),
                        tom = LocalDate.now().sisteDagIMåned(),
                        verdi =
                            AndelTilkjentYtelseGenerator.AndelMedEndretUtbetalingForTidslinje(
                                aktør = barn.aktør,
                                beløp = 1766,
                                sats = 1766,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                prosent = BigDecimal(100),
                                endretUtbetalingAndel = null,
                            ),
                    ),
                )

            // Act
            val perioderEtterSammenslåing =
                andeler.tilTidslinje().slåSammenEtterfølgende0krAndelerPgaSammeEndretAndel().tilPerioderIkkeNull()

            // Assert
            assertThat(perioderEtterSammenslåing.size).isEqualTo(2)
        }

        @Test
        fun `skal slå sammen etterfølgende 0kr-andeler som skyldes samme endret andel, men er splittet pga satsendring`() {
            // Arrange
            val barn = lagPerson(type = PersonType.BARN)
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    person = barn,
                    prosent = BigDecimal.ZERO,
                    årsak = Årsak.ALLEREDE_UTBETALT,
                    fom = YearMonth.now().minusMonths(9),
                    tom = YearMonth.now(),
                )
            val andeler =
                listOf(
                    Periode(
                        fom = LocalDate.now().minusMonths(9).førsteDagIInneværendeMåned(),
                        tom = LocalDate.now().minusMonths(5).sisteDagIMåned(),
                        verdi =
                            AndelTilkjentYtelseGenerator.AndelMedEndretUtbetalingForTidslinje(
                                aktør = barn.aktør,
                                beløp = 0,
                                sats = 1054,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                prosent = BigDecimal.ZERO,
                                endretUtbetalingAndel =
                                    EndretUtbetalingAndelMedAndelerTilkjentYtelse(
                                        andeler = emptyList(),
                                        endretUtbetalingAndel = endretUtbetalingAndel,
                                    ),
                            ),
                    ),
                    Periode(
                        fom = LocalDate.now().minusMonths(4).førsteDagIInneværendeMåned(),
                        tom = LocalDate.now().sisteDagIMåned(),
                        verdi =
                            AndelTilkjentYtelseGenerator.AndelMedEndretUtbetalingForTidslinje(
                                aktør = barn.aktør,
                                beløp = 0,
                                sats = 1766,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                prosent = BigDecimal.ZERO,
                                endretUtbetalingAndel =
                                    EndretUtbetalingAndelMedAndelerTilkjentYtelse(
                                        andeler = emptyList(),
                                        endretUtbetalingAndel = endretUtbetalingAndel,
                                    ),
                            ),
                    ),
                )

            // Act
            val perioderEtterSammenslåing =
                andeler.tilTidslinje().slåSammenEtterfølgende0krAndelerPgaSammeEndretAndel().tilPerioderIkkeNull()

            // Assert
            assertThat(perioderEtterSammenslåing.size).isEqualTo(1)
        }
    }

    @Nested
    inner class FraTidslinjeTilAndelerTest {
        @Test
        fun `skal lage AndelTilkjentYtelseMedEndreteUtbetalinger uten endring hvis perioden ikke er knyttet til en endret utbetaling`() {
            // Arrange
            val barn = lagPerson(type = PersonType.BARN)
            val fom = LocalDate.now().minusMonths(9).førsteDagIInneværendeMåned()
            val tom = LocalDate.now().minusMonths(5).sisteDagIMåned()
            val periode = Periode(
                fom = fom,
                tom = tom,
                verdi =
                    AndelTilkjentYtelseGenerator.AndelMedEndretUtbetalingForTidslinje(
                        aktør = barn.aktør,
                        beløp = 1054,
                        sats = 1054,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        prosent = BigDecimal(100),
                        endretUtbetalingAndel = null,
                    )
            )
            val tilkjentYtelse = TilkjentYtelse(
                behandling = lagBehandling(),
                endretDato = LocalDate.now(),
                opprettetDato = LocalDate.now().minusMonths(9)
            )

            // Act
            val andel = periode.tilAndelTilkjentYtelseMedEndreteUtbetalinger(tilkjentYtelse)

            // Assert
            assertThat(andel.endreteUtbetalinger.size).isEqualTo(0)
            assertThat(andel.andel.tilkjentYtelse).isEqualTo(tilkjentYtelse)
            assertThat(andel.andel.aktør).isEqualTo(barn.aktør)
            assertThat(andel.andel.type).isEqualTo(YtelseType.ORDINÆR_BARNETRYGD)
            assertThat(andel.andel.kalkulertUtbetalingsbeløp).isEqualTo(1054)
            assertThat(andel.andel.nasjonaltPeriodebeløp).isEqualTo(1054)
            assertThat(andel.andel.differanseberegnetPeriodebeløp).isEqualTo(null)
            assertThat(andel.andel.sats).isEqualTo(1054)
            assertThat(andel.andel.prosent).isEqualTo(BigDecimal(100))
            assertThat(andel.andel.stønadFom).isEqualTo(fom.toYearMonth())
            assertThat(andel.andel.stønadTom).isEqualTo(tom.toYearMonth())
        }

        @Test
        fun `skal lage AndelTilkjentYtelseMedEndreteUtbetalinger med endring hvis perioden er knyttet til en endret utbetaling`() {
            // Arrange
            val barn = lagPerson(type = PersonType.BARN)
            val fom = LocalDate.now().minusMonths(9).førsteDagIInneværendeMåned()
            val tom = LocalDate.now().minusMonths(5).sisteDagIMåned()
            val periode = Periode(
                fom = fom,
                tom = tom,
                verdi =
                    AndelTilkjentYtelseGenerator.AndelMedEndretUtbetalingForTidslinje(
                        aktør = barn.aktør,
                        beløp = 0,
                        sats = 1054,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        prosent = BigDecimal.ZERO,
                        endretUtbetalingAndel =
                            EndretUtbetalingAndelMedAndelerTilkjentYtelse(
                                andeler = emptyList(),
                                endretUtbetalingAndel =
                                    lagEndretUtbetalingAndel(
                                        person = barn,
                                        prosent = BigDecimal.ZERO,
                                        årsak = Årsak.ETTERBETALING_3MND,
                                    ),
                            ),
                    )
            )
            val tilkjentYtelse = TilkjentYtelse(
                behandling = lagBehandling(),
                endretDato = LocalDate.now(),
                opprettetDato = LocalDate.now().minusMonths(9)
            )

            // Act
            val andel = periode.tilAndelTilkjentYtelseMedEndreteUtbetalinger(tilkjentYtelse)

            // Assert
            assertThat(andel.endreteUtbetalinger.size).isEqualTo(1)
            assertThat(andel.endreteUtbetalinger.single().prosent).isEqualTo(BigDecimal.ZERO)
            assertThat(andel.endreteUtbetalinger.single().årsak).isEqualTo(Årsak.ETTERBETALING_3MND)
            assertThat(andel.endreteUtbetalinger.single().person).isEqualTo(barn)

            assertThat(andel.andel.tilkjentYtelse).isEqualTo(tilkjentYtelse)
            assertThat(andel.andel.aktør).isEqualTo(barn.aktør)
            assertThat(andel.andel.type).isEqualTo(YtelseType.ORDINÆR_BARNETRYGD)
            assertThat(andel.andel.kalkulertUtbetalingsbeløp).isEqualTo(0)
            assertThat(andel.andel.nasjonaltPeriodebeløp).isEqualTo(0)
            assertThat(andel.andel.differanseberegnetPeriodebeløp).isEqualTo(null)
            assertThat(andel.andel.sats).isEqualTo(1054)
            assertThat(andel.andel.prosent).isEqualTo(BigDecimal.ZERO)
            assertThat(andel.andel.stønadFom).isEqualTo(fom.toYearMonth())
            assertThat(andel.andel.stønadTom).isEqualTo(tom.toYearMonth())
        }

        @Test
        fun `skal lage andeler kun for perioder med verdi`() {
            // Arrange
            val barn = lagPerson(type = PersonType.BARN)
            val perioder =
                listOf(
                    Periode(
                        fom = LocalDate.now().minusMonths(9).førsteDagIInneværendeMåned(),
                        tom = LocalDate.now().minusMonths(5).sisteDagIMåned(),
                        verdi =
                            AndelTilkjentYtelseGenerator.AndelMedEndretUtbetalingForTidslinje(
                                aktør = barn.aktør,
                                beløp = 0,
                                sats = 1054,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                prosent = BigDecimal.ZERO,
                                endretUtbetalingAndel =
                                    EndretUtbetalingAndelMedAndelerTilkjentYtelse(
                                        andeler = emptyList(),
                                        endretUtbetalingAndel =
                                            lagEndretUtbetalingAndel(
                                                person = barn,
                                                prosent = BigDecimal.ZERO,
                                                årsak = Årsak.ETTERBETALING_3MND,
                                            ),
                                    ),
                            ),
                    ),
                    Periode(
                        fom = LocalDate.now().minusMonths(2).førsteDagIInneværendeMåned(),
                        tom = LocalDate.now().sisteDagIMåned(),
                        verdi =
                            AndelTilkjentYtelseGenerator.AndelMedEndretUtbetalingForTidslinje(
                                aktør = barn.aktør,
                                beløp = 1054,
                                sats = 1054,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                prosent = BigDecimal(100),
                                endretUtbetalingAndel = null,
                            ),
                    ),
                )

            val tilkjentYtelse = TilkjentYtelse(
                behandling = lagBehandling(),
                endretDato = LocalDate.now(),
                opprettetDato = LocalDate.now().minusMonths(9)
            )

            // Act
            val tidslinje = perioder.tilTidslinje()
            // Dobbeltsjekker at det blir laget en null-periode mellom de to periodene med verdi
            assertThat(tidslinje.tilPerioder().size).isEqualTo(3)

            val andeler = tidslinje.tilAndelerTilkjentYtelseMedEndreteUtbetalinger(tilkjentYtelse)

            // Assert
            assertThat(andeler.size).isEqualTo(2)
            assertThat(andeler[0].kalkulertUtbetalingsbeløp).isEqualTo(0)
            assertThat(andeler[1].kalkulertUtbetalingsbeløp).isEqualTo(1054)
        }
    }
}
