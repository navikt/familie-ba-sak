package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE
import java.time.LocalDate
import java.time.YearMonth

internal class BehandlingsresultatValideringUtilsTest {
    @Test
    fun `Valider eksplisitt avlag - Skal kaste feil hvis eksplisitt avslått for barn det ikke er fremstilt krav for`() {
        // Arrange
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)
        val barn1 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(5))
        val barn2 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(7))

        val barn1PersonResultat =
            lagPersonResultat(
                vilkårsvurdering = vikårsvurdering,
                person = barn1,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = LocalDate.now().minusMonths(5),
                periodeTom = LocalDate.now(),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
                erEksplisittAvslagPåSøknad = true,
            )
        val barn2PersonResultat =
            lagPersonResultat(
                vilkårsvurdering = vikårsvurdering,
                person = barn2,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = LocalDate.now().minusMonths(5),
                periodeTom = LocalDate.now(),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
                erEksplisittAvslagPåSøknad = true,
            )

        // Act & Assert
        assertThrows<FunksjonellFeil> {
            BehandlingsresultatValideringUtils.validerAtBarePersonerFremstiltKravForEllerSøkerHarFåttEksplisittAvslag(
                personResultater = setOf(barn1PersonResultat, barn2PersonResultat),
                personerFremstiltKravFor = listOf(barn2.aktør),
            )
        }
    }

    @Test
    fun `Valider eksplisitt avslag - Skal ikke kaste feil hvis søker er eksplisitt avslått`() {
        // Arrange
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)
        val søker = lagPerson(type = PersonType.SØKER)

        val søkerPersonResultat =
            lagPersonResultat(
                vilkårsvurdering = vikårsvurdering,
                person = søker,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = LocalDate.now().minusMonths(5),
                periodeTom = LocalDate.now(),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.SØKER,
                erEksplisittAvslagPåSøknad = true,
            )

        // Act & Assert
        assertDoesNotThrow {
            BehandlingsresultatValideringUtils.validerAtBarePersonerFremstiltKravForEllerSøkerHarFåttEksplisittAvslag(
                personResultater = setOf(søkerPersonResultat),
                personerFremstiltKravFor = emptyList(),
            )
        }
    }

    @Test
    fun `Valider eksplisitt avslag - Skal ikke kaste feil hvis person med eksplsitt avslag er fremstilt krav for`() {
        // Arrange
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)
        val barn1 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(5))
        val barn2 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(7))

        val barn1PersonResultat =
            lagPersonResultat(
                vilkårsvurdering = vikårsvurdering,
                person = barn1,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = LocalDate.now().minusMonths(5),
                periodeTom = LocalDate.now(),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
                erEksplisittAvslagPåSøknad = true,
            )
        val barn2PersonResultat =
            lagPersonResultat(
                vilkårsvurdering = vikårsvurdering,
                person = barn2,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.now().minusMonths(5),
                periodeTom = LocalDate.now(),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
                erEksplisittAvslagPåSøknad = false,
            )

        // Act & Assert
        assertDoesNotThrow {
            BehandlingsresultatValideringUtils.validerAtBarePersonerFremstiltKravForEllerSøkerHarFåttEksplisittAvslag(
                personResultater = setOf(barn1PersonResultat, barn2PersonResultat),
                personerFremstiltKravFor = listOf(barn1.aktør, barn2.aktør),
            )
        }
    }

    @Test
    fun `validerIngenEndringTilbakeITid - Skal kaste feil ved endring tilbake i tid`() {
        // Arrange
        val originalAndel =
            lagAndelTilkjentYtelse(
                fom = YearMonth.now().minusMonths(12),
                tom = YearMonth.now().minusMonths(6),
                kalkulertUtbetalingsbeløp = 2000,
            )
        val andelMedLavereBeløp = originalAndel.copy(kalkulertUtbetalingsbeløp = 1000)
        val andelSomAvsluttesTidligere = originalAndel.copy(stønadTom = originalAndel.stønadTom.minusMonths(3))

        val andelerForrigeBehandling = listOf(originalAndel)
        val andelerDenneBehandlingenLavereBeløp = listOf(andelMedLavereBeløp)
        val andelerDenneBehandlingenAvsluttesTidlig = listOf(andelSomAvsluttesTidligere)

        // Act & Assert
        assertThatExceptionOfType(Feil::class.java).isThrownBy {
            BehandlingsresultatValideringUtils.validerIngenEndringTilbakeITid(
                andelerDenneBehandlingen = andelerDenneBehandlingenAvsluttesTidlig,
                andelerForrigeBehandling = andelerForrigeBehandling,
                nåMåned = YearMonth.now(),
            )
        }

        assertThatExceptionOfType(Feil::class.java).isThrownBy {
            BehandlingsresultatValideringUtils.validerIngenEndringTilbakeITid(
                andelerDenneBehandlingen = andelerDenneBehandlingenLavereBeløp,
                andelerForrigeBehandling = andelerForrigeBehandling,
                nåMåned = YearMonth.now(),
            )
        }
    }

    @Test
    fun `validerIngenEndringTilbakeITid - Skal ikke kaste feil ved endring framover i tid`() {
        // Arrange
        val originalAndel =
            lagAndelTilkjentYtelse(
                fom = YearMonth.now().minusMonths(12),
                tom = YearMonth.now().plusMonths(6),
                kalkulertUtbetalingsbeløp = 2000,
            )
        val andelSomAvsluttesTidligere = originalAndel.copy(stønadTom = originalAndel.stønadTom.minusMonths(3))
        val andelMedLavereBeløpIFramtiden = originalAndel.copy(stønadFom = andelSomAvsluttesTidligere.stønadTom.plusMonths(1), kalkulertUtbetalingsbeløp = 1000)

        val andelerForrigeBehandling = listOf(originalAndel)
        val andelerDenneBehandlingenLavereBeløpIFramtiden = listOf(andelSomAvsluttesTidligere, andelMedLavereBeløpIFramtiden)
        val andelerDenneBehandlingenSomAvsluttesTidligere = listOf(andelSomAvsluttesTidligere)

        // Act & Assert
        assertDoesNotThrow {
            BehandlingsresultatValideringUtils.validerIngenEndringTilbakeITid(
                andelerDenneBehandlingen = andelerDenneBehandlingenSomAvsluttesTidligere,
                andelerForrigeBehandling = andelerForrigeBehandling,
                nåMåned = YearMonth.now(),
            )
        }

        assertDoesNotThrow {
            BehandlingsresultatValideringUtils.validerIngenEndringTilbakeITid(
                andelerDenneBehandlingen = andelerDenneBehandlingenLavereBeløpIFramtiden,
                andelerForrigeBehandling = andelerForrigeBehandling,
                nåMåned = YearMonth.now(),
            )
        }
    }

    @Test
    fun `validerSatsErUendret - Skal kaste feil dersom sats endrer seg`() {
        // Arrange
        val originalAndel =
            lagAndelTilkjentYtelse(
                fom = YearMonth.now().minusMonths(12),
                tom = YearMonth.now().plusMonths(6),
                sats = 2000,
            )

        val andelMedAnnenSats =
            lagAndelTilkjentYtelse(
                fom = YearMonth.now().minusMonths(12),
                tom = YearMonth.now().plusMonths(6),
                sats = 2000,
            )

        // Act & Assert
        assertThatExceptionOfType(Feil::class.java).isThrownBy {
            BehandlingsresultatValideringUtils.validerSatsErUendret(
                andelerForrigeBehandling = listOf(originalAndel),
                andelerDenneBehandlingen = listOf(andelMedAnnenSats),
            )
        }

        assertDoesNotThrow {
            BehandlingsresultatValideringUtils.validerSatsErUendret(
                andelerForrigeBehandling = listOf(originalAndel),
                andelerDenneBehandlingen = listOf(originalAndel),
            )
        }
    }

    @Test
    fun `validerSatsErUendret - Skal ikke kaste feil dersom sats endrer seg hvis kalkulertUtbetalingsbeløp er 0`() {
        // Arrange
        val person = tilfeldigPerson()
        val originalAndel =
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2019, 1),
                tom = YearMonth.of(2019, 4),
                sats = 970,
                kalkulertUtbetalingsbeløp = 0,
                person = person,
            )

        val nyeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2019, 1),
                    tom = YearMonth.of(2019, 2),
                    sats = 970,
                    kalkulertUtbetalingsbeløp = 0,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2019, 3),
                    tom = YearMonth.of(2019, 4),
                    sats = 1054,
                    kalkulertUtbetalingsbeløp = 0,
                    person = person,
                ),
            )

        // Act & Assert
        assertDoesNotThrow {
            BehandlingsresultatValideringUtils.validerSatsErUendret(
                andelerForrigeBehandling = listOf(originalAndel),
                andelerDenneBehandlingen = nyeAndeler,
            )
        }
    }

    @ParameterizedTest
    @EnumSource(
        value = Behandlingsresultat::class,
        names = ["AVSLÅTT", "AVSLÅTT_OG_OPPHØRT", "AVSLÅTT_OG_ENDRET", "AVSLÅTT_ENDRET_OG_OPPHØRT", "DELVIS_INNVILGET"],
        mode = INCLUDE,
    )
    fun `Skal kaste exception hvis behandlingsresultat er ugyldig for en manuell migrering`(
        behandlingsresultat: Behandlingsresultat,
    ) {
        // Arrange
        val behandling =
            lagBehandling(
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
                resultat = behandlingsresultat,
            )

        // Act & Assert
        val feil =
            assertThrows<FunksjonellFeil> {
                BehandlingsresultatValideringUtils.validerBehandlingsresultat(behandling)
            }

        assertThat(feil.message)
            .isEqualTo(
                "Du har fått behandlingsresultatet ${behandlingsresultat.displayName}. " +
                    "Dette er ikke støttet på migreringsbehandlinger. " +
                    "Meld sak i Porten om du er uenig i resultatet.",
            )
    }

    @ParameterizedTest
    @EnumSource(
        value = Behandlingsresultat::class,
        names = ["ENDRET_UTBETALING", "ENDRET_UTEN_UTBETALING", "ENDRET_OG_OPPHØRT", "OPPHØRT"],
        mode = INCLUDE,
    )
    fun `skal kaste feil hvis behandlingsresultat ved omregning er endret`(
        behandlingsresultat: Behandlingsresultat,
    ) {
        // Arrange
        val behandling =
            lagBehandling(
                behandlingType = BehandlingType.REVURDERING,
                årsak = BehandlingÅrsak.OMREGNING_18ÅR,
                resultat = behandlingsresultat,
            )

        // Act & Assert
        assertThrows<Feil> {
            BehandlingsresultatValideringUtils.validerBehandlingsresultat(behandling)
        }
    }

    @ParameterizedTest
    @EnumSource(
        value = Behandlingsresultat::class,
        names = ["FORTSATT_INNVILGET", "FORTSATT_OPPHØRT"],
        mode = INCLUDE,
    )
    fun `skal ikke kaste feil hvis behandlingsresultat ved omregning er uendret`(
        behandlingsresultat: Behandlingsresultat,
    ) {
        // Arrange
        val behandling =
            lagBehandling(
                behandlingType = BehandlingType.REVURDERING,
                årsak = BehandlingÅrsak.OMREGNING_18ÅR,
                resultat = behandlingsresultat,
            )

        // Act & Assert
        assertDoesNotThrow {
            BehandlingsresultatValideringUtils.validerBehandlingsresultat(behandling)
        }
    }

    @ParameterizedTest
    @EnumSource(
        value = Behandlingsresultat::class,
        names = ["OPPHØRT", "ENDRET_OG_OPPHØRT", "IKKE_VURDERT"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `skal kaste feil dersom behandlingsresultat i 'Falsk identitet'-behandling er noe annet enn OPPHØRT`(behandlingsresultat: Behandlingsresultat) {
        // Arrange
        val behandling =
            lagBehandling(
                behandlingType = BehandlingType.REVURDERING,
                årsak = BehandlingÅrsak.FALSK_IDENTITET,
                resultat = behandlingsresultat,
            )

        // Act & Assert
        val funksjonellFeil =
            assertThrows<FunksjonellFeil> {
                BehandlingsresultatValideringUtils.validerBehandlingsresultat(behandling)
            }
        assertThat(funksjonellFeil.message).isEqualTo("Du har fått behandlingsresultatet ${behandlingsresultat.displayName}. 'Falsk identitet'-behandlinger kan kun ha behandlingsresultat: '${OPPHØRT.displayName}'")
    }

    @ParameterizedTest
    @EnumSource(
        value = Behandlingsresultat::class,
        names = ["OPPHØRT", "ENDRET_OG_OPPHØRT"],
        mode = INCLUDE,
    )
    fun `skal ikke kaste feil dersom behandlingsresultat i 'Falsk identitet'-behandling er OPPHØRT`(behandlingsresultat: Behandlingsresultat) {
        // Arrange
        val behandling =
            lagBehandling(
                behandlingType = BehandlingType.REVURDERING,
                årsak = BehandlingÅrsak.FALSK_IDENTITET,
                resultat = behandlingsresultat,
            )

        // Act & Assert
        assertDoesNotThrow { BehandlingsresultatValideringUtils.validerBehandlingsresultat(behandling) }
    }
}
