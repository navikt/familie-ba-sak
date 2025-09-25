package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.TestClockProvider.Companion.lagClockProviderMedFastTidspunkt
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.SatsendringFeil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagKompetanse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.datagenerator.lagValutakurs
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
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
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
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
        )

    private val søker = lagPerson(type = PersonType.SØKER)
    private val barn = lagPerson(type = PersonType.BARN)
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
        fun `skal kaste feil dersom det har vært endringer i andeler annet enn finnmarkstillegg`() {
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
            val feil = assertThrows<Feil> { behandlingsresultatStegValideringService.validerFinnmarkstilleggBehandling(tilkjentYtelse) }

            assertThat(feil.message).contains(
                "Det er oppdaget forskjell i utbetaling utenom FINNMARKSTILLEGG andeler. Dette kan ikke skje " +
                    "i en behandling der årsak er FINNMARKSTILLEGG, og den automatiske kjøring stoppes derfor.",
            )
        }

        @Test
        fun `skal kaste feil dersom tidspunktet for førstegang innvilgelse av finnmarkstillegget ligger mer enn 1 måned fram i tid`() {
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
                            fom = YearMonth.now(clockProvider.get()).plusMonths(2), // Mer enn 1 måned fram i tid
                            tom = YearMonth.now(clockProvider.get()).plusMonths(122),
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
            val feil = assertThrows<Feil> { behandlingsresultatStegValideringService.validerFinnmarkstilleggBehandling(tilkjentYtelse) }

            assertThat(feil.message).contains(
                "Det eksisterer FINNMARKSTILLEGG andeler som først blir innvilget mer enn 1 måned " +
                    "fram i tid. Det er ikke mulig å innvilge disse enda, og behandlingen stoppes derfor.",
            )
        }

        @Test
        fun `skal kaste feil dersom det eksisterer barn som ikke bor i finnmark men har delt bosted i perioder søker bor i finnmark`() {
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
                            fom = YearMonth.now().plusMonths(1),
                            tom = YearMonth.now().plusMonths(122),
                            behandling = behandling,
                            tilkjentYtelse = it,
                            beløp = 500,
                            person = barn,
                            ytelseType = YtelseType.FINNMARKSTILLEGG,
                        ),
                    )
                }

            val vilkårsvurdering =
                lagVilkårsvurdering(behandling = behandling) {
                    setOf(
                        lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                            behandling = behandling,
                            person = søker,
                            perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 1, 1) to null),
                            vilkårsvurdering = it,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS, UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD),
                        ),
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            aktør = barn.aktør,
                            lagVilkårResultater = { personResultat ->
                                setOf(
                                    lagVilkårResultat(
                                        personResultat = personResultat,
                                        vilkårType = Vilkår.BOSATT_I_RIKET,
                                        resultat = Resultat.OPPFYLT,
                                        periodeFom = LocalDate.of(2025, 1, 1),
                                        periodeTom = LocalDate.now().plusYears(1),
                                        behandlingId = behandling.id,
                                    ),
                                    lagVilkårResultat(
                                        personResultat = personResultat,
                                        vilkårType = Vilkår.UNDER_18_ÅR,
                                        resultat = Resultat.OPPFYLT,
                                        periodeFom = LocalDate.now().minusYears(15),
                                        periodeTom = LocalDate.now().plusYears(1),
                                        behandlingId = behandling.id,
                                    ),
                                    lagVilkårResultat(
                                        personResultat = personResultat,
                                        vilkårType = Vilkår.BOR_MED_SØKER,
                                        resultat = Resultat.OPPFYLT,
                                        periodeFom = LocalDate.of(2025, 1, 1),
                                        periodeTom = LocalDate.now().plusYears(1),
                                        behandlingId = behandling.id,
                                        utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                                    ),
                                )
                            },
                        ),
                    )
                }

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler
            every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns vilkårsvurdering

            // Act & Assert
            val feil = assertThrows<Feil> { behandlingsresultatStegValideringService.validerFinnmarkstilleggBehandling(tilkjentYtelse) }

            assertThat(feil.message).contains(
                "Det finnes perioder der søker bor i finnmark samtidig som et barn med delt bosted ikke bor " +
                    "i finnmark. Disse sakene støtter vi ikke automatisk, og vi stanser derfor denne behandlingen.",
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
        fun `skal kaste feil dersom det har vært endringer i andeler annet enn svalbardtillegg`() {
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

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler

            // Act & Assert
            val feil = assertThrows<Feil> { behandlingsresultatStegValideringService.validerSvalbardtilleggBehandling(tilkjentYtelse) }

            assertThat(feil.message).contains(
                "Det er oppdaget forskjell i utbetaling utenom SVALBARDTILLEGG andeler. Dette kan ikke skje " +
                    "i en behandling der årsak er SVALBARDTILLEGG, og den automatiske kjøring stoppes derfor.",
            )
        }

        @Test
        fun `skal kaste feil dersom tidspunktet for førstegang innvilgelse av svalbardtillegget ligger mer enn 1 måned fram i tid`() {
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
                            fom = YearMonth.now(clockProvider.get()).plusMonths(2), // Mer enn 1 måned fram i tid
                            tom = YearMonth.now(clockProvider.get()).plusMonths(122),
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
            val feil = assertThrows<Feil> { behandlingsresultatStegValideringService.validerSvalbardtilleggBehandling(tilkjentYtelse) }

            assertThat(feil.message).contains(
                "Det eksisterer SVALBARDTILLEGG andeler som først blir innvilget mer enn 1 måned " +
                    "fram i tid. Det er ikke mulig å innvilge disse enda, og behandlingen stoppes derfor.",
            )
        }
    }
}
