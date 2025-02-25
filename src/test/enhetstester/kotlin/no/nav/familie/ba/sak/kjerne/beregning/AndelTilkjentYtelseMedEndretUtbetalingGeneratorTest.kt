package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseMedEndretUtbetalingGenerator.lagAndelerMedEndretUtbetalingAndeler
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseMedEndretUtbetalingGenerator.oppdaterAndelerForPersonMedEndretUtbetalingAndeler
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseMedEndretUtbetalingGenerator.slåSammenEtterfølgende0krAndelerPgaSammeEndretAndel
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseMedEndretUtbetalingGenerator.tilAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseMedEndretUtbetalingGenerator.tilAndelerTilkjentYtelseMedEndreteUtbetalinger
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
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class AndelTilkjentYtelseMedEndretUtbetalingGeneratorTest {
    @Nested
    inner class OppdaterAndelerMedEndringer {
        @Test
        fun `skal returnere tom liste hvis det ikke finnes noen andeler`() {
            // Arrange
            val tilkjentYtelse =
                TilkjentYtelse(
                    behandling = lagBehandling(),
                    endretDato = LocalDate.now(),
                    opprettetDato = LocalDate.now().minusMonths(9),
                )

            // Act
            val oppdaterteAndeler =
                lagAndelerMedEndretUtbetalingAndeler(
                    andelTilkjentYtelserUtenEndringer = emptyList(),
                    endretUtbetalingAndeler = emptyList(),
                    tilkjentYtelse = tilkjentYtelse,
                )

            // Assert
            assertThat(oppdaterteAndeler).isEmpty()
        }

        @Test
        fun `skal returnere eksisterende andeler uten endringer hvis det ikke finnes noen endret utbetalinger`() {
            // Arrange
            val barn1 = lagPerson(type = PersonType.BARN)
            val barn2 = lagPerson(type = PersonType.BARN)
            val behandling = lagBehandling()

            val tilkjentYtelse =
                TilkjentYtelse(
                    behandling = behandling,
                    endretDato = LocalDate.now(),
                    opprettetDato = LocalDate.now().minusMonths(9),
                )

            val andel1 =
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().minusMonths(5),
                    person = barn1,
                    behandling = behandling,
                    beløp = 1000,
                    sats = 1000,
                )

            val andel2 =
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusMonths(4),
                    tom = YearMonth.now(),
                    person = barn2,
                    behandling = behandling,
                    beløp = 1500,
                    sats = 1500,
                )

            // Act
            val oppdaterteAndeler =
                lagAndelerMedEndretUtbetalingAndeler(
                    andelTilkjentYtelserUtenEndringer = listOf(andel1, andel2),
                    endretUtbetalingAndeler = emptyList(),
                    tilkjentYtelse = tilkjentYtelse,
                )

            // Assert
            assertThat(oppdaterteAndeler.size).isEqualTo(2)

            val førsteAndelMedEndring = oppdaterteAndeler.minBy { it.stønadFom }
            assertThat(førsteAndelMedEndring.andel).isEqualTo(andel1)
            assertThat(førsteAndelMedEndring.endreteUtbetalinger).isEmpty()

            val sisteAndelMedEndring = oppdaterteAndeler.maxBy { it.stønadFom }
            assertThat(sisteAndelMedEndring.andel).isEqualTo(andel2)
            assertThat(sisteAndelMedEndring.endreteUtbetalinger).isEmpty()
        }

        @Test
        fun `skal oppdatere andeler for riktig person med endret utbetaling`() {
            // Arrange
            val barn1 = lagPerson(type = PersonType.BARN)
            val barn2 = lagPerson(type = PersonType.BARN)
            val behandling = lagBehandling()

            val tilkjentYtelse =
                TilkjentYtelse(
                    behandling = behandling,
                    endretDato = LocalDate.now(),
                    opprettetDato = LocalDate.now().minusMonths(9),
                )

            val fom = YearMonth.now().minusMonths(10)
            val tom = YearMonth.now().minusMonths(5)

            val andel1 =
                lagAndelTilkjentYtelse(
                    fom = fom,
                    tom = tom,
                    person = barn1,
                    behandling = behandling,
                    beløp = 1000,
                    sats = 1000,
                )

            val andel2 =
                lagAndelTilkjentYtelse(
                    fom = fom,
                    tom = tom,
                    person = barn2,
                    behandling = behandling,
                    beløp = 1500,
                    sats = 1500,
                )

            val endretUtbetalingAndelForBarn1 =
                lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                    behandlingId = behandling.id,
                    person = barn1,
                    prosent = BigDecimal.ZERO,
                    årsak = Årsak.ALLEREDE_UTBETALT,
                    fom = fom,
                    tom = tom,
                )

            // Act
            val oppdaterteAndeler =
                lagAndelerMedEndretUtbetalingAndeler(
                    andelTilkjentYtelserUtenEndringer = listOf(andel1, andel2),
                    endretUtbetalingAndeler = listOf(endretUtbetalingAndelForBarn1),
                    tilkjentYtelse = tilkjentYtelse,
                )

            // Assert
            assertThat(oppdaterteAndeler.size).isEqualTo(2)

            assertThat(oppdaterteAndeler[0].kalkulertUtbetalingsbeløp).isEqualTo(0)
            assertThat(oppdaterteAndeler[0].prosent).isEqualTo(BigDecimal.ZERO)
            assertThat(oppdaterteAndeler[0].endreteUtbetalinger.size).isEqualTo(1)
            assertThat(oppdaterteAndeler[0].endreteUtbetalinger.single()).isEqualTo(endretUtbetalingAndelForBarn1.endretUtbetalingAndel)

            assertThat(oppdaterteAndeler[1].andel).isEqualTo(andel2)
            assertThat(oppdaterteAndeler[1].endreteUtbetalinger).isEmpty()
        }

        @Test
        fun `skal overstyre utvidet andeler, men ikke småbarnstillegg ved endret utbetaling på søker`() {
            // Arrange
            val barn1 = lagPerson(type = PersonType.BARN)
            val søker = lagPerson(type = PersonType.SØKER)
            val behandling = lagBehandling()

            val tilkjentYtelse =
                TilkjentYtelse(
                    behandling = behandling,
                    endretDato = LocalDate.now(),
                    opprettetDato = LocalDate.now().minusMonths(9),
                )

            val fom = YearMonth.now().minusMonths(10)
            val tom = YearMonth.now().minusMonths(5)

            val andelBarn =
                lagAndelTilkjentYtelse(
                    fom = fom,
                    tom = tom,
                    person = barn1,
                    behandling = behandling,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                )

            val andelUtvidet =
                lagAndelTilkjentYtelse(
                    fom = fom,
                    tom = tom,
                    person = søker,
                    behandling = behandling,
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                )

            val andelSmåbarnstillegg =
                lagAndelTilkjentYtelse(
                    fom = fom,
                    tom = tom,
                    person = søker,
                    behandling = behandling,
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                )

            val endretUtbetalingAndelForSøker =
                lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                    behandlingId = behandling.id,
                    person = søker,
                    prosent = BigDecimal.ZERO,
                    årsak = Årsak.ETTERBETALING_3MND,
                    fom = fom,
                    tom = tom,
                )

            // Act
            val oppdaterteAndeler =
                lagAndelerMedEndretUtbetalingAndeler(
                    andelTilkjentYtelserUtenEndringer = listOf(andelBarn, andelUtvidet, andelSmåbarnstillegg),
                    endretUtbetalingAndeler = listOf(endretUtbetalingAndelForSøker),
                    tilkjentYtelse = tilkjentYtelse,
                )

            // Assert
            assertThat(oppdaterteAndeler.size).isEqualTo(3)

            assertThat(oppdaterteAndeler.map { it.andel }).contains(andelBarn, andelSmåbarnstillegg)
            assertThat(
                oppdaterteAndeler.any {
                    it.kalkulertUtbetalingsbeløp == 0 &&
                        it.prosent == BigDecimal.ZERO &&
                        it.endreteUtbetalinger.size == 1 &&
                        it.endreteUtbetalinger.single() == endretUtbetalingAndelForSøker.endretUtbetalingAndel
                },
            ).isTrue()
        }

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

            val andelerTilkjentYtelse =
                lagAndelerMedEndretUtbetalingAndeler(
                    utbetalingsandeler,
                    endretUtbetalingAndeler,
                    utbetalingsandeler.first().tilkjentYtelse,
                )

            assertThat(andelerTilkjentYtelse.size).isEqualTo(1)
            assertThat(andelerTilkjentYtelse.single().prosent).isEqualTo(endretProsent)
            assertThat(andelerTilkjentYtelse.single().endreteUtbetalinger.size).isEqualTo(1)
        }

        @Test
        fun `endret utbetalingsandel kobler endrede andeler til riktig endret utbetalingandel`() {
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

            val andelerTilkjentYtelse =
                lagAndelerMedEndretUtbetalingAndeler(
                    utbetalingsandeler,
                    endretUtbetalingAndeler,
                    utbetalingsandeler.first().tilkjentYtelse,
                )

            assertThat(andelerTilkjentYtelse.size).isEqualTo(2)
            andelerTilkjentYtelse.forEach { assertThat(it.prosent).isEqualTo(endretProsent) }
            andelerTilkjentYtelse.forEach { assertThat(it.endreteUtbetalinger.size).isEqualTo(1) }
            andelerTilkjentYtelse.forEach {
                assertThat(
                    it.endreteUtbetalinger.single().id,
                ).isEqualTo(endretUtbetalingAndel.id)
            }
        }
    }

    @Nested
    inner class OppdaterAndelerMedEndringerForPerson {
        @Test
        fun `skal returnere tom liste hvis person ikke har noen andeler`() {
            // Arrange
            val tilkjentYtelse =
                TilkjentYtelse(
                    behandling = lagBehandling(),
                    endretDato = LocalDate.now(),
                    opprettetDato = LocalDate.now().minusMonths(9),
                )

            // Act
            val oppdaterteAndeler =
                oppdaterAndelerForPersonMedEndretUtbetalingAndeler(
                    andelerForPerson = emptyList(),
                    endretUtbetalingAndelerForPerson = emptyList(),
                    tilkjentYtelse = tilkjentYtelse,
                )

            // Assert
            assertThat(oppdaterteAndeler).isEmpty()
        }

        @Test
        fun `skal returnere eksisterende andeler uten endringer hvis det ikke er noen endringer for person`() {
            // Arrange
            val behandling = lagBehandling()
            val barn = lagPerson(type = PersonType.BARN)
            val tilkjentYtelse =
                TilkjentYtelse(
                    behandling = behandling,
                    endretDato = LocalDate.now(),
                    opprettetDato = LocalDate.now().minusMonths(9),
                )

            val andel1 =
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().minusMonths(5),
                    person = barn,
                    behandling = behandling,
                    beløp = 1000,
                    sats = 1000,
                )

            val andel2 =
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusMonths(4),
                    tom = YearMonth.now(),
                    person = barn,
                    behandling = behandling,
                    beløp = 1500,
                    sats = 1500,
                )

            // Act
            val oppdaterteAndeler =
                oppdaterAndelerForPersonMedEndretUtbetalingAndeler(
                    andelerForPerson = listOf(andel1, andel2),
                    endretUtbetalingAndelerForPerson = emptyList(),
                    tilkjentYtelse = tilkjentYtelse,
                )

            // Assert
            assertThat(oppdaterteAndeler.size).isEqualTo(2)

            val førsteAndelMedEndring = oppdaterteAndeler.minBy { it.stønadFom }
            assertThat(førsteAndelMedEndring.andel).isEqualTo(andel1)
            assertThat(førsteAndelMedEndring.endreteUtbetalinger).isEmpty()

            val sisteAndelMedEndring = oppdaterteAndeler.maxBy { it.stønadFom }
            assertThat(sisteAndelMedEndring.andel).isEqualTo(andel2)
            assertThat(sisteAndelMedEndring.endreteUtbetalinger).isEmpty()
        }

        @Test
        fun `skal oppdatere andeler med endring som går på tvers av andeler`() {
            // Arrange
            val behandling = lagBehandling()
            val barn = lagPerson(type = PersonType.BARN)
            val tilkjentYtelse =
                TilkjentYtelse(
                    behandling = behandling,
                    endretDato = LocalDate.now(),
                    opprettetDato = LocalDate.now().minusMonths(9),
                )

            val andel1 =
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().minusMonths(5),
                    person = barn,
                    behandling = behandling,
                    beløp = 1000,
                    sats = 1000,
                )

            val andel2 =
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusMonths(4),
                    tom = YearMonth.now(),
                    person = barn,
                    behandling = behandling,
                    beløp = 1500,
                    sats = 1500,
                )

            val endretUtbetalingAndel =
                lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                    behandlingId = behandling.id,
                    person = barn,
                    prosent = BigDecimal.ZERO,
                    årsak = Årsak.ALLEREDE_UTBETALT,
                    fom = YearMonth.now().minusMonths(7),
                    tom = YearMonth.now().minusMonths(2),
                )

            // Act
            val oppdaterteAndeler =
                oppdaterAndelerForPersonMedEndretUtbetalingAndeler(
                    andelerForPerson = listOf(andel1, andel2),
                    endretUtbetalingAndelerForPerson = listOf(endretUtbetalingAndel),
                    tilkjentYtelse = tilkjentYtelse,
                )

            // Assert
            assertThat(oppdaterteAndeler.size).isEqualTo(3)

            assertThat(oppdaterteAndeler[0].kalkulertUtbetalingsbeløp).isEqualTo(andel1.kalkulertUtbetalingsbeløp)
            assertThat(oppdaterteAndeler[0].prosent).isEqualTo(andel1.prosent)
            assertThat(oppdaterteAndeler[0].stønadFom).isEqualTo(andel1.stønadFom)
            assertThat(oppdaterteAndeler[0].stønadTom).isEqualTo(endretUtbetalingAndel.fom?.minusMonths(1))
            assertThat(oppdaterteAndeler[0].endreteUtbetalinger).isEmpty()

            assertThat(oppdaterteAndeler[1].kalkulertUtbetalingsbeløp).isEqualTo(0)
            assertThat(oppdaterteAndeler[1].prosent).isEqualTo(endretUtbetalingAndel.prosent)
            assertThat(oppdaterteAndeler[1].stønadFom).isEqualTo(endretUtbetalingAndel.fom)
            assertThat(oppdaterteAndeler[1].stønadTom).isEqualTo(endretUtbetalingAndel.tom)
            assertThat(oppdaterteAndeler[1].endreteUtbetalinger.size).isEqualTo(1)
            assertThat(oppdaterteAndeler[1].endreteUtbetalinger.single()).isEqualTo(endretUtbetalingAndel.endretUtbetalingAndel)

            assertThat(oppdaterteAndeler[2].kalkulertUtbetalingsbeløp).isEqualTo(andel2.kalkulertUtbetalingsbeløp)
            assertThat(oppdaterteAndeler[2].prosent).isEqualTo(andel2.prosent)
            assertThat(oppdaterteAndeler[2].stønadFom).isEqualTo(endretUtbetalingAndel.tom?.plusMonths(1))
            assertThat(oppdaterteAndeler[2].stønadTom).isEqualTo(andel2.stønadTom)
            assertThat(oppdaterteAndeler[2].endreteUtbetalinger).isEmpty()
        }

        @Test
        fun `skal kaste feil om man prøver å oppdatere småbarnstillegg-andeler med endringer`() {
            // Arrange
            val behandling = lagBehandling()
            val barn = lagPerson(type = PersonType.BARN)
            val tilkjentYtelse =
                TilkjentYtelse(
                    behandling = behandling,
                    endretDato = LocalDate.now(),
                    opprettetDato = LocalDate.now().minusMonths(9),
                )

            val andel =
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().minusMonths(5),
                    person = barn,
                    behandling = behandling,
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                )

            // Act & Assert
            assertThrows<Feil> {
                oppdaterAndelerForPersonMedEndretUtbetalingAndeler(
                    andelerForPerson = listOf(andel),
                    endretUtbetalingAndelerForPerson = emptyList(),
                    tilkjentYtelse = tilkjentYtelse,
                )
            }
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
                            AndelTilkjentYtelseMedEndretUtbetalingGenerator.AndelMedEndretUtbetalingForTidslinje(
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
                            AndelTilkjentYtelseMedEndretUtbetalingGenerator.AndelMedEndretUtbetalingForTidslinje(
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
                            AndelTilkjentYtelseMedEndretUtbetalingGenerator.AndelMedEndretUtbetalingForTidslinje(
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
                            AndelTilkjentYtelseMedEndretUtbetalingGenerator.AndelMedEndretUtbetalingForTidslinje(
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
                            AndelTilkjentYtelseMedEndretUtbetalingGenerator.AndelMedEndretUtbetalingForTidslinje(
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
                            AndelTilkjentYtelseMedEndretUtbetalingGenerator.AndelMedEndretUtbetalingForTidslinje(
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
                            AndelTilkjentYtelseMedEndretUtbetalingGenerator.AndelMedEndretUtbetalingForTidslinje(
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
                            AndelTilkjentYtelseMedEndretUtbetalingGenerator.AndelMedEndretUtbetalingForTidslinje(
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
            val periode =
                Periode(
                    fom = fom,
                    tom = tom,
                    verdi =
                        AndelTilkjentYtelseMedEndretUtbetalingGenerator.AndelMedEndretUtbetalingForTidslinje(
                            aktør = barn.aktør,
                            beløp = 1054,
                            sats = 1054,
                            ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                            prosent = BigDecimal(100),
                            endretUtbetalingAndel = null,
                        ),
                )
            val tilkjentYtelse =
                TilkjentYtelse(
                    behandling = lagBehandling(),
                    endretDato = LocalDate.now(),
                    opprettetDato = LocalDate.now().minusMonths(9),
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
            val periode =
                Periode(
                    fom = fom,
                    tom = tom,
                    verdi =
                        AndelTilkjentYtelseMedEndretUtbetalingGenerator.AndelMedEndretUtbetalingForTidslinje(
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
                )
            val tilkjentYtelse =
                TilkjentYtelse(
                    behandling = lagBehandling(),
                    endretDato = LocalDate.now(),
                    opprettetDato = LocalDate.now().minusMonths(9),
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
                            AndelTilkjentYtelseMedEndretUtbetalingGenerator.AndelMedEndretUtbetalingForTidslinje(
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
                            AndelTilkjentYtelseMedEndretUtbetalingGenerator.AndelMedEndretUtbetalingForTidslinje(
                                aktør = barn.aktør,
                                beløp = 1054,
                                sats = 1054,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                prosent = BigDecimal(100),
                                endretUtbetalingAndel = null,
                            ),
                    ),
                )

            val tilkjentYtelse =
                TilkjentYtelse(
                    behandling = lagBehandling(),
                    endretDato = LocalDate.now(),
                    opprettetDato = LocalDate.now().minusMonths(9),
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
