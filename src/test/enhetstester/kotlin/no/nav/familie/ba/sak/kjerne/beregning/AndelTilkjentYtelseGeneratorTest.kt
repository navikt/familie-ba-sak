package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseGenerator.oppdaterAndelerMedEndretUtbetalingAndeler
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseGenerator.slåSammenEtterfølgende0krAndelerPgaSammeEndretAndel
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
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

            val perioderEtterSammenslåing =
                andeler.tilTidslinje().slåSammenEtterfølgende0krAndelerPgaSammeEndretAndel().tilPerioderIkkeNull()

            assertThat(perioderEtterSammenslåing.size).isEqualTo(2)
        }

        @Test
        fun `skal ikke slå sammen 0kr-andeler som har tom periode mellom seg`() {
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

            val perioderEtterSammenslåing =
                andeler.tilTidslinje().slåSammenEtterfølgende0krAndelerPgaSammeEndretAndel().tilPerioderIkkeNull()

            assertThat(perioderEtterSammenslåing.size).isEqualTo(2)
        }

        @Test
        fun `skal ikke slå sammen etterfølgende andeler med 100 prosent utbetaling av ulik sats`() {
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

            val perioderEtterSammenslåing =
                andeler.tilTidslinje().slåSammenEtterfølgende0krAndelerPgaSammeEndretAndel().tilPerioderIkkeNull()

            assertThat(perioderEtterSammenslåing.size).isEqualTo(2)
        }

        @Test
        fun `skal slå sammen etterfølgende 0kr-andeler som skyldes samme endret andel, men er splittet pga satsendring`() {
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

            val perioderEtterSammenslåing =
                andeler.tilTidslinje().slåSammenEtterfølgende0krAndelerPgaSammeEndretAndel().tilPerioderIkkeNull()

            assertThat(perioderEtterSammenslåing.size).isEqualTo(1)
        }
    }
}
