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
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagKompetanse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.datagenerator.lagValutakurs
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.SatsendringEøsKjøringService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.domene.SatsendringEøsKjøring
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType.REVURDERING
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.FINNMARKSTILLEGG
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.SATSENDRING
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.SATSENDRING_EØS
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
    private val strengtFortroligService = mockk<no.nav.familie.ba.sak.kjerne.strengtfortrolig.StrengtFortroligService>(relaxed = true)
    private val clockProvider = lagClockProviderMedFastTidspunkt(LocalDate.of(2025, 10, 10))
    private val satsendringEøsKjøringService: SatsendringEøsKjøringService = mockk()

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
            strengtFortroligService = strengtFortroligService,
            clockProvider = clockProvider,
            satsendringEøsKjøringService = satsendringEøsKjøringService,
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

        @Test
        fun `skal kaste feil dersom det finnes sekundærland kompetanser med fom senere enn inneværende måned`() {
            // Arrange
            val enMånedFramITid = YearMonth.now().plusMonths(1)
            val gyldigKompetanse =
                lagKompetanse(
                    behandlingId = behandling.id,
                    kompetanseResultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                    søkersAktivitetsland = "NO",
                    barnetsBostedsland = "SE",
                    annenForeldersAktivitetsland = "NO",
                    barnAktører = setOf(barn.aktør),
                    fom = enMånedFramITid,
                )

            every { kompetanseRepository.finnFraBehandlingId(behandling.id) } returns listOf(gyldigKompetanse)

            // Act & Assert
            val feil = assertThrows<FunksjonellFeil> { behandlingsresultatStegValideringService.validerKompetanse(behandling.id) }

            assertThat(feil.melding).isEqualTo(
                "Det er kompetanse som starter lengre fram i tid enn inneværende måned." +
                    " Det er ikke mulig å hente inn valutakurs for perioder fram i tid," +
                    " og du må derfor vente til ${enMånedFramITid.tilMånedÅr()} før du kan fortsette behandlingen.",
            )
        }
    }

    @Nested
    inner class ValiderSekundærlandKompetanseTest {
        @Test
        fun `Skal kaste funksjonell feil ved sekundærland kompetanser uten utenlandskperiode beløp eller valutakurs`() {
            // Arrange
            val sekundærlandKompetanse =
                lagKompetanse(
                    behandlingId = behandling.id,
                    kompetanseResultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                    søkersAktivitetsland = "NO",
                    barnetsBostedsland = "SE",
                    annenForeldersAktivitetsland = "SE",
                    barnAktører = setOf(barn.aktør),
                    fom = YearMonth.of(2021, 1),
                    tom = YearMonth.of(2025, 12),
                )

            val valutakurs =
                lagValutakurs(
                    fom = YearMonth.of(2021, 1),
                    tom = YearMonth.of(2025, 11),
                    valutakode = "SEK",
                    valutakursdato = LocalDate.now(),
                    kurs = BigDecimal.valueOf(1.2),
                    barnAktører = setOf(barn.aktør),
                    vurderingsform = Vurderingsform.MANUELL,
                )

            val utenlandskPeriodebeløp =
                lagUtenlandskPeriodebeløp(
                    fom = YearMonth.of(2021, 1),
                    tom = YearMonth.of(2025, 11),
                    beløp = BigDecimal.valueOf(100),
                    intervall = Intervall.MÅNEDLIG,
                    valutakode = "SEK",
                    utbetalingsland = "S",
                    barnAktører = setOf(barn.aktør),
                )

            every { kompetanseRepository.finnFraBehandlingId(behandling.id) } returns listOf(sekundærlandKompetanse)
            every { valutakursRepository.finnFraBehandlingId(behandling.id) } returns listOf(valutakurs)
            every { utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandling.id) } returns listOf(utenlandskPeriodebeløp)

            // Act & Assert
            val feil = assertThrows<FunksjonellFeil> { behandlingsresultatStegValideringService.validerSekundærlandKompetanse(behandling.id) }

            assertThat(feil.melding).isEqualTo(
                """
                For perioden desember 2025 finnes det sekundærland kompetanse med endret utbetaling i det andre landet en måned som er lengre fram i tid enn inneværende måned.
                Det er ikke mulig å hente inn valutakurs for perioder fram i tid, og du må derfor vente til desember 2025 før du kan fortsette behandlingen.
                """.trimIndent(),
            )
        }

        @Test
        fun `Skal kaste annen feilmelding ved feil i samme måned eller før`() {
            // Arrange
            val sekundærlandKompetanse =
                lagKompetanse(
                    behandlingId = behandling.id,
                    kompetanseResultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                    søkersAktivitetsland = "NO",
                    barnetsBostedsland = "SE",
                    annenForeldersAktivitetsland = "SE",
                    barnAktører = setOf(barn.aktør),
                    fom = YearMonth.of(2021, 1),
                    tom = YearMonth.of(2025, 10),
                )

            val valutakurs =
                lagValutakurs(
                    fom = YearMonth.of(2021, 1),
                    tom = YearMonth.of(2025, 9),
                    valutakode = "SEK",
                    valutakursdato = LocalDate.now(),
                    kurs = BigDecimal.valueOf(1.2),
                    barnAktører = setOf(barn.aktør),
                    vurderingsform = Vurderingsform.MANUELL,
                )

            val utenlandskPeriodebeløp =
                lagUtenlandskPeriodebeløp(
                    fom = YearMonth.of(2021, 1),
                    tom = YearMonth.of(2025, 9),
                    beløp = BigDecimal.valueOf(100),
                    intervall = Intervall.MÅNEDLIG,
                    valutakode = "SEK",
                    utbetalingsland = "S",
                    barnAktører = setOf(barn.aktør),
                )

            every { kompetanseRepository.finnFraBehandlingId(behandling.id) } returns listOf(sekundærlandKompetanse)
            every { valutakursRepository.finnFraBehandlingId(behandling.id) } returns listOf(valutakurs)
            every { utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandling.id) } returns listOf(utenlandskPeriodebeløp)

            // Act & Assert
            val feil = assertThrows<FunksjonellFeil> { behandlingsresultatStegValideringService.validerSekundærlandKompetanse(behandling.id) }

            assertThat(feil.melding).isEqualTo(
                """
                For perioden oktober 2025 finnes det sekundærland kompetanse som enda ikke har fått utenlandskperiode beløp eller valutakurs.
                Gå tilbake til vilkårsvurderingen og trykk 'Neste' for å hente inn manglende utenlandskperiode beløp og valutakurs.
                """.trimIndent(),
            )
        }

        @Test
        fun `Skal ikke kaste feil ved primærland perioder uten utenlandsk beløp eller valutakurs`() {
            // Arrange
            // Arrange
            val sekundærlandKompetanse =
                lagKompetanse(
                    behandlingId = behandling.id,
                    kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                    søkersAktivitetsland = "NO",
                    barnetsBostedsland = "SE",
                    annenForeldersAktivitetsland = "NO",
                    barnAktører = setOf(barn.aktør),
                    fom = YearMonth.of(2021, 1),
                    tom = YearMonth.of(2025, 10),
                )

            val valutakurs =
                lagValutakurs(
                    fom = YearMonth.of(2021, 1),
                    tom = YearMonth.of(2025, 9),
                    valutakode = "SEK",
                    valutakursdato = LocalDate.now(),
                    kurs = BigDecimal.valueOf(1.2),
                    barnAktører = setOf(barn.aktør),
                    vurderingsform = Vurderingsform.MANUELL,
                )

            val utenlandskPeriodebeløp =
                lagUtenlandskPeriodebeløp(
                    fom = YearMonth.of(2021, 1),
                    tom = YearMonth.of(2025, 9),
                    beløp = BigDecimal.valueOf(100),
                    intervall = Intervall.MÅNEDLIG,
                    valutakode = "SEK",
                    utbetalingsland = "S",
                    barnAktører = setOf(barn.aktør),
                )

            every { kompetanseRepository.finnFraBehandlingId(behandling.id) } returns listOf(sekundærlandKompetanse)
            every { valutakursRepository.finnFraBehandlingId(behandling.id) } returns listOf(valutakurs)
            every { utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandling.id) } returns listOf(utenlandskPeriodebeløp)

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
        fun `skal kaste AutovedtakMåBehandlesManueltFeil dersom det har vært endringer i svalbardtillegg i en finnmarkstillegg-behandling med toggle på`() {
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
                        ytelseType = YtelseType.SVALBARDTILLEGG,
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
                            ytelseType = YtelseType.SVALBARDTILLEGG,
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

            // Act & Assert
            val feil = assertThrows<AutovedtakMåBehandlesManueltFeil> { behandlingsresultatStegValideringService.validerFinnmarkstilleggBehandling(tilkjentYtelse) }

            assertThat(feil.message).isEqualTo(
                "Finnmarkstillegg kan ikke behandles automatisk som følge av adresseendring.\n" +
                    "Endring av Finnmarkstillegg fører også til endring av Svalbardtillegg.\n" +
                    "Endring av Finnmarkstillegg og Svalbardtillegg må håndteres manuelt.",
            )
        }

        @Test
        fun `skal kaste Feil dersom det har vært endringer i andre ytelsetyper enn finnmarkstillegg og svalbardtillegg`() {
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

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler

            // Act & Assert
            val feil = assertThrows<Feil> { behandlingsresultatStegValideringService.validerFinnmarkstilleggBehandling(tilkjentYtelse) }

            assertThat(feil.message).isEqualTo(
                "Det er en uforventet endring i andeler utenom Finnmarkstillegg og Svalbardtillegg i en FINNMARKSTILLEGG-behandling.",
            )
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

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler
            every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns mockk(relaxed = true)

            // Act & Assert
            val feil = assertThrows<AutovedtakMåBehandlesManueltFeil> { behandlingsresultatStegValideringService.validerFinnmarkstilleggBehandling(tilkjentYtelse) }

            assertThat(feil.message).isEqualTo(
                "Finnmarkstillegg kan ikke behandles automatisk som følge av adresseendring.\n" +
                    "Automatisk behandling fører til innvilgelse av Finnmarkstillegg mer enn én måned fram i tid.\nEndring av Finnmarkstillegg må håndteres manuelt.",
            )
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
        fun `skal kaste AutovedtakMåBehandlesManueltFeil dersom det har vært endringer i finnmarkstillegg i en svalbardtillegg-behandling med toggle på`() {
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
                        ytelseType = YtelseType.FINNMARKSTILLEGG,
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
                            ytelseType = YtelseType.FINNMARKSTILLEGG,
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
            val feil = assertThrows<AutovedtakMåBehandlesManueltFeil> { behandlingsresultatStegValideringService.validerSvalbardtilleggBehandling(tilkjentYtelse) }

            assertThat(feil.message).isEqualTo(
                "Svalbardtillegg kan ikke behandles automatisk som følge av adresseendring.\n" +
                    "Endring av Svalbardtillegg fører også til endring av Finnmarkstillegg.\n" +
                    "Endring av Svalbardtillegg og Finnmarkstillegg må håndteres manuelt.",
            )
        }

        @Test
        fun `skal kaste Feil dersom det har vært endringer i andre ytelsetyper enn finnmarkstillegg og svalbardtillegg`() {
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
                            ytelseType = YtelseType.FINNMARKSTILLEGG,
                        ),
                    )
                }

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler

            // Act & Assert
            val feil = assertThrows<Feil> { behandlingsresultatStegValideringService.validerSvalbardtilleggBehandling(tilkjentYtelse) }

            assertThat(feil.message).isEqualTo(
                "Det er en uforventet endring i andeler utenom Finnmarkstillegg og Svalbardtillegg i en SVALBARDTILLEGG-behandling.",
            )
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

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeAndeler
            every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns mockk(relaxed = true)

            // Act & Assert
            val feil = assertThrows<AutovedtakMåBehandlesManueltFeil> { behandlingsresultatStegValideringService.validerSvalbardtilleggBehandling(tilkjentYtelse) }

            assertThat(feil.message).isEqualTo(
                "Svalbardtillegg kan ikke behandles automatisk som følge av adresseendring.\n" +
                    "Automatisk behandling fører til innvilgelse av Svalbardtillegg mer enn én måned fram i tid.\nEndring av Svalbardtillegg må håndteres manuelt.",
            )
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

    @Nested
    inner class ValiderFalskIdentitetBehandling {
        @Test
        fun `skal kaste feil dersom det er innvilget nye andeler i behandling`() {
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

            val tilkjentYtelse = lagTilkjentYtelse { nåværendeAndeler.toSet() }

            every { beregningService.hentAndelerFraForrigeVedtatteBehandling(tilkjentYtelse.behandling) } returns forrigeAndeler

            // Act
            val funksjonellFeil = assertThrows<FunksjonellFeil> { behandlingsresultatStegValideringService.validerFalskIdentitetBehandling(tilkjentYtelse) }

            // Assert
            assertThat(funksjonellFeil.message).isEqualTo("Det finnes nye andeler i behandling. Kan ikke innvilge nye andeler i 'Falsk identitet'-behandling.")
        }

        @Test
        fun `skal ikke kaste feil dersom det er fjernet andeler i behandling`() {
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
                        fom = YearMonth.of(2025, 1),
                        tom = YearMonth.of(2026, 1),
                        beløp = 1000,
                        aktør = barn.aktør,
                    ),
                )

            val tilkjentYtelse = lagTilkjentYtelse { nåværendeAndeler.toSet() }

            every { beregningService.hentAndelerFraForrigeVedtatteBehandling(tilkjentYtelse.behandling) } returns forrigeAndeler

            // Act
            assertDoesNotThrow { behandlingsresultatStegValideringService.validerFalskIdentitetBehandling(tilkjentYtelse) }
        }
    }

    @Nested
    inner class ValiderIngenEndringIUtbetalingIPerioderMedSkjermedeBarnTest {
        private val skjermetBarnAktør = randomAktør()
        private val annetBarnAktør = randomAktør()
        private val søkerAktør = randomAktør()
        private val fagsak = lagFagsak(aktør = søkerAktør)
        private val nåværendeBehandling = lagBehandling(fagsak = fagsak)
        private val forrigeBehandling = lagBehandling(fagsak = fagsak)
        private val skjermetPeriodeFom = YearMonth.of(2023, 1)
        private val skjermetPeriodeTom = YearMonth.of(2024, 6)

        private val skjermetBarnsHistoriskeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = skjermetPeriodeFom,
                    tom = skjermetPeriodeTom,
                    behandling = forrigeBehandling,
                    aktør = skjermetBarnAktør,
                ),
            )

        private val skjermetBarnsAndelerINåværendeBehandling =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = skjermetPeriodeFom,
                    tom = skjermetPeriodeTom,
                    behandling = nåværendeBehandling,
                    aktør = skjermetBarnAktør,
                ),
            )

        @Test
        fun `skal ikke kaste feil når det ikke finnes skjermede barn uten løpende andeler`() {
            // Arrange
            every { strengtFortroligService.hentSkjermedeBarnUtenLøpendeAndelerSaksbehandlerIkkeHarTilgangTil(fagsak) } returns emptySet()

            // Act + Assert
            assertDoesNotThrow {
                behandlingsresultatStegValideringService.validerIngenEndringIUtbetalingIPerioderMedSkjermedeBarn(nåværendeBehandling)
            }
        }

        @Test
        fun `skal ikke kaste feil når det ikke finnes en forrige vedtatt behandling`() {
            // Arrange
            every { strengtFortroligService.hentSkjermedeBarnUtenLøpendeAndelerSaksbehandlerIkkeHarTilgangTil(fagsak) } returns setOf(skjermetBarnAktør.aktivFødselsnummer())
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(nåværendeBehandling) } returns null

            // Act + Assert
            assertDoesNotThrow {
                behandlingsresultatStegValideringService.validerIngenEndringIUtbetalingIPerioderMedSkjermedeBarn(nåværendeBehandling)
            }
        }

        @Test
        fun `skal ikke kaste feil når andre barns andeler er uendret i skjermet periode`() {
            // Arrange
            mockSkjermetBarnFinnesOgForrigeBehandlingErVedtatt()
            val annetBarnsAndel =
                lagAndelTilkjentYtelse(
                    fom = skjermetPeriodeFom,
                    tom = skjermetPeriodeTom,
                    behandling = forrigeBehandling,
                    aktør = annetBarnAktør,
                )
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns skjermetBarnsHistoriskeAndeler + annetBarnsAndel
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(nåværendeBehandling.id) } returns
                skjermetBarnsAndelerINåværendeBehandling + annetBarnsAndel.copy(id = 0, behandlingId = nåværendeBehandling.id)

            // Act + Assert
            assertDoesNotThrow {
                behandlingsresultatStegValideringService.validerIngenEndringIUtbetalingIPerioderMedSkjermedeBarn(nåværendeBehandling)
            }
        }

        @Test
        fun `skal ikke kaste feil når endring for andre barn er utenfor skjermet periode`() {
            // Arrange
            mockSkjermetBarnFinnesOgForrigeBehandlingErVedtatt()
            val annetBarnsAndelFørSkjermetPeriode =
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2022, 1),
                    tom = YearMonth.of(2022, 12),
                    behandling = forrigeBehandling,
                    aktør = annetBarnAktør,
                    beløp = 1000,
                )
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns skjermetBarnsHistoriskeAndeler + annetBarnsAndelFørSkjermetPeriode
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(nåværendeBehandling.id) } returns
                skjermetBarnsAndelerINåværendeBehandling +
                annetBarnsAndelFørSkjermetPeriode.copy(id = 0, behandlingId = nåværendeBehandling.id, kalkulertUtbetalingsbeløp = 2000)

            // Act + Assert
            assertDoesNotThrow {
                behandlingsresultatStegValideringService.validerIngenEndringIUtbetalingIPerioderMedSkjermedeBarn(nåværendeBehandling)
            }
        }

        @Test
        fun `skal kaste Funksjonell feil når endring for annet barn er innenfor skjermet periode`() {
            // Arrange
            mockSkjermetBarnFinnesOgForrigeBehandlingErVedtatt()
            val annetBarnsAndelForrige =
                lagAndelTilkjentYtelse(
                    fom = skjermetPeriodeFom,
                    tom = skjermetPeriodeTom,
                    behandling = forrigeBehandling,
                    aktør = annetBarnAktør,
                    beløp = 1000,
                )
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns skjermetBarnsHistoriskeAndeler + annetBarnsAndelForrige
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(nåværendeBehandling.id) } returns
                skjermetBarnsAndelerINåværendeBehandling +
                annetBarnsAndelForrige.copy(id = 0, behandlingId = nåværendeBehandling.id, kalkulertUtbetalingsbeløp = 2000)

            // Act + Assert
            val feil =
                assertThrows<FunksjonellFeil> {
                    behandlingsresultatStegValideringService.validerIngenEndringIUtbetalingIPerioderMedSkjermedeBarn(nåværendeBehandling)
                }

            assertThat(feil.message).contains("Behandlingen må overføres til enhet 2103")
        }

        @Test
        fun `skal kaste funksjonell feil ved endring i søkers utbetaling i skjermet periode`() {
            // Arrange
            mockSkjermetBarnFinnesOgForrigeBehandlingErVedtatt()
            val søkerAndelForrige =
                lagAndelTilkjentYtelse(
                    fom = skjermetPeriodeFom,
                    tom = skjermetPeriodeTom,
                    behandling = forrigeBehandling,
                    aktør = søkerAktør,
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                    beløp = 1000,
                )
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns
                skjermetBarnsHistoriskeAndeler + søkerAndelForrige
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(nåværendeBehandling.id) } returns
                skjermetBarnsAndelerINåværendeBehandling +
                søkerAndelForrige.copy(id = 0, behandlingId = nåværendeBehandling.id, kalkulertUtbetalingsbeløp = 1500)

            // Act + Assert
            val feil =
                assertThrows<FunksjonellFeil> {
                    behandlingsresultatStegValideringService.validerIngenEndringIUtbetalingIPerioderMedSkjermedeBarn(nåværendeBehandling)
                }

            assertThat(feil.message).contains("Behandlingen må overføres til enhet 2103")
        }

        @Test
        fun `skal kaste feil ved endring i det skjermede barnets egne andeler`() {
            // Arrange
            mockSkjermetBarnFinnesOgForrigeBehandlingErVedtatt()
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(nåværendeBehandling.id) } returns
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = skjermetPeriodeFom,
                        tom = skjermetPeriodeTom,
                        behandling = nåværendeBehandling,
                        aktør = skjermetBarnAktør,
                        beløp = 9999,
                    ),
                )

            // Act + Assert
            assertThrows<FunksjonellFeil> {
                behandlingsresultatStegValideringService.validerIngenEndringIUtbetalingIPerioderMedSkjermedeBarn(nåværendeBehandling)
            }
        }

        @Test
        fun `skal kaste feil dersom skjermet barns andeler fjernes i ny behandling`() {
            // Arrange
            mockSkjermetBarnFinnesOgForrigeBehandlingErVedtatt()
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(nåværendeBehandling.id) } returns emptyList()

            // Act + Assert
            assertThrows<FunksjonellFeil> {
                behandlingsresultatStegValideringService.validerIngenEndringIUtbetalingIPerioderMedSkjermedeBarn(nåværendeBehandling)
            }
        }

        private fun mockSkjermetBarnFinnesOgForrigeBehandlingErVedtatt() {
            every { strengtFortroligService.hentSkjermedeBarnUtenLøpendeAndelerSaksbehandlerIkkeHarTilgangTil(fagsak) } returns setOf(skjermetBarnAktør.aktivFødselsnummer())
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(nåværendeBehandling) } returns forrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns skjermetBarnsHistoriskeAndeler
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(nåværendeBehandling.id) } returns skjermetBarnsAndelerINåværendeBehandling
        }
    }

    @Nested
    inner class ValiderAtMinstEttUtenlandskPeriodebeløpErEndret {
        private val barn = lagPerson(type = PersonType.BARN)
        private val forrigeBehandling = lagBehandling(årsak = SØKNAD)
        private val nåværendeBehandling = lagBehandling(årsak = SATSENDRING_EØS)
        private val satsendringstidspunkt = YearMonth.of(2025, 5)
        private val kjøring =
            SatsendringEøsKjøring(
                fagsakId = nåværendeBehandling.fagsak.id,
                behandlingId = nåværendeBehandling.id,
                utbetalingsland = "PL",
                satsTidspunkt = satsendringstidspunkt,
            )

        @Test
        fun `skal ikke kaste feil når minst ett UPB har endret beløp`() {
            // Arrange
            val forrigeUpb =
                lagUtenlandskPeriodebeløp(
                    behandlingId = forrigeBehandling.id,
                    fom = satsendringstidspunkt,
                    beløp = BigDecimal("1000"),
                    intervall = Intervall.MÅNEDLIG,
                    valutakode = "PLN",
                    utbetalingsland = "PL",
                    barnAktører = setOf(barn.aktør),
                )
            val nyUpb =
                lagUtenlandskPeriodebeløp(
                    behandlingId = nåværendeBehandling.id,
                    fom = satsendringstidspunkt,
                    beløp = BigDecimal("1200"),
                    intervall = Intervall.MÅNEDLIG,
                    valutakode = "PLN",
                    utbetalingsland = "PL",
                    barnAktører = setOf(barn.aktør),
                )
            every { satsendringEøsKjøringService.hentSatsendringEøsKjøring(nåværendeBehandling.id) } returns kjøring
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(nåværendeBehandling) } returns forrigeBehandling
            every { utenlandskPeriodebeløpRepository.finnFraBehandlingId(nåværendeBehandling.id) } returns listOf(nyUpb)
            every { utenlandskPeriodebeløpRepository.finnFraBehandlingId(forrigeBehandling.id) } returns listOf(forrigeUpb)

            // Act & Assert
            assertDoesNotThrow {
                behandlingsresultatStegValideringService.validerAtMinstEttUtenlandskPeriodebeløpErEndret(nåværendeBehandling)
            }
        }

        @Test
        fun `skal kaste feil når beløp er identisk med forrige behandling`() {
            // Arrange
            val forrigeUpb =
                lagUtenlandskPeriodebeløp(
                    behandlingId = forrigeBehandling.id,
                    fom = satsendringstidspunkt,
                    beløp = BigDecimal("1000"),
                    intervall = Intervall.MÅNEDLIG,
                    valutakode = "PLN",
                    utbetalingsland = "PL",
                    barnAktører = setOf(barn.aktør),
                )
            val nyUpb =
                lagUtenlandskPeriodebeløp(
                    behandlingId = nåværendeBehandling.id,
                    fom = satsendringstidspunkt,
                    beløp = BigDecimal("1000"),
                    intervall = Intervall.MÅNEDLIG,
                    valutakode = "PLN",
                    utbetalingsland = "PL",
                    barnAktører = setOf(barn.aktør),
                )
            every { satsendringEøsKjøringService.hentSatsendringEøsKjøring(nåværendeBehandling.id) } returns kjøring
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(nåværendeBehandling) } returns forrigeBehandling
            every { utenlandskPeriodebeløpRepository.finnFraBehandlingId(nåværendeBehandling.id) } returns listOf(nyUpb)
            every { utenlandskPeriodebeløpRepository.finnFraBehandlingId(forrigeBehandling.id) } returns listOf(forrigeUpb)

            // Act & Assert
            assertThrows<Feil> {
                behandlingsresultatStegValideringService.validerAtMinstEttUtenlandskPeriodebeløpErEndret(nåværendeBehandling)
            }
        }

        @Test
        fun `skal kaste feil når UPB-endring er for et annet land enn det kjøringen gjelder`() {
            // Arrange
            val forrigeUpb =
                lagUtenlandskPeriodebeløp(
                    behandlingId = forrigeBehandling.id,
                    fom = satsendringstidspunkt,
                    beløp = BigDecimal("1000"),
                    intervall = Intervall.MÅNEDLIG,
                    valutakode = "SEK",
                    utbetalingsland = "SE",
                    barnAktører = setOf(barn.aktør),
                )
            val nyUpb =
                lagUtenlandskPeriodebeløp(
                    behandlingId = nåværendeBehandling.id,
                    fom = satsendringstidspunkt,
                    beløp = BigDecimal("1200"),
                    intervall = Intervall.MÅNEDLIG,
                    valutakode = "SEK",
                    utbetalingsland = "SE",
                    barnAktører = setOf(barn.aktør),
                )
            every { satsendringEøsKjøringService.hentSatsendringEøsKjøring(nåværendeBehandling.id) } returns kjøring
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(nåværendeBehandling) } returns forrigeBehandling
            every { utenlandskPeriodebeløpRepository.finnFraBehandlingId(nåværendeBehandling.id) } returns listOf(nyUpb)
            every { utenlandskPeriodebeløpRepository.finnFraBehandlingId(forrigeBehandling.id) } returns listOf(forrigeUpb)

            // Act & Assert
            assertThrows<Feil> {
                behandlingsresultatStegValideringService.validerAtMinstEttUtenlandskPeriodebeløpErEndret(nåværendeBehandling)
            }
        }

        @Test
        fun `skal kaste feil når det ikke finnes SatsendringEøsKjøring for behandlingen`() {
            every { satsendringEøsKjøringService.hentSatsendringEøsKjøring(nåværendeBehandling.id) } throws Feil("Fant ikke")

            assertThrows<Feil> {
                behandlingsresultatStegValideringService.validerAtMinstEttUtenlandskPeriodebeløpErEndret(nåværendeBehandling)
            }
        }
    }

    @Nested
    inner class ValiderIngenEndringFørSatsendringstidspunkt {
        private val barn = lagPerson(type = PersonType.BARN)
        private val forrigeBehandling = lagBehandling(årsak = SØKNAD)
        private val nåværendeBehandling = lagBehandling(årsak = SATSENDRING_EØS)
        private val satsendringstidspunkt = YearMonth.of(2025, 5)
        private val kjøring =
            SatsendringEøsKjøring(
                fagsakId = nåværendeBehandling.fagsak.id,
                behandlingId = nåværendeBehandling.id,
                utbetalingsland = "PL",
                satsTidspunkt = satsendringstidspunkt,
            )

        @Test
        fun `skal ikke kaste feil når andeler er uendret før satsendringstidspunktet`() {
            // Arrange
            val tilkjentYtelse =
                lagTilkjentYtelse(
                    behandling = nåværendeBehandling,
                    lagAndelerTilkjentYtelse = { ty ->
                        setOf(
                            lagAndelTilkjentYtelse(
                                fom = YearMonth.of(2025, 1),
                                tom = satsendringstidspunkt.minusMonths(1),
                                aktør = barn.aktør,
                                kalkulertUtbetalingsbeløp = 500,
                                tilkjentYtelse = ty,
                            ),
                            lagAndelTilkjentYtelse(
                                fom = satsendringstidspunkt,
                                tom = YearMonth.of(2025, 12),
                                aktør = barn.aktør,
                                kalkulertUtbetalingsbeløp = 400,
                                tilkjentYtelse = ty,
                            ),
                        )
                    },
                )
            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2025, 1),
                        tom = satsendringstidspunkt.minusMonths(1),
                        aktør = barn.aktør,
                        kalkulertUtbetalingsbeløp = 500,
                        behandling = forrigeBehandling,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = satsendringstidspunkt,
                        tom = YearMonth.of(2025, 12),
                        aktør = barn.aktør,
                        kalkulertUtbetalingsbeløp = 500,
                        behandling = forrigeBehandling,
                    ),
                )

            every { satsendringEøsKjøringService.hentSatsendringEøsKjøring(nåværendeBehandling.id) } returns kjøring
            every { beregningService.hentAndelerFraForrigeVedtatteBehandling(nåværendeBehandling) } returns forrigeAndeler

            // Act & Assert
            assertDoesNotThrow {
                behandlingsresultatStegValideringService.validerIngenEndringIAndelerFørSatsendringstidspunkt(tilkjentYtelse)
            }
        }

        @Test
        fun `skal kaste feil når kalkulert utbetalingsbeløp er endret i en måned før satsendringstidspunktet`() {
            // Arrange
            val tilkjentYtelse =
                lagTilkjentYtelse(
                    behandling = nåværendeBehandling,
                    lagAndelerTilkjentYtelse = { ty ->
                        setOf(
                            lagAndelTilkjentYtelse(
                                fom = YearMonth.of(2025, 1),
                                tom = YearMonth.of(2025, 12),
                                aktør = barn.aktør,
                                kalkulertUtbetalingsbeløp = 400,
                                tilkjentYtelse = ty,
                            ),
                        )
                    },
                )
            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2025, 1),
                        tom = YearMonth.of(2025, 12),
                        aktør = barn.aktør,
                        kalkulertUtbetalingsbeløp = 500,
                        behandling = forrigeBehandling,
                    ),
                )

            every { satsendringEøsKjøringService.hentSatsendringEøsKjøring(nåværendeBehandling.id) } returns kjøring
            every { beregningService.hentAndelerFraForrigeVedtatteBehandling(nåværendeBehandling) } returns forrigeAndeler

            // Act & Assert
            assertThrows<Feil> {
                behandlingsresultatStegValideringService.validerIngenEndringIAndelerFørSatsendringstidspunkt(tilkjentYtelse)
            }
        }

        @Test
        fun `skal kaste feil når det ikke finnes SatsendringEøsKjøring for behandlingen`() {
            val tilkjentYtelse = lagTilkjentYtelse(behandling = nåværendeBehandling)

            every { satsendringEøsKjøringService.hentSatsendringEøsKjøring(nåværendeBehandling.id) } throws Feil("Fant ikke")

            assertThrows<Feil> {
                behandlingsresultatStegValideringService.validerIngenEndringIAndelerFørSatsendringstidspunkt(tilkjentYtelse)
            }
        }
    }
}
