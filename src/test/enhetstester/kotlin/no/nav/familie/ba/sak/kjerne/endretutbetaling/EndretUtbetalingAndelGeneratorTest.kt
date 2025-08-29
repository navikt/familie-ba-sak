package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class EndretUtbetalingAndelGeneratorTest {
    private val behandling = lagBehandling()
    private val forrigeBehandling = lagBehandling()
    private val søker = lagPerson()
    private val barn = lagPerson(type = PersonType.BARN)
    private val fomAndelTilkjentYtelse = YearMonth.of(2021, 1)
    private val tomAndelTilkjentYtelse = YearMonth.of(2024, 12)

    @Test
    fun `Skal preutfylle endret utbetaling andeler med årsak ETTERBETALING_3ÅR når søknad er mottatt før 2024-10-01`() {
        // Arrange
        val søknadMottattDato = LocalDate.of(2024, 9, 30)

        val nåværendeAndeler = lagAndelTilkjentYtelserForSøkerOgBarn(behandling = behandling, beløp = 2000)
        val forrigeAndeler = lagAndelTilkjentYtelserForSøkerOgBarn(behandling = forrigeBehandling, beløp = 1000)

        // Act
        val endretUtbetalingAndeler =
            genererEndretUtbetalingAndelerMedÅrsakEtterbetaling3ÅrEller3Mnd(
                behandling = behandling,
                søknadMottattDato = søknadMottattDato,
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                personerPåBehandling = listOf(søker, barn),
                nåværendeEndretUtbetalingAndeler = emptyList(),
            )

        // Assert
        val endretUtbetalingAndel = endretUtbetalingAndeler.single()
        assertEndretUtbetalingAndel(endretUtbetalingAndel, søknadMottattDato)
    }

    @Test
    fun `Skal preutfylle endret utbetaling andeler med årsak ETTERBETALING_3MND når søknad er mottatt etter 2024-10-01`() {
        // Arrange
        val søknadMottattDato = LocalDate.of(2024, 10, 1)

        val nåværendeAndeler = lagAndelTilkjentYtelserForSøkerOgBarn(behandling = behandling, beløp = 2000)
        val forrigeAndeler = lagAndelTilkjentYtelserForSøkerOgBarn(behandling = forrigeBehandling, beløp = 1000)

        // Act
        val endretUtbetalingAndeler =
            genererEndretUtbetalingAndelerMedÅrsakEtterbetaling3ÅrEller3Mnd(
                behandling = behandling,
                søknadMottattDato = søknadMottattDato,
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                personerPåBehandling = listOf(søker, barn),
                nåværendeEndretUtbetalingAndeler = emptyList(),
            )

        // Assert
        val endretUtbetalingAndel = endretUtbetalingAndeler.single()
        assertEndretUtbetalingAndel(endretUtbetalingAndel, søknadMottattDato)
    }

    @Test
    fun `Skal preutfylle endret utbetaling andeler med årsak ETTERBETALING_3MND for flere barn`() {
        // Arrange
        val søknadMottattDato = LocalDate.of(2024, 10, 1)

        val barn2 = lagPerson()

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = fomAndelTilkjentYtelse,
                    tom = tomAndelTilkjentYtelse,
                    aktør = søker.aktør,
                    behandling = behandling,
                    kalkulertUtbetalingsbeløp = 2000,
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ),
                lagAndelTilkjentYtelse(
                    fom = fomAndelTilkjentYtelse,
                    tom = tomAndelTilkjentYtelse,
                    aktør = barn.aktør,
                    behandling = behandling,
                    kalkulertUtbetalingsbeløp = 2000,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                ),
                lagAndelTilkjentYtelse(
                    fom = fomAndelTilkjentYtelse.plusMonths(4),
                    tom = tomAndelTilkjentYtelse,
                    aktør = barn2.aktør,
                    behandling = behandling,
                    kalkulertUtbetalingsbeløp = 2000,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                ),
            )
        val forrigeAndeler =
            nåværendeAndeler.map {
                it.copy(
                    behandlingId = forrigeBehandling.id,
                    kalkulertUtbetalingsbeløp = 1000,
                )
            }

        // Act
        val endretUtbetalingAndeler =
            genererEndretUtbetalingAndelerMedÅrsakEtterbetaling3ÅrEller3Mnd(
                behandling = behandling,
                søknadMottattDato = søknadMottattDato,
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                personerPåBehandling = listOf(søker, barn, barn2),
                nåværendeEndretUtbetalingAndeler = emptyList(),
            )

        // Assert
        assertThat(endretUtbetalingAndeler).hasSize(2)

        assertEndretUtbetalingAndel(
            endretUtbetalingAndel = endretUtbetalingAndeler[0],
            forventetSøknadMottattDato = søknadMottattDato,
            forventedePersoner = setOf(søker, barn),
            forventetFom = fomAndelTilkjentYtelse,
            forventetTom = fomAndelTilkjentYtelse.plusMonths(3),
        )

        assertEndretUtbetalingAndel(
            endretUtbetalingAndel = endretUtbetalingAndeler[1],
            forventetSøknadMottattDato = søknadMottattDato,
            forventedePersoner = setOf(søker, barn, barn2),
            forventetFom = fomAndelTilkjentYtelse.plusMonths(4),
        )
    }

    @Test
    fun `Skal bare preutfylle endret utbetaling andeler med årsak ETTERBETALING_3MND for barn som ikke allerede har eksisterende endret utbetaling i periode`() {
        // Arrange
        val søknadMottattDato = LocalDate.of(2024, 10, 1)

        val barn2 = lagPerson(type = PersonType.BARN)

        val nåværendeEndretUtbetalingAndeler =
            lagEndretUtbetalingAndel(
                fom = fomAndelTilkjentYtelse.plusMonths(4),
                tom = tomAndelTilkjentYtelse,
                personer = setOf(barn),
            )

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = fomAndelTilkjentYtelse,
                    tom = tomAndelTilkjentYtelse,
                    aktør = søker.aktør,
                    behandling = behandling,
                    kalkulertUtbetalingsbeløp = 2000,
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ),
                lagAndelTilkjentYtelse(
                    fom = fomAndelTilkjentYtelse,
                    tom = tomAndelTilkjentYtelse,
                    aktør = barn.aktør,
                    behandling = behandling,
                    kalkulertUtbetalingsbeløp = 2000,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                ),
                lagAndelTilkjentYtelse(
                    fom = fomAndelTilkjentYtelse.plusMonths(4),
                    tom = tomAndelTilkjentYtelse,
                    aktør = barn2.aktør,
                    behandling = behandling,
                    kalkulertUtbetalingsbeløp = 2000,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                ),
            )
        val forrigeAndeler =
            nåværendeAndeler.map {
                it.copy(
                    behandlingId = forrigeBehandling.id,
                    kalkulertUtbetalingsbeløp = 1000,
                )
            }

        // Act
        val endretUtbetalingAndeler =
            genererEndretUtbetalingAndelerMedÅrsakEtterbetaling3ÅrEller3Mnd(
                behandling = behandling,
                søknadMottattDato = søknadMottattDato,
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                personerPåBehandling = listOf(søker, barn, barn2),
                nåværendeEndretUtbetalingAndeler = listOf(nåværendeEndretUtbetalingAndeler),
            )

        // Assert
        assertThat(endretUtbetalingAndeler).hasSize(2)

        assertEndretUtbetalingAndel(
            endretUtbetalingAndel = endretUtbetalingAndeler[0],
            forventetSøknadMottattDato = søknadMottattDato,
            forventedePersoner = setOf(søker),
            forventetFom = fomAndelTilkjentYtelse,
            forventetTom = fomAndelTilkjentYtelse.plusMonths(3),
        )

        assertEndretUtbetalingAndel(
            endretUtbetalingAndel = endretUtbetalingAndeler[1],
            forventetSøknadMottattDato = søknadMottattDato,
            forventedePersoner = setOf(søker, barn2),
            forventetFom = fomAndelTilkjentYtelse.plusMonths(4),
        )
    }

    @Test
    fun `Skal ikke preutfylle endret utbetaling andeler når det ikke finnes perioder med ugyldig etterbetaling`() {
        // Arrange
        val søknadMottattDato = LocalDate.of(2024, 10, 1)

        val nåværendeAndeler = lagAndelTilkjentYtelserForSøkerOgBarn(behandling = behandling, beløp = 1000)
        val forrigeAndeler = lagAndelTilkjentYtelserForSøkerOgBarn(behandling = forrigeBehandling, beløp = 1000)

        // Act
        val endretUtbetalingAndeler =
            genererEndretUtbetalingAndelerMedÅrsakEtterbetaling3ÅrEller3Mnd(
                behandling = behandling,
                søknadMottattDato = søknadMottattDato,
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                personerPåBehandling = listOf(søker, barn),
                nåværendeEndretUtbetalingAndeler = emptyList(),
            )

        // Assert
        assertThat(endretUtbetalingAndeler).isEmpty()
    }

    @Test
    fun `Skal håndtere tilfelle der det ikke finnes forrige behandling`() {
        // Arrange
        val søknadMottattDato = LocalDate.of(2024, 10, 1)

        val nåværendeAndeler = lagAndelTilkjentYtelserForSøkerOgBarn(behandling = behandling, beløp = 2000)

        // Act
        val endretUtbetalingAndeler =
            genererEndretUtbetalingAndelerMedÅrsakEtterbetaling3ÅrEller3Mnd(
                behandling = behandling,
                søknadMottattDato = søknadMottattDato,
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = emptyList(),
                personerPåBehandling = listOf(søker, barn),
                nåværendeEndretUtbetalingAndeler = emptyList(),
            )

        // Assert
        val endretUtbetalingAndel = endretUtbetalingAndeler.single()
        assertEndretUtbetalingAndel(endretUtbetalingAndel, søknadMottattDato)
    }

    private fun lagAndelTilkjentYtelserForSøkerOgBarn(
        behandling: Behandling,
        beløp: Int,
    ) = listOf(
        lagAndelTilkjentYtelse(
            fom = fomAndelTilkjentYtelse,
            tom = tomAndelTilkjentYtelse,
            aktør = søker.aktør,
            behandling = behandling,
            kalkulertUtbetalingsbeløp = beløp,
            ytelseType = YtelseType.UTVIDET_BARNETRYGD,
        ),
        lagAndelTilkjentYtelse(
            fom = fomAndelTilkjentYtelse,
            tom = tomAndelTilkjentYtelse,
            aktør = barn.aktør,
            behandling = behandling,
            kalkulertUtbetalingsbeløp = beløp,
            ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
        ),
    )

    private fun assertEndretUtbetalingAndel(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        forventetSøknadMottattDato: LocalDate,
        forventedePersoner: Set<Person> = setOf(søker, barn),
        forventetFom: YearMonth = fomAndelTilkjentYtelse,
        forventetTom: YearMonth =
            if (forventetSøknadMottattDato.isBefore(LocalDate.of(2024, 10, 1))) {
                forventetSøknadMottattDato.minusYears(3).minusMonths(1)
            } else {
                forventetSøknadMottattDato.minusMonths(4)
            }.toYearMonth(),
    ) {
        assertThat(endretUtbetalingAndel.behandlingId).isEqualTo(behandling.id)
        assertThat(endretUtbetalingAndel.personer).isEqualTo(forventedePersoner)
        assertThat(endretUtbetalingAndel.prosent).isEqualTo(BigDecimal.ZERO)
        assertThat(endretUtbetalingAndel.søknadstidspunkt).isEqualTo(forventetSøknadMottattDato)
        assertThat(endretUtbetalingAndel.fom).isEqualTo(forventetFom)
        assertThat(endretUtbetalingAndel.tom).isEqualTo(forventetTom)
        assertThat(endretUtbetalingAndel.årsak).isEqualTo(
            if (forventetSøknadMottattDato.isBefore(LocalDate.of(2024, 10, 1))) {
                Årsak.ETTERBETALING_3ÅR
            } else {
                Årsak.ETTERBETALING_3MND
            },
        )
        assertThat(endretUtbetalingAndel.avtaletidspunktDeltBosted).isNull()
        assertThat(endretUtbetalingAndel.begrunnelse).isEqualTo("Fylt ut automatisk fra søknadstidspunkt.")
    }
}
