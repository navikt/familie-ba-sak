package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.TestClockProvider.Companion.lagClockProviderMedFastTidspunkt
import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.SatsendringFeil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagKompetanse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.datagenerator.lagValutakurs
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType.REVURDERING
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.FINNMARKSTILLEGG
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.SATSENDRING
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.SVALBARDTILLEGG
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.SØKNAD
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Vurderingsform
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIUtbetalingUtil
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.prosessering.error.RekjørSenereException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class BehandlingsresultatStegValideringServiceTest {
    private val beregningService: BeregningService = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository = mockk()
    private val vilkårService: VilkårService = mockk()
    private val kompetanseRepository: KompetanseRepository = mockk()
    private val utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository = mockk()
    private val valutakursRepository: ValutakursRepository = mockk()
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService = mockk()
    private val clockProvider = lagClockProviderMedFastTidspunkt(LocalDate.of(2025, 10, 10))
    private val featureToggleService = mockk<FeatureToggleService>()

    private val behandlingsresultatStegValideringService =
        BehandlingsresultatStegValideringService(
            beregningService = beregningService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            vilkårService = vilkårService,
            kompetanseRepository = kompetanseRepository,
            utenlandskPeriodebeløpRepository = utenlandskPeriodebeløpRepository,
            valutakursRepository = valutakursRepository,
            andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
            clockProvider = clockProvider,
            featureToggleService = featureToggleService,
        )

    private val barn = lagPerson(type = PersonType.BARN)
    private val barn2 = lagPerson(type = PersonType.BARN)
    private val behandling = lagBehandling()

    @Nested
    inner class ValiderAtUtenlandskPeriodebeløpOgValutakursErUtfylt {
        @Test
        fun `skal kaste feil dersom det finnes utenlandskperiodebeløp som ikke er fylt ut`() {
            // Arrange
            every { utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandling.id) } returns listOf(lagUtenlandskPeriodebeløp())
            every { valutakursRepository.finnFraBehandlingId(behandling.id) } returns emptyList()

            // Act & Assert
            val feil = assertThrows<FunksjonellFeil> { behandlingsresultatStegValideringService.validerAtUtenlandskPeriodebeløpOgValutakursErUtfylt(behandling) }

            assertThat(feil.message).isEqualTo(
                "Kan ikke fullføre behandlingsresultat-steg før utbetalt i det " +
                    "andre landet og valutakurs er fylt ut for alle barn og perioder",
            )
        }

        @Test
        fun `skal kaste feil dersom det finnes valutakurser som ikke er fylt ut`() {
            // Arrange
            every { utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandling.id) } returns emptyList()
            every { valutakursRepository.finnFraBehandlingId(behandling.id) } returns listOf(lagValutakurs())

            // Act & Assert
            val feil = assertThrows<FunksjonellFeil> { behandlingsresultatStegValideringService.validerAtUtenlandskPeriodebeløpOgValutakursErUtfylt(behandling) }

            assertThat(feil.message).isEqualTo(
                "Kan ikke fullføre behandlingsresultat-steg før utbetalt i det " +
                    "andre landet og valutakurs er fylt ut for alle barn og perioder",
            )
        }

        @Test
        fun `skal ikke kaste feil dersom utenlandsk periodebeløp og valutakurser er utfylt`() {
            // Arrange
            every { utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandling.id) } returns
                listOf(
                    lagUtenlandskPeriodebeløp(
                        fom = LocalDate.now().toYearMonth(),
                        beløp = BigDecimal.valueOf(100),
                        intervall = Intervall.MÅNEDLIG,
                        valutakode = "SEK",
                        utbetalingsland = "S",
                        barnAktører = setOf(randomAktør()),
                    ),
                )
            every { valutakursRepository.finnFraBehandlingId(behandling.id) } returns
                listOf(
                    lagValutakurs(
                        fom = LocalDate.now().toYearMonth(),
                        valutakode = "SEK",
                        valutakursdato = LocalDate.now(),
                        kurs = BigDecimal.valueOf(1.2),
                        barnAktører = setOf(randomAktør()),
                        vurderingsform = Vurderingsform.MANUELL,
                    ),
                )

            // Act & Assert
            assertDoesNotThrow { behandlingsresultatStegValideringService.validerAtUtenlandskPeriodebeløpOgValutakursErUtfylt(behandling) }
        }
    }

    @Nested
    inner class ValiderKompetanse {
        @Test
        fun `skal kaste feil dersom det finnes kompetanser der Norge er sekundærland men aktivitetsland og bosted er satt til Norge`() {
            // Arrange
            val ugyldigKompetanse =
                lagKompetanse(
                    behandlingId = behandling.id,
                    kompetanseResultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                    søkersAktivitetsland = "NO",
                    barnetsBostedsland = "NO",
                    annenForeldersAktivitetsland = "NO",
                    barnAktører = setOf(barn.aktør),
                )

            every { kompetanseRepository.finnFraBehandlingId(behandling.id) } returns listOf(ugyldigKompetanse)

            // Act & Assert
            val feil = assertThrows<FunksjonellFeil> { behandlingsresultatStegValideringService.validerKompetanse(behandling.id) }

            assertThat(feil.melding).isEqualTo(
                "Dersom Norge er sekundærland, må søkers aktivitetsland, annen forelders " +
                    "aktivitetsland eller barnets bostedsland være satt til noe annet enn Norge",
            )
        }

        @Test
        fun `skal ikke kaste feil dersom det ikke finnes kompetanser der Norge er sekundærland med aktivitetsland og bosted som er satt til Norge`() {
            // Arrange
            val gyldigKompetanse =
                lagKompetanse(
                    behandlingId = behandling.id,
                    kompetanseResultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                    søkersAktivitetsland = "NO",
                    barnetsBostedsland = "SE",
                    annenForeldersAktivitetsland = "NO",
                    barnAktører = setOf(barn.aktør),
                )

            every { kompetanseRepository.finnFraBehandlingId(behandling.id) } returns listOf(gyldigKompetanse)

            // Act & Assert
            assertDoesNotThrow { behandlingsresultatStegValideringService.validerKompetanse(behandling.id) }
        }
    }

    @Nested
    inner class ValiderSatsendring {
        @Test
        fun `skal kaste feil når endringene i andeler er relatert til noe annet enn endring i sats`() {
            // Arrange
            val forrigeBehandling = lagBehandling(behandlingType = FØRSTEGANGSBEHANDLING, årsak = SØKNAD)
            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp =
                            SatsService.finnGjeldendeSatsForDato(
                                SatsType.ORBA,
                                YearMonth.of(2023, 1).førsteDagIInneværendeMåned(),
                            ),
                        aktør = barn.aktør,
                        prosent = BigDecimal(50),
                    ),
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 3),
                        tom = YearMonth.of(2033, 1),
                        behandling = forrigeBehandling,
                        beløp =
                            SatsService.finnGjeldendeSatsForDato(
                                SatsType.ORBA,
                                YearMonth.of(2023, 3).førsteDagIInneværendeMåned(),
                            ),
                        aktør = barn.aktør,
                        prosent = BigDecimal(50),
                    ),
                )

            val behandling = lagBehandling(behandlingType = REVURDERING, årsak = SATSENDRING)
            val tilkjentYtelse =
                lagTilkjentYtelse(behandling = behandling) {
                    setOf(
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp =
                                SatsService.finnGjeldendeSatsForDato(
                                    SatsType.ORBA,
                                    YearMonth.of(2023, 1).førsteDagIInneværendeMåned(),
                                ),
                            aktør = barn.aktør,
                            prosent = BigDecimal(50),
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 3),
                            tom = YearMonth.of(2023, 6),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp =
                                SatsService.finnGjeldendeSatsForDato(
                                    SatsType.ORBA,
                                    YearMonth.of(2023, 3).førsteDagIInneværendeMåned(),
                                ),
                            aktør = barn.aktør,
                            prosent = BigDecimal(100), // Endret prosent som ikke skal være tillatt
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 7),
                            tom = YearMonth.of(2033, 1),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp =
                                SatsService.finnGjeldendeSatsForDato(
                                    SatsType.ORBA,
                                    YearMonth.of(2023, 7).førsteDagIInneværendeMåned(),
                                ),
                            aktør = barn.aktør,
                            prosent = BigDecimal(50),
                        ),
                    )
                }

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler

            // Act & Assert
            val feil = assertThrows<SatsendringFeil> { behandlingsresultatStegValideringService.validerSatsendring(tilkjentYtelse) }

            assertThat(feil.message).contains("Satsendring kan ikke endre på prosenten til en andel")
        }

        @Test
        fun `skal ikke kaste feil når endringene i andeler kun er relatert til endring i sats`() {
            // Arrange
            val forrigeBehandling = lagBehandling(behandlingType = FØRSTEGANGSBEHANDLING, årsak = SØKNAD)
            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp =
                            SatsService.finnGjeldendeSatsForDato(
                                SatsType.ORBA,
                                YearMonth.of(2023, 1).førsteDagIInneværendeMåned(),
                            ),
                        aktør = barn.aktør,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 3),
                        tom = YearMonth.of(2033, 1),
                        behandling = forrigeBehandling,
                        beløp =
                            SatsService.finnGjeldendeSatsForDato(
                                SatsType.ORBA,
                                YearMonth.of(2023, 3).førsteDagIInneværendeMåned(),
                            ),
                        aktør = barn.aktør,
                    ),
                )

            val behandling = lagBehandling(behandlingType = REVURDERING, årsak = SATSENDRING)
            val tilkjentYtelse =
                lagTilkjentYtelse(behandling = behandling) {
                    setOf(
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp =
                                SatsService.finnGjeldendeSatsForDato(
                                    SatsType.ORBA,
                                    YearMonth.of(2023, 1).førsteDagIInneværendeMåned(),
                                ),
                            aktør = barn.aktør,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 3),
                            tom = YearMonth.of(2023, 6),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp =
                                SatsService.finnGjeldendeSatsForDato(
                                    SatsType.ORBA,
                                    YearMonth.of(2023, 3).førsteDagIInneværendeMåned(),
                                ),
                            aktør = barn.aktør,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 7),
                            tom = YearMonth.of(2033, 1),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp =
                                SatsService.finnGjeldendeSatsForDato(
                                    SatsType.ORBA,
                                    YearMonth.of(2023, 7).førsteDagIInneværendeMåned(),
                                ),
                            aktør = barn.aktør,
                        ),
                    )
                }

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler

            // Act & Assert
            assertDoesNotThrow { behandlingsresultatStegValideringService.validerSatsendring(tilkjentYtelse) }
        }
    }

    @Nested
    inner class ValiderFinnmarkstilleggBehandling {
        @Test
        fun `skal ikke kaste feil dersom eneste endringer i andeler har vært i finnmarkstillegg andeler`() {
            // Arrange
            val forrigeBehandling = lagBehandling(behandlingType = FØRSTEGANGSBEHANDLING, årsak = SØKNAD)
            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 1000,
                        person = barn,
                    ),
                )

            val behandling = lagBehandling(behandlingType = REVURDERING, årsak = FINNMARKSTILLEGG)
            val tilkjentYtelse =
                lagTilkjentYtelse(behandling = behandling) {
                    setOf(
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 1000,
                            person = barn,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn,
                            ytelseType = YtelseType.FINNMARKSTILLEGG,
                        ),
                    )
                }

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler
            every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns mockk(relaxed = true)

            // Act & Assert
            assertDoesNotThrow { behandlingsresultatStegValideringService.validerFinnmarkstilleggBehandling(tilkjentYtelse) }
        }

        @Test
        fun `skal kaste Feil dersom det har vært endringer i andre ytelsetyper enn finnmarkstillegg med toggle av`() {
            // Arrange
            val forrigeBehandling = lagBehandling(behandlingType = FØRSTEGANGSBEHANDLING, årsak = SØKNAD)
            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 999, // Forskjellig beløp
                        person = barn,
                    ),
                )

            val behandling = lagBehandling(behandlingType = REVURDERING, årsak = FINNMARKSTILLEGG)
            val tilkjentYtelse =
                lagTilkjentYtelse(behandling = behandling) {
                    setOf(
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 1000, // Endret beløp som ikke skal være tillatt
                            person = barn,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn,
                            ytelseType = YtelseType.FINNMARKSTILLEGG,
                        ),
                    )
                }

            every { featureToggleService.isEnabled(FeatureToggle.OPPRETT_MANUELL_OPPGAVE_AUTOVEDTAK_FINNMARK_SVALBARD) } returns false
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler

            // Act & Assert
            val feil = assertThrows<Feil> { behandlingsresultatStegValideringService.validerFinnmarkstilleggBehandling(tilkjentYtelse) }

            assertThat(feil.message).isEqualTo("Automatisk behandlingen fører til endringer i utbetaling utenom Finnmarkstillegg.")
        }

        @Test
        fun `skal kaste AutovedtakMåBehandlesManueltFeil dersom det har vært endringer i andre ytelsetyper enn finnmarkstillegg med toggle på`() {
            // Arrange
            val forrigeBehandling = lagBehandling(behandlingType = FØRSTEGANGSBEHANDLING, årsak = SØKNAD)
            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 999, // Forskjellig beløp
                        person = barn,
                    ),
                )

            val behandling = lagBehandling(behandlingType = REVURDERING, årsak = FINNMARKSTILLEGG)
            val tilkjentYtelse =
                lagTilkjentYtelse(behandling = behandling) {
                    setOf(
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 1000, // Endret beløp som ikke skal være tillatt
                            person = barn,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn,
                            ytelseType = YtelseType.SVALBARDTILLEGG,
                        ),
                    )
                }

            every { featureToggleService.isEnabled(FeatureToggle.OPPRETT_MANUELL_OPPGAVE_AUTOVEDTAK_FINNMARK_SVALBARD) } returns true
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler

            // Act & Assert
            val feil = assertThrows<AutovedtakMåBehandlesManueltFeil> { behandlingsresultatStegValideringService.validerFinnmarkstilleggBehandling(tilkjentYtelse) }

            assertThat(feil.message).isEqualTo("Automatisk behandlingen fører til endringer i utbetaling utenom Finnmarkstillegg.")
        }

        @Test
        fun `skal ikke kaste feil dersom finnmarkstillegg er innvilget fra og med inneværende måned for et barn og neste måned for et annet barn`() {
            // Arrange
            val forrigeBehandling = lagBehandling(behandlingType = FØRSTEGANGSBEHANDLING, årsak = SØKNAD)
            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 999,
                        person = barn,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 999,
                        person = barn2,
                    ),
                )

            val behandling = lagBehandling(behandlingType = REVURDERING, årsak = FINNMARKSTILLEGG)
            val tilkjentYtelse =
                lagTilkjentYtelse(behandling = behandling) {
                    setOf(
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 999,
                            person = barn,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.now(clockProvider.get()), // Inneværende måned
                            tom = YearMonth.now(clockProvider.get()).plusMonths(122),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn,
                            ytelseType = YtelseType.FINNMARKSTILLEGG,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 999,
                            person = barn2,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.now(clockProvider.get()).plusMonths(1), // Neste måned
                            tom = YearMonth.now(clockProvider.get()).plusMonths(122),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn2,
                            ytelseType = YtelseType.FINNMARKSTILLEGG,
                        ),
                    )
                }

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler
            every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns mockk(relaxed = true)

            // Act & Assert
            assertDoesNotThrow { behandlingsresultatStegValideringService.validerFinnmarkstilleggBehandling(tilkjentYtelse) }
        }

        @Test
        fun `skal kaste Feil dersom finnmarkstillegg er innvilget inneværende måned og om to måneder med toggle av`() {
            // Arrange
            val forrigeBehandling = lagBehandling(behandlingType = FØRSTEGANGSBEHANDLING, årsak = SØKNAD)
            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 999,
                        person = barn,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 999,
                        person = barn2,
                    ),
                )

            val behandling = lagBehandling(behandlingType = REVURDERING, årsak = FINNMARKSTILLEGG)
            val tilkjentYtelse =
                lagTilkjentYtelse(behandling = behandling) {
                    setOf(
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 999,
                            person = barn,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.now(clockProvider.get()), // Inneværende måned
                            tom = YearMonth.now(clockProvider.get()).plusMonths(122),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn,
                            ytelseType = YtelseType.FINNMARKSTILLEGG,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 999,
                            person = barn2,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.now(clockProvider.get()).plusMonths(2), // Mer enn 1 måned fram i tid
                            tom = YearMonth.now(clockProvider.get()).plusMonths(122),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn2,
                            ytelseType = YtelseType.FINNMARKSTILLEGG,
                        ),
                    )
                }

            every { featureToggleService.isEnabled(FeatureToggle.OPPRETT_MANUELL_OPPGAVE_AUTOVEDTAK_FINNMARK_SVALBARD) } returns false
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler
            every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns mockk(relaxed = true)

            // Act & Assert
            val feil = assertThrows<Feil> { behandlingsresultatStegValideringService.validerFinnmarkstilleggBehandling(tilkjentYtelse) }

            assertThat(feil.message).isEqualTo("Automatisk behandling fører til innvilgelse av Finnmarkstillegg mer enn én måned fram i tid.")
        }

        @Test
        fun `skal kaste AutovedtakMåBehandlesManueltFeil dersom finnmarkstillegg er innvilget inneværende måned og om to måneder med toggle på`() {
            // Arrange
            val forrigeBehandling = lagBehandling(behandlingType = FØRSTEGANGSBEHANDLING, årsak = SØKNAD)
            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 999,
                        person = barn,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 999,
                        person = barn2,
                    ),
                )

            val behandling = lagBehandling(behandlingType = REVURDERING, årsak = FINNMARKSTILLEGG)
            val tilkjentYtelse =
                lagTilkjentYtelse(behandling = behandling) {
                    setOf(
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 999,
                            person = barn,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.now(clockProvider.get()), // Inneværende måned
                            tom = YearMonth.now(clockProvider.get()).plusMonths(122),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn,
                            ytelseType = YtelseType.FINNMARKSTILLEGG,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 999,
                            person = barn2,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.now(clockProvider.get()).plusMonths(2), // Mer enn 1 måned fram i tid
                            tom = YearMonth.now(clockProvider.get()).plusMonths(122),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn2,
                            ytelseType = YtelseType.FINNMARKSTILLEGG,
                        ),
                    )
                }

            every { featureToggleService.isEnabled(FeatureToggle.OPPRETT_MANUELL_OPPGAVE_AUTOVEDTAK_FINNMARK_SVALBARD) } returns true
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler
            every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns mockk(relaxed = true)

            // Act & Assert
            val feil = assertThrows<AutovedtakMåBehandlesManueltFeil> { behandlingsresultatStegValideringService.validerFinnmarkstilleggBehandling(tilkjentYtelse) }

            assertThat(feil.message).isEqualTo("Automatisk behandling fører til innvilgelse av Finnmarkstillegg mer enn én måned fram i tid.")
        }

        @Test
        fun `skal kaste rekjør senere-exception dersom finnmarkstillegg er innvilget to måneder fram i tid`() {
            // Arrange
            val forrigeBehandling = lagBehandling(behandlingType = FØRSTEGANGSBEHANDLING, årsak = SØKNAD)
            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 999,
                        person = barn,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 999,
                        person = barn2,
                    ),
                )

            val behandling = lagBehandling(behandlingType = REVURDERING, årsak = FINNMARKSTILLEGG)
            val tilkjentYtelse =
                lagTilkjentYtelse(behandling = behandling) {
                    setOf(
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 999,
                            person = barn,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.now(clockProvider.get()).plusMonths(1), // Neste måned
                            tom = YearMonth.now(clockProvider.get()).plusMonths(122),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn,
                            ytelseType = YtelseType.FINNMARKSTILLEGG,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 999,
                            person = barn2,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.now(clockProvider.get()).plusMonths(2), // Mer enn 1 måned fram i tid
                            tom = YearMonth.now(clockProvider.get()).plusMonths(122),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn2,
                            ytelseType = YtelseType.FINNMARKSTILLEGG,
                        ),
                    )
                }

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler
            every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns mockk(relaxed = true)

            // Act & Assert
            val feil = assertThrows<RekjørSenereException> { behandlingsresultatStegValideringService.validerFinnmarkstilleggBehandling(tilkjentYtelse) }

            val forventetTriggertid = LocalDate.now(clockProvider.get()).førsteDagINesteMåned().atTime(6, 0)
            assertThat(feil.triggerTid).isEqualTo(forventetTriggertid)
            assertThat(feil.årsak).isEqualTo(
                "Det eksisterer FINNMARKSTILLEGG-andeler som er innvilget mer enn en måned fram i tid. " +
                    "Disse andelene kan ikke innvilges ennå. Prøver igjen neste måned.",
            )
        }
    }

    @Nested
    inner class ValiderSvalbardtilleggBehandling {
        @Test
        fun `skal ikke kaste feil dersom eneste endringer i andeler har vært i svalbardtillegg andeler`() {
            // Arrange
            val forrigeBehandling = lagBehandling(behandlingType = FØRSTEGANGSBEHANDLING, årsak = SØKNAD)
            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 1000,
                        person = barn,
                    ),
                )

            val behandling = lagBehandling(behandlingType = REVURDERING, årsak = SVALBARDTILLEGG)
            val tilkjentYtelse =
                lagTilkjentYtelse(behandling = behandling) {
                    setOf(
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 1000,
                            person = barn,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn,
                            ytelseType = YtelseType.SVALBARDTILLEGG,
                        ),
                    )
                }

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler

            // Act & Assert
            assertDoesNotThrow { behandlingsresultatStegValideringService.validerSvalbardtilleggBehandling(tilkjentYtelse) }
        }

        @Test
        fun `skal kaste Feil dersom det har vært endringer i andre ytelsetyper enn svalbardtillegg med toggle av`() {
            // Arrange
            val forrigeBehandling = lagBehandling(behandlingType = FØRSTEGANGSBEHANDLING, årsak = SØKNAD)
            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 999, // Forskjellig beløp
                        person = barn,
                    ),
                )

            val behandling = lagBehandling(behandlingType = REVURDERING, årsak = SVALBARDTILLEGG)
            val tilkjentYtelse =
                lagTilkjentYtelse(behandling = behandling) {
                    setOf(
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 1000, // Endret beløp som ikke skal være tillatt
                            person = barn,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn,
                            ytelseType = YtelseType.SVALBARDTILLEGG,
                        ),
                    )
                }

            every { featureToggleService.isEnabled(FeatureToggle.OPPRETT_MANUELL_OPPGAVE_AUTOVEDTAK_FINNMARK_SVALBARD) } returns false
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler

            // Act & Assert
            val feil = assertThrows<Feil> { behandlingsresultatStegValideringService.validerSvalbardtilleggBehandling(tilkjentYtelse) }

            assertThat(feil.message).isEqualTo("Automatisk behandlingen fører til endringer i utbetaling utenom Svalbardtillegg.")
        }

        @Test
        fun `skal kaste AutovedtakMåBehandlesManueltFeil dersom det har vært endringer i andre ytelsetyper enn svalbardtillegg med toggle på`() {
            // Arrange
            val forrigeBehandling = lagBehandling(behandlingType = FØRSTEGANGSBEHANDLING, årsak = SØKNAD)
            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 999, // Forskjellig beløp
                        person = barn,
                    ),
                )

            val behandling = lagBehandling(behandlingType = REVURDERING, årsak = SVALBARDTILLEGG)
            val tilkjentYtelse =
                lagTilkjentYtelse(behandling = behandling) {
                    setOf(
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 1000, // Endret beløp som ikke skal være tillatt
                            person = barn,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn,
                            ytelseType = YtelseType.SVALBARDTILLEGG,
                        ),
                    )
                }

            every { featureToggleService.isEnabled(FeatureToggle.OPPRETT_MANUELL_OPPGAVE_AUTOVEDTAK_FINNMARK_SVALBARD) } returns true
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler

            // Act & Assert
            val feil = assertThrows<AutovedtakMåBehandlesManueltFeil> { behandlingsresultatStegValideringService.validerSvalbardtilleggBehandling(tilkjentYtelse) }

            assertThat(feil.message).isEqualTo("Automatisk behandlingen fører til endringer i utbetaling utenom Svalbardtillegg.")
        }

        @Test
        fun `skal ikke kaste feil dersom svalbardtillegg er innvilget fra og med inneværende måned for et barn og neste måned for et annet barn`() {
            // Arrange
            val forrigeBehandling = lagBehandling(behandlingType = FØRSTEGANGSBEHANDLING, årsak = SØKNAD)
            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 999,
                        person = barn,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 999,
                        person = barn2,
                    ),
                )

            val behandling = lagBehandling(behandlingType = REVURDERING, årsak = SVALBARDTILLEGG)
            val tilkjentYtelse =
                lagTilkjentYtelse(behandling = behandling) {
                    setOf(
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 999,
                            person = barn,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.now(clockProvider.get()), // Inneværende måned
                            tom = YearMonth.now(clockProvider.get()).plusMonths(122),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn,
                            ytelseType = YtelseType.SVALBARDTILLEGG,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 999,
                            person = barn2,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.now(clockProvider.get()).plusMonths(1), // Neste måned
                            tom = YearMonth.now(clockProvider.get()).plusMonths(122),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn2,
                            ytelseType = YtelseType.SVALBARDTILLEGG,
                        ),
                    )
                }

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler
            every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns mockk(relaxed = true)

            // Act & Assert
            assertDoesNotThrow { behandlingsresultatStegValideringService.validerSvalbardtilleggBehandling(tilkjentYtelse) }
        }

        @Test
        fun `skal kaste Feil dersom svalbardtillegg er innvilget inneværende måned og om to måneder med toggle av`() {
            // Arrange
            val forrigeBehandling = lagBehandling(behandlingType = FØRSTEGANGSBEHANDLING, årsak = SØKNAD)
            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 999,
                        person = barn,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 999,
                        person = barn2,
                    ),
                )

            val behandling = lagBehandling(behandlingType = REVURDERING, årsak = SVALBARDTILLEGG)
            val tilkjentYtelse =
                lagTilkjentYtelse(behandling = behandling) {
                    setOf(
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 999,
                            person = barn,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.now(clockProvider.get()), // Inneværende måned
                            tom = YearMonth.now(clockProvider.get()).plusMonths(122),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn,
                            ytelseType = YtelseType.SVALBARDTILLEGG,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 999,
                            person = barn2,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.now(clockProvider.get()).plusMonths(2), // Mer enn 1 måned fram i tid
                            tom = YearMonth.now(clockProvider.get()).plusMonths(122),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn2,
                            ytelseType = YtelseType.SVALBARDTILLEGG,
                        ),
                    )
                }

            every { featureToggleService.isEnabled(FeatureToggle.OPPRETT_MANUELL_OPPGAVE_AUTOVEDTAK_FINNMARK_SVALBARD) } returns false
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler
            every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns mockk(relaxed = true)

            // Act & Assert
            val feil = assertThrows<Feil> { behandlingsresultatStegValideringService.validerSvalbardtilleggBehandling(tilkjentYtelse) }

            assertThat(feil.message).isEqualTo("Automatisk behandling fører til innvilgelse av Svalbardtillegg mer enn én måned fram i tid.")
        }

        @Test
        fun `skal kaste AutovedtakMåBehandlesManueltFeil dersom svalbardtillegg er innvilget inneværende måned og om to måneder med toggle på`() {
            // Arrange
            val forrigeBehandling = lagBehandling(behandlingType = FØRSTEGANGSBEHANDLING, årsak = SØKNAD)
            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 999,
                        person = barn,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 999,
                        person = barn2,
                    ),
                )

            val behandling = lagBehandling(behandlingType = REVURDERING, årsak = SVALBARDTILLEGG)
            val tilkjentYtelse =
                lagTilkjentYtelse(behandling = behandling) {
                    setOf(
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 999,
                            person = barn,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.now(clockProvider.get()), // Inneværende måned
                            tom = YearMonth.now(clockProvider.get()).plusMonths(122),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn,
                            ytelseType = YtelseType.SVALBARDTILLEGG,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 999,
                            person = barn2,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.now(clockProvider.get()).plusMonths(2), // Mer enn 1 måned fram i tid
                            tom = YearMonth.now(clockProvider.get()).plusMonths(122),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn2,
                            ytelseType = YtelseType.SVALBARDTILLEGG,
                        ),
                    )
                }

            every { featureToggleService.isEnabled(FeatureToggle.OPPRETT_MANUELL_OPPGAVE_AUTOVEDTAK_FINNMARK_SVALBARD) } returns true
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler
            every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns mockk(relaxed = true)

            // Act & Assert
            val feil = assertThrows<AutovedtakMåBehandlesManueltFeil> { behandlingsresultatStegValideringService.validerSvalbardtilleggBehandling(tilkjentYtelse) }

            assertThat(feil.message).isEqualTo("Automatisk behandling fører til innvilgelse av Svalbardtillegg mer enn én måned fram i tid.")
        }

        @Test
        fun `skal kaste rekjør senere-exception dersom svalbardtillegg er innvilget to måneder fram i tid`() {
            // Arrange
            val forrigeBehandling = lagBehandling(behandlingType = FØRSTEGANGSBEHANDLING, årsak = SØKNAD)
            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 999,
                        person = barn,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        beløp = 999,
                        person = barn2,
                    ),
                )

            val behandling = lagBehandling(behandlingType = REVURDERING, årsak = SVALBARDTILLEGG)
            val tilkjentYtelse =
                lagTilkjentYtelse(behandling = behandling) {
                    setOf(
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 999,
                            person = barn,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.now(clockProvider.get()).plusMonths(1), // Neste måned
                            tom = YearMonth.now(clockProvider.get()).plusMonths(122),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn,
                            ytelseType = YtelseType.SVALBARDTILLEGG,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 1),
                            tom = YearMonth.of(2023, 2),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 999,
                            person = barn2,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.now(clockProvider.get()).plusMonths(2), // Mer enn 1 måned fram i tid
                            tom = YearMonth.now(clockProvider.get()).plusMonths(122),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn2,
                            ytelseType = YtelseType.SVALBARDTILLEGG,
                        ),
                    )
                }

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler
            every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns mockk(relaxed = true)

            // Act & Assert
            val feil = assertThrows<RekjørSenereException> { behandlingsresultatStegValideringService.validerSvalbardtilleggBehandling(tilkjentYtelse) }

            val forventetTriggertid = LocalDate.now(clockProvider.get()).førsteDagINesteMåned().atTime(6, 0)
            assertThat(feil.triggerTid).isEqualTo(forventetTriggertid)
            assertThat(feil.årsak).isEqualTo(
                "Det eksisterer SVALBARDTILLEGG-andeler som er innvilget mer enn en måned fram i tid. " +
                    "Disse andelene kan ikke innvilges ennå. Prøver igjen neste måned.",
            )
        }
    }

    @Nested
    inner class ValiderIngenEndringIUtbetalingEtterMigreringsdatoenTilForrigeIverksatteBehandling {
        @Test
        fun `skal ikke kaste feil når det ikke er endringer etter migreringsdato`() {
            // Arrange
            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2025, 1),
                        tom = YearMonth.of(2042, 12),
                        beløp = 1000,
                        aktør = barn.aktør,
                    ),
                )
            val nåværendeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2024, 1),
                        tom = YearMonth.of(2042, 12),
                        beløp = 1000,
                        aktør = barn.aktør,
                    ),
                )

            val endringIUtbetalingTidslinje =
                EndringIUtbetalingUtil.lagEndringIUtbetalingTidslinje(
                    nåværendeAndeler = nåværendeAndeler,
                    forrigeAndeler = forrigeAndeler,
                )

            every { beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomiTidslinje(behandling) } returns endringIUtbetalingTidslinje
            every { beregningService.hentAndelerFraForrigeIverksattebehandling(behandling) } returns forrigeAndeler

            // Act & Assert
            assertDoesNotThrow {
                behandlingsresultatStegValideringService.validerIngenEndringIUtbetalingEtterMigreringsdatoenTilForrigeIverksatteBehandling(behandling)
            }
        }

        @Test
        fun `skal kaste feil når det er endringer etter migreringsdato`() {
            // Arrange
            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2025, 1),
                        tom = YearMonth.of(2042, 12),
                        beløp = 1000,
                        aktør = barn.aktør,
                    ),
                )
            val nåværendeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2026, 1),
                        tom = YearMonth.of(2042, 12),
                        beløp = 1000,
                        aktør = barn.aktør,
                    ),
                )

            val endringIUtbetalingTidslinje =
                EndringIUtbetalingUtil.lagEndringIUtbetalingTidslinje(
                    nåværendeAndeler = nåværendeAndeler,
                    forrigeAndeler = forrigeAndeler,
                )

            every { beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomiTidslinje(behandling) } returns endringIUtbetalingTidslinje
            every { beregningService.hentAndelerFraForrigeIverksattebehandling(behandling) } returns forrigeAndeler

            // Act & Assert
            val feil =
                assertThrows<FunksjonellFeil> {
                    behandlingsresultatStegValideringService.validerIngenEndringIUtbetalingEtterMigreringsdatoenTilForrigeIverksatteBehandling(behandling)
                }

            assertThat(feil.message).contains("Det finnes endringer i behandlingen som har økonomisk konsekvens for bruker")
        }

        @Test
        fun `skal ikke kaste feil når behandling er avsluttet`() {
            // Arrange
            val behandling = lagBehandling(status = BehandlingStatus.AVSLUTTET)

            // Act & Assert
            assertDoesNotThrow {
                behandlingsresultatStegValideringService.validerIngenEndringIUtbetalingEtterMigreringsdatoenTilForrigeIverksatteBehandling(behandling)
            }
        }
    }
}
