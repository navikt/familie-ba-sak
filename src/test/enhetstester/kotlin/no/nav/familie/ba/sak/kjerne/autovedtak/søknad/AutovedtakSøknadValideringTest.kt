package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.lagØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.datagenerator.lagØkonomiSimuleringPostering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.YearMonth

class AutovedtakSøknadValideringTest {
    private val fagsak = lagFagsak()
    private val behandling = lagBehandling(fagsak = fagsak)
    private val forrigeBehandling = lagBehandling(fagsak = fagsak)
    private val søker = fagsak.aktør
    private val barnFremstiltKravFor = lagPerson(type = PersonType.BARN).aktør
    private val barnUtenKrav = lagPerson(type = PersonType.BARN).aktør

    @Nested
    inner class ValiderAtVilkårsvurderingErOppfylt {
        @Test
        fun `skal ikke kaste feil når alle vilkår er oppfylt`() {
            // Arrange
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            // Act & Assert
            assertDoesNotThrow { AutovedtakSøknadValidering.validerAtVilkårsvurderingErOppfylt(vilkårsvurdering) }
        }

        @Test
        fun `skal kaste feil når et vilkår ikke er oppfylt`() {
            // Arrange
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = behandling,
                    lagPersonResultater = {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                aktør = søker,
                                lagVilkårResultater = { personResultat ->
                                    setOf(lagVilkårResultat(personResultat = personResultat, resultat = Resultat.IKKE_OPPFYLT))
                                },
                            ),
                        )
                    },
                )

            // Act
            val feil = assertThrows<AutovedtakMåBehandlesManueltFeil> { AutovedtakSøknadValidering.validerAtVilkårsvurderingErOppfylt(vilkårsvurdering) }

            // Assert
            assertThat(feil.message).isEqualTo("Vilkårsvurderingen er ikke oppfylt.\nBehandling av søknad må håndteres manuelt.")
        }
    }

    @Nested
    inner class ValiderAtBehandlingsresultatErInnvilgetEllerDelvisInnvilget {
        @ParameterizedTest
        @EnumSource(value = Behandlingsresultat::class, names = ["INNVILGET", "DELVIS_INNVILGET"])
        fun `skal ikke kaste feil når behandlingsresultatet er gyldig`(behandlingsresultat: Behandlingsresultat) {
            // Act & Assert
            assertDoesNotThrow { AutovedtakSøknadValidering.validerAtBehandlingsresultatErInnvilgetEllerDelvisInnvilget(behandlingsresultat) }
        }

        @ParameterizedTest
        @EnumSource(value = Behandlingsresultat::class, names = ["INNVILGET", "DELVIS_INNVILGET"], mode = EnumSource.Mode.EXCLUDE)
        fun `skal kaste feil når behandlingsresultatet er ugyldig`(behandlingsresultat: Behandlingsresultat) {
            // Act
            val feil = assertThrows<AutovedtakMåBehandlesManueltFeil> { AutovedtakSøknadValidering.validerAtBehandlingsresultatErInnvilgetEllerDelvisInnvilget(behandlingsresultat) }

            // Assert
            assertThat(feil.message).isEqualTo(
                "Automatisk behandling av søknad gir behandlingsresultatet '${behandlingsresultat.displayName}'.\nBehandling av søknad må håndteres manuelt.",
            )
        }
    }

    @Nested
    inner class ValiderAtKunPersonerFremstiltKravForHarEndringIAndeler {
        private fun lagAndel(
            aktør: Aktør,
            behandling: Behandling = this@AutovedtakSøknadValideringTest.behandling,
            beløp: Int = 1054,
            prosent: BigDecimal = BigDecimal(100),
            ytelseType: YtelseType = YtelseType.ORDINÆR_BARNETRYGD,
            fom: YearMonth = YearMonth.of(2025, 1),
            tom: YearMonth = YearMonth.of(2030, 12),
        ) = lagAndelTilkjentYtelse(
            fom = fom,
            tom = tom,
            aktør = aktør,
            behandling = behandling,
            beløp = beløp,
            prosent = prosent,
            ytelseType = ytelseType,
        )

        private fun valider(
            andelerDenneBehandlingen: List<AndelTilkjentYtelse>,
            andelerForrigeBehandling: List<AndelTilkjentYtelse> = emptyList(),
            personerFremstiltKravFor: Set<Aktør> = setOf(barnFremstiltKravFor),
        ) = AutovedtakSøknadValidering.validerAtKunPersonerFremstiltKravForHarEndringIAndeler(
            behandlingId = behandling.id,
            andelerDenneBehandlingen = andelerDenneBehandlingen,
            andelerForrigeBehandling = andelerForrigeBehandling,
            personerFremstiltKravFor = personerFremstiltKravFor,
        )

        @Test
        fun `skal ikke kaste feil når bare barn med krav får nye andeler`() {
            // Arrange
            val andelerDenneBehandlingen = listOf(lagAndel(barnFremstiltKravFor))

            // Act & Assert
            assertDoesNotThrow { valider(andelerDenneBehandlingen) }
        }

        @Test
        fun `skal kaste feil når barn uten krav får ny andel`() {
            // Arrange
            val andelerDenneBehandlingen = listOf(lagAndel(barnFremstiltKravFor), lagAndel(barnUtenKrav))

            // Act
            val feil = assertThrows<AutovedtakMåBehandlesManueltFeil> { valider(andelerDenneBehandlingen) }

            // Assert
            assertThat(feil.message).isEqualTo(
                "Automatisk behandling av søknad gir endring i andeler for 1 person(er) det ikke er fremstilt krav for.\nBehandling av søknad må håndteres manuelt.",
            )
        }

        @Test
        fun `skal telle alle personer uten krav i feilmeldingen`() {
            // Arrange
            val annetBarnUtenKrav = lagPerson(type = PersonType.BARN).aktør
            val andelerDenneBehandlingen = listOf(lagAndel(barnFremstiltKravFor), lagAndel(barnUtenKrav), lagAndel(annetBarnUtenKrav))

            // Act
            val feil = assertThrows<AutovedtakMåBehandlesManueltFeil> { valider(andelerDenneBehandlingen) }

            // Assert
            assertThat(feil.message).isEqualTo(
                "Automatisk behandling av søknad gir endring i andeler for 2 person(er) det ikke er fremstilt krav for.\nBehandling av søknad må håndteres manuelt.",
            )
        }

        @Test
        fun `skal kaste feil når barn uten krav får ny andel med 0 i beløp`() {
            // Arrange
            val andelerDenneBehandlingen = listOf(lagAndel(barnFremstiltKravFor), lagAndel(barnUtenKrav, beløp = 0))

            // Act & Assert
            assertThrows<AutovedtakMåBehandlesManueltFeil> { valider(andelerDenneBehandlingen) }
        }

        @Test
        fun `skal kaste feil når barn uten krav mister andel`() {
            // Arrange
            val andelerDenneBehandlingen = listOf(lagAndel(barnFremstiltKravFor))
            val andelerForrigeBehandling = listOf(lagAndel(barnUtenKrav, behandling = forrigeBehandling))

            // Act & Assert
            assertThrows<AutovedtakMåBehandlesManueltFeil> { valider(andelerDenneBehandlingen, andelerForrigeBehandling) }
        }

        @Test
        fun `skal ikke kaste feil når andelene til barn uten krav er uendret`() {
            // Arrange
            val andelerDenneBehandlingen = listOf(lagAndel(barnFremstiltKravFor), lagAndel(barnUtenKrav))
            val andelerForrigeBehandling = listOf(lagAndel(barnUtenKrav, behandling = forrigeBehandling))

            // Act & Assert
            assertDoesNotThrow { valider(andelerDenneBehandlingen, andelerForrigeBehandling) }
        }

        @Test
        fun `skal kaste feil når andelen til barn uten krav forkortes`() {
            // Arrange
            val andelerDenneBehandlingen = listOf(lagAndel(barnFremstiltKravFor), lagAndel(barnUtenKrav, tom = YearMonth.of(2027, 12)))
            val andelerForrigeBehandling = listOf(lagAndel(barnUtenKrav, behandling = forrigeBehandling))

            // Act & Assert
            assertThrows<AutovedtakMåBehandlesManueltFeil> { valider(andelerDenneBehandlingen, andelerForrigeBehandling) }
        }

        @Test
        fun `skal kaste feil når andelen til barn uten krav starter tidligere`() {
            // Arrange
            val andelerDenneBehandlingen = listOf(lagAndel(barnFremstiltKravFor), lagAndel(barnUtenKrav, fom = YearMonth.of(2024, 1)))
            val andelerForrigeBehandling = listOf(lagAndel(barnUtenKrav, behandling = forrigeBehandling))

            // Act & Assert
            assertThrows<AutovedtakMåBehandlesManueltFeil> { valider(andelerDenneBehandlingen, andelerForrigeBehandling) }
        }

        @Test
        fun `skal ikke kaste feil når bare barn med krav har endret periode`() {
            // Arrange
            val andelerDenneBehandlingen =
                listOf(lagAndel(barnFremstiltKravFor, tom = YearMonth.of(2032, 12)), lagAndel(barnUtenKrav))
            val andelerForrigeBehandling =
                listOf(lagAndel(barnFremstiltKravFor, behandling = forrigeBehandling), lagAndel(barnUtenKrav, behandling = forrigeBehandling))

            // Act & Assert
            assertDoesNotThrow { valider(andelerDenneBehandlingen, andelerForrigeBehandling) }
        }

        @Test
        fun `skal ikke kaste feil når bare barn med krav har endret beløp`() {
            // Arrange
            val andelerDenneBehandlingen = listOf(lagAndel(barnFremstiltKravFor, beløp = 2000), lagAndel(barnUtenKrav))
            val andelerForrigeBehandling =
                listOf(lagAndel(barnFremstiltKravFor, behandling = forrigeBehandling), lagAndel(barnUtenKrav, behandling = forrigeBehandling))

            // Act & Assert
            assertDoesNotThrow { valider(andelerDenneBehandlingen, andelerForrigeBehandling) }
        }

        @Test
        fun `skal ikke kaste feil når prosent bare har ulik skala`() {
            // Arrange
            val andelerDenneBehandlingen = listOf(lagAndel(barnFremstiltKravFor), lagAndel(barnUtenKrav, prosent = BigDecimal("100.00")))
            val andelerForrigeBehandling = listOf(lagAndel(barnUtenKrav, behandling = forrigeBehandling, prosent = BigDecimal(100)))

            // Act & Assert
            assertDoesNotThrow { valider(andelerDenneBehandlingen, andelerForrigeBehandling) }
        }

        @ParameterizedTest
        @EnumSource(EndretFelt::class)
        fun `skal kaste feil når ett felt er endret for barn uten krav`(endretFelt: EndretFelt) {
            // Arrange
            val forrigeAndel = lagAndel(barnUtenKrav, behandling = forrigeBehandling)
            val nyAndel =
                when (endretFelt) {
                    EndretFelt.SATS -> forrigeAndel.copy(sats = 2000)
                    EndretFelt.PROSENT -> forrigeAndel.copy(prosent = BigDecimal(50))
                    EndretFelt.KALKULERT_UTBETALINGSBELØP -> forrigeAndel.copy(kalkulertUtbetalingsbeløp = 2000)
                    EndretFelt.NASJONALT_PERIODEBELØP -> forrigeAndel.copy(nasjonaltPeriodebeløp = 2000)
                    EndretFelt.DIFFERANSEBEREGNET_PERIODEBELØP -> forrigeAndel.copy(differanseberegnetPeriodebeløp = 500)
                    EndretFelt.BELØP_UTEN_ENDRET_UTBETALING -> forrigeAndel.copy(beløpUtenEndretUtbetaling = 2000)
                }

            // Act & Assert
            assertThrows<AutovedtakMåBehandlesManueltFeil> { valider(listOf(nyAndel), listOf(forrigeAndel)) }
        }

        @Test
        fun `skal kaste feil når barn uten krav får ny ytelsetype`() {
            // Arrange
            val andelerDenneBehandlingen = listOf(lagAndel(barnUtenKrav), lagAndel(barnUtenKrav, ytelseType = YtelseType.FINNMARKSTILLEGG))
            val andelerForrigeBehandling = listOf(lagAndel(barnUtenKrav, behandling = forrigeBehandling))

            // Act & Assert
            assertThrows<AutovedtakMåBehandlesManueltFeil> { valider(andelerDenneBehandlingen, andelerForrigeBehandling) }
        }

        @Test
        fun `skal kaste feil når søker får utvidet uten krav`() {
            // Arrange
            val andelerDenneBehandlingen = listOf(lagAndel(barnFremstiltKravFor), lagAndel(søker, ytelseType = YtelseType.UTVIDET_BARNETRYGD))

            // Act & Assert
            assertThrows<AutovedtakMåBehandlesManueltFeil> { valider(andelerDenneBehandlingen) }
        }

        @Test
        fun `skal ikke kaste feil når søker får utvidet med krav`() {
            // Arrange
            val andelerDenneBehandlingen = listOf(lagAndel(barnFremstiltKravFor), lagAndel(søker, ytelseType = YtelseType.UTVIDET_BARNETRYGD))

            // Act & Assert
            assertDoesNotThrow { valider(andelerDenneBehandlingen, personerFremstiltKravFor = setOf(barnFremstiltKravFor, søker)) }
        }
    }

    @Nested
    inner class ValiderAtInnvilgedePerioderIkkeOverlapperTidligereUtbetalingsperioder {
        private fun lagAndel(
            aktør: Aktør,
            fom: YearMonth,
            tom: YearMonth,
            behandling: Behandling = this@AutovedtakSøknadValideringTest.behandling,
            beløp: Int = 1054,
            ytelseType: YtelseType = YtelseType.ORDINÆR_BARNETRYGD,
        ) = lagAndelTilkjentYtelse(
            fom = fom,
            tom = tom,
            aktør = aktør,
            behandling = behandling,
            beløp = beløp,
            ytelseType = ytelseType,
        )

        private fun valider(
            andelerDenneBehandlingen: List<AndelTilkjentYtelse>,
            andelerFraTidligereIverksatteBehandlinger: List<AndelTilkjentYtelse> = emptyList(),
        ) = AutovedtakSøknadValidering.validerAtInnvilgedePerioderIkkeOverlapperTidligereUtbetalingsperioder(
            behandlingId = behandling.id,
            andelerDenneBehandlingen = andelerDenneBehandlingen,
            andelerFraTidligereIverksatteBehandlinger = andelerFraTidligereIverksatteBehandlinger,
        )

        @Test
        fun `skal ikke kaste feil når søker ikke har tidligere utbetalingsperioder`() {
            // Arrange
            val andelerDenneBehandlingen = listOf(lagAndel(barnFremstiltKravFor, fom = YearMonth.of(2025, 1), tom = YearMonth.of(2030, 12)))

            // Act & Assert
            assertDoesNotThrow { valider(andelerDenneBehandlingen) }
        }

        @Test
        fun `skal ikke kaste feil når innvilgede perioder ikke overlapper med tidligere utbetalingsperioder`() {
            // Arrange
            val andelerDenneBehandlingen = listOf(lagAndel(barnFremstiltKravFor, fom = YearMonth.of(2025, 1), tom = YearMonth.of(2030, 12)))
            val andelerFraTidligereIverksatteBehandlinger =
                listOf(lagAndel(barnUtenKrav, behandling = forrigeBehandling, fom = YearMonth.of(2020, 1), tom = YearMonth.of(2024, 12)))

            // Act & Assert
            assertDoesNotThrow { valider(andelerDenneBehandlingen, andelerFraTidligereIverksatteBehandlinger) }
        }

        @Test
        fun `skal kaste feil når en innvilget periode overlapper med en tidligere utbetalingsperiode for en annen person`() {
            // Arrange
            val andelerDenneBehandlingen = listOf(lagAndel(barnFremstiltKravFor, fom = YearMonth.of(2025, 1), tom = YearMonth.of(2030, 12)))
            val andelerFraTidligereIverksatteBehandlinger =
                listOf(lagAndel(barnUtenKrav, behandling = forrigeBehandling, fom = YearMonth.of(2020, 1), tom = YearMonth.of(2025, 1)))

            // Act & Assert
            val feil = assertThrows<AutovedtakMåBehandlesManueltFeil> { valider(andelerDenneBehandlingen, andelerFraTidligereIverksatteBehandlinger) }
            assertThat(feil.message).isEqualTo("Automatisk behandling av søknad innvilger 1 periode(r) som overlapper med tidligere utbetalingsperioder til søker.\nBehandling av søknad må håndteres manuelt.")
        }

        @Test
        fun `skal kaste feil når en innvilget periode overlapper med en tidligere utvidet utbetalingsperiode til søker`() {
            // Arrange
            val andelerDenneBehandlingen = listOf(lagAndel(barnFremstiltKravFor, fom = YearMonth.of(2025, 1), tom = YearMonth.of(2030, 12)))
            val andelerFraTidligereIverksatteBehandlinger =
                listOf(
                    lagAndel(
                        søker,
                        behandling = forrigeBehandling,
                        fom = YearMonth.of(2026, 1),
                        tom = YearMonth.of(2026, 6),
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                    ),
                )

            // Act & Assert
            assertThrows<AutovedtakMåBehandlesManueltFeil> { valider(andelerDenneBehandlingen, andelerFraTidligereIverksatteBehandlinger) }
        }

        @Test
        fun `skal telle overlappende periode kun én gang selv om flere barn har samme periode`() {
            // Arrange
            val andelerDenneBehandlingen =
                listOf(
                    lagAndel(barnFremstiltKravFor, fom = YearMonth.of(2025, 1), tom = YearMonth.of(2030, 12)),
                    lagAndel(barnUtenKrav, fom = YearMonth.of(2025, 1), tom = YearMonth.of(2030, 12)),
                )
            val andelerFraTidligereIverksatteBehandlinger =
                listOf(lagAndel(barnUtenKrav, behandling = forrigeBehandling, fom = YearMonth.of(2020, 1), tom = YearMonth.of(2025, 1)))

            // Act & Assert
            val feil = assertThrows<AutovedtakMåBehandlesManueltFeil> { valider(andelerDenneBehandlingen, andelerFraTidligereIverksatteBehandlinger) }
            assertThat(feil.message).isEqualTo("Automatisk behandling av søknad innvilger 1 periode(r) som overlapper med tidligere utbetalingsperioder til søker.\nBehandling av søknad må håndteres manuelt.")
        }

        @Test
        fun `skal telle hver overlappende periode når flere innvilgede perioder overlapper`() {
            // Arrange
            val andelerDenneBehandlingen =
                listOf(
                    lagAndel(barnFremstiltKravFor, fom = YearMonth.of(2025, 1), tom = YearMonth.of(2025, 6)),
                    lagAndel(barnFremstiltKravFor, fom = YearMonth.of(2026, 1), tom = YearMonth.of(2026, 6)),
                )
            val andelerFraTidligereIverksatteBehandlinger =
                listOf(lagAndel(barnUtenKrav, behandling = forrigeBehandling, fom = YearMonth.of(2020, 1), tom = YearMonth.of(2030, 12)))

            // Act & Assert
            val feil = assertThrows<AutovedtakMåBehandlesManueltFeil> { valider(andelerDenneBehandlingen, andelerFraTidligereIverksatteBehandlinger) }
            assertThat(feil.message).isEqualTo("Automatisk behandling av søknad innvilger 2 periode(r) som overlapper med tidligere utbetalingsperioder til søker.\nBehandling av søknad må håndteres manuelt.")
        }

        @Test
        fun `skal ikke kaste feil når den overlappende tidligere andelen ikke ga utbetaling`() {
            // Arrange
            val andelerDenneBehandlingen = listOf(lagAndel(barnFremstiltKravFor, fom = YearMonth.of(2025, 1), tom = YearMonth.of(2030, 12)))
            val andelerFraTidligereIverksatteBehandlinger =
                listOf(lagAndel(barnUtenKrav, behandling = forrigeBehandling, fom = YearMonth.of(2025, 1), tom = YearMonth.of(2025, 12), beløp = 0))

            // Act & Assert
            assertDoesNotThrow { valider(andelerDenneBehandlingen, andelerFraTidligereIverksatteBehandlinger) }
        }

        @Test
        fun `skal ikke kaste feil når den overlappende andelen i denne behandlingen ikke gir utbetaling`() {
            // Arrange
            val andelerDenneBehandlingen = listOf(lagAndel(barnFremstiltKravFor, fom = YearMonth.of(2025, 1), tom = YearMonth.of(2030, 12), beløp = 0))
            val andelerFraTidligereIverksatteBehandlinger =
                listOf(lagAndel(barnUtenKrav, behandling = forrigeBehandling, fom = YearMonth.of(2025, 1), tom = YearMonth.of(2025, 12)))

            // Act & Assert
            assertDoesNotThrow { valider(andelerDenneBehandlingen, andelerFraTidligereIverksatteBehandlinger) }
        }
    }

    @Nested
    inner class ValiderAtSimuleringGirUtbetaling {
        @Test
        fun `skal ikke kaste feil når simuleringen gir utbetaling`() {
            // Arrange
            val simulering = listOf(lagØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = listOf(lagØkonomiSimuleringPostering(beløp = 100))))

            // Act & Assert
            assertDoesNotThrow { AutovedtakSøknadValidering.validerAtSimuleringGirUtbetaling(simulering) }
        }

        @Test
        fun `skal kaste feil når simuleringen ikke gir utbetaling`() {
            // Arrange
            val simulering = listOf(lagØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = listOf(lagØkonomiSimuleringPostering(beløp = 0))))

            // Act
            val feil = assertThrows<AutovedtakMåBehandlesManueltFeil> { AutovedtakSøknadValidering.validerAtSimuleringGirUtbetaling(simulering) }

            // Assert
            assertThat(feil.message).isEqualTo("Automatisk behandling av søknad fører til ingen utbetaling.\nBehandling av søknad må håndteres manuelt.")
        }

        @Test
        fun `skal kaste feil når simuleringen er tom`() {
            // Act
            val feil = assertThrows<AutovedtakMåBehandlesManueltFeil> { AutovedtakSøknadValidering.validerAtSimuleringGirUtbetaling(emptyList()) }

            // Assert
            assertThat(feil.message).isEqualTo("Automatisk behandling av søknad fører til ingen utbetaling.\nBehandling av søknad må håndteres manuelt.")
        }

        @Test
        fun `skal kaste feil når simuleringen ikke gir utbetaling og beløpet har desimaler`() {
            // Arrange
            val postering = lagØkonomiSimuleringPostering(beløp = 0).copy(beløp = BigDecimal("0.00"))
            val simulering = listOf(lagØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = listOf(postering)))

            // Act
            val feil = assertThrows<AutovedtakMåBehandlesManueltFeil> { AutovedtakSøknadValidering.validerAtSimuleringGirUtbetaling(simulering) }

            // Assert
            assertThat(feil.message).isEqualTo("Automatisk behandling av søknad fører til ingen utbetaling.\nBehandling av søknad må håndteres manuelt.")
        }
    }

    @Nested
    inner class ValiderAtDetIkkeErFeilutbetaling {
        @Test
        fun `skal ikke kaste feil når det ikke er feilutbetaling`() {
            // Arrange
            val simulering = listOf(lagØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = listOf(lagØkonomiSimuleringPostering(beløp = 100))))

            // Act & Assert
            assertDoesNotThrow { AutovedtakSøknadValidering.validerAtDetIkkeErFeilutbetaling(simulering) }
        }

        @Test
        fun `skal kaste feil når det er feilutbetaling`() {
            // Arrange
            val feilutbetaling = lagØkonomiSimuleringPostering(beløp = 100, posteringType = PosteringType.FEILUTBETALING)
            val simulering = listOf(lagØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = listOf(feilutbetaling)))

            // Act
            val feil = assertThrows<AutovedtakMåBehandlesManueltFeil> { AutovedtakSøknadValidering.validerAtDetIkkeErFeilutbetaling(simulering) }

            // Assert
            assertThat(feil.message).isEqualTo("Automatisk behandling av søknad fører til feilutbetaling.\nBehandling av søknad må håndteres manuelt.")
        }
    }

    enum class EndretFelt {
        SATS,
        PROSENT,
        KALKULERT_UTBETALINGSBELØP,
        NASJONALT_PERIODEBELØP,
        DIFFERANSEBEREGNET_PERIODEBELØP,
        BELØP_UTEN_ENDRET_UTBETALING,
    }
}
