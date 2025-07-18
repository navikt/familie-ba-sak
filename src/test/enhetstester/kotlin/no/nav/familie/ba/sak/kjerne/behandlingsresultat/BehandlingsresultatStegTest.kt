package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.TestClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.SatsendringFeil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagKompetanse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.datagenerator.lagValutakurs
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.tilPersonEnkel
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Vurderingsform
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIUtbetalingUtil
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.småbarnstillegg.SmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.steg.EndringerIUtbetalingForBehandlingSteg
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class BehandlingsresultatStegTest {
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val simuleringService: SimuleringService = mockk()
    private val vedtakService: VedtakService = mockk()
    private val vedtaksperiodeService: VedtaksperiodeService = mockk()
    private val mockBehandlingsresultatService: BehandlingsresultatService = mockk()
    private val vilkårService: VilkårService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val beregningService: BeregningService = mockk()
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService = mockk<AndelerTilkjentYtelseOgEndreteUtbetalingerService>()
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository = mockk()
    private val utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository = mockk()
    private val valutakursRepository: ValutakursRepository = mockk()
    private val valutakursService = mockk<ValutakursService>()
    private val kompetanseRepository = mockk<KompetanseRepository>()
    private val småbarnstilleggService = mockk<SmåbarnstilleggService>()
    private val tilbakestillBehandlingService = mockk<TilbakestillBehandlingService>()

    private val behandlingsresultatSteg: BehandlingsresultatSteg =
        BehandlingsresultatSteg(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            behandlingService = behandlingService,
            simuleringService = simuleringService,
            vedtakService = vedtakService,
            vedtaksperiodeService = vedtaksperiodeService,
            behandlingsresultatService = mockBehandlingsresultatService,
            vilkårService = vilkårService,
            persongrunnlagService = persongrunnlagService,
            beregningService = beregningService,
            andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            utenlandskPeriodebeløpRepository = utenlandskPeriodebeløpRepository,
            valutakursRepository = valutakursRepository,
            clockProvider = TestClockProvider(),
            kompetanseRepository = kompetanseRepository,
            småbarnstilleggService = småbarnstilleggService,
            tilbakestillBehandlingService = tilbakestillBehandlingService,
        )

    private val behandling =
        lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
        )

    @BeforeEach
    fun init() {
        every { simuleringService.oppdaterSimuleringPåBehandling(any()) } returns emptyList()
        every { simuleringService.hentSimuleringPåBehandling(any()) } returns emptyList()
        every { valutakursService.hentValutakurser(any()) } returns emptyList()
        every { kompetanseRepository.finnFraBehandlingId(any()) } returns emptyList()
        justRun {
            tilbakestillBehandlingService
                .slettTilbakekrevingsvedtakMotregningHvisBehandlingIkkeAvregner(any())
        }
    }

    @Nested
    inner class UtførStegOgAngiNesteTest {
        @Test
        fun `Skal gå rett fra behandlingsresultat til iverksetting for alle fødselshendelser`() {
            val fødselshendelseBehandling =
                behandling.copy(
                    skalBehandlesAutomatisk = true,
                    opprettetÅrsak = BehandlingÅrsak.FØDSELSHENDELSE,
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                )
            val vedtak =
                lagVedtak(
                    fødselshendelseBehandling,
                )
            every { mockBehandlingsresultatService.utledBehandlingsresultat(any()) } returns Behandlingsresultat.INNVILGET_OG_ENDRET
            every { behandlingService.nullstillEndringstidspunkt(fødselshendelseBehandling.id) } just runs
            every {
                behandlingService.oppdaterBehandlingsresultat(
                    any(),
                    any(),
                )
            } returns fødselshendelseBehandling.copy(resultat = Behandlingsresultat.INNVILGET_OG_ENDRET)
            every {
                behandlingService.oppdaterStatusPåBehandling(
                    fødselshendelseBehandling.id,
                    BehandlingStatus.IVERKSETTER_VEDTAK,
                )
            } returns fødselshendelseBehandling.copy(status = BehandlingStatus.IVERKSETTER_VEDTAK)
            every { vedtakService.hentAktivForBehandlingThrows(fødselshendelseBehandling.id) } returns vedtak
            every { vedtaksperiodeService.oppdaterVedtakMedVedtaksperioder(vedtak) } just runs
            every { beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(fødselshendelseBehandling) } returns EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING
            every { utenlandskPeriodebeløpRepository.finnFraBehandlingId(fødselshendelseBehandling.id) } returns emptyList()
            every { valutakursRepository.finnFraBehandlingId(fødselshendelseBehandling.id) } returns emptyList()

            assertEquals(
                behandlingsresultatSteg.utførStegOgAngiNeste(fødselshendelseBehandling, ""),
                StegType.IVERKSETT_MOT_OPPDRAG,
            )
        }

        @Test
        fun `Skal kaste exception hvis behandlingsresultat er Avslått for en manuell migrering`() {
            every { mockBehandlingsresultatService.utledBehandlingsresultat(any()) } returns Behandlingsresultat.AVSLÅTT

            every {
                behandlingService.oppdaterBehandlingsresultat(
                    any(),
                    any(),
                )
            } returns behandling.copy(resultat = Behandlingsresultat.AVSLÅTT)

            val exception = assertThrows<RuntimeException> { behandlingsresultatSteg.utførStegOgAngiNeste(behandling, "") }
            assertEquals(
                "Du har fått behandlingsresultatet Avslått. " +
                    "Dette er ikke støttet på migreringsbehandlinger. " +
                    "Meld sak i Porten om du er uenig i resultatet.",
                exception.message,
            )
        }

        @Test
        fun `Skal kaste exception hvis behandlingsresultat er Delvis Innvilget for en manuell migrering`() {
            every { mockBehandlingsresultatService.utledBehandlingsresultat(any()) } returns Behandlingsresultat.DELVIS_INNVILGET

            every {
                behandlingService.oppdaterBehandlingsresultat(
                    any(),
                    any(),
                )
            } returns behandling.copy(resultat = Behandlingsresultat.DELVIS_INNVILGET)

            val exception = assertThrows<RuntimeException> { behandlingsresultatSteg.utførStegOgAngiNeste(behandling, "") }
            assertEquals(
                "Du har fått behandlingsresultatet Delvis innvilget. " +
                    "Dette er ikke støttet på migreringsbehandlinger. " +
                    "Meld sak i Porten om du er uenig i resultatet.",
                exception.message,
            )
        }

        @Test
        fun `Skal kaste exception hvis behandlingsresultat er Avslått,Endret og Opphørt for en manuell migrering`() {
            every { mockBehandlingsresultatService.utledBehandlingsresultat(any()) } returns Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT

            every {
                behandlingService.oppdaterBehandlingsresultat(
                    any(),
                    any(),
                )
            } returns behandling.copy(resultat = Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT)

            val exception = assertThrows<RuntimeException> { behandlingsresultatSteg.utførStegOgAngiNeste(behandling, "") }
            assertEquals(
                "Du har fått behandlingsresultatet Avslått, endret og opphørt. " +
                    "Dette er ikke støttet på migreringsbehandlinger. " +
                    "Meld sak i Porten om du er uenig i resultatet.",
                exception.message,
            )
        }

        @Test
        fun `Skal kaste exception dersom det finnes utenlandskperiodebeløp som ikke er fylt ut`() {
            every { mockBehandlingsresultatService.utledBehandlingsresultat(any()) } returns Behandlingsresultat.FORTSATT_INNVILGET

            every {
                behandlingService.oppdaterBehandlingsresultat(
                    any(),
                    any(),
                )
            } returns behandling.copy(resultat = Behandlingsresultat.FORTSATT_INNVILGET)

            every { utenlandskPeriodebeløpRepository.finnFraBehandlingId(any()) } returns listOf(lagUtenlandskPeriodebeløp())
            every { valutakursRepository.finnFraBehandlingId(any()) } returns listOf(lagValutakurs())

            val exception = assertThrows<FunksjonellFeil> { behandlingsresultatSteg.utførStegOgAngiNeste(behandling, "") }
            assertEquals(
                "Kan ikke fullføre behandlingsresultat-steg før utbetalt i det andre landet og valutakurs er fylt ut for alle barn og perioder",
                exception.message,
            )
        }

        @Test
        fun `Skal kaste exception dersom det finnes valutakurser som ikke er fylt ut`() {
            every { mockBehandlingsresultatService.utledBehandlingsresultat(any()) } returns Behandlingsresultat.FORTSATT_INNVILGET

            every {
                behandlingService.oppdaterBehandlingsresultat(
                    any(),
                    any(),
                )
            } returns behandling.copy(resultat = Behandlingsresultat.FORTSATT_INNVILGET)

            every { utenlandskPeriodebeløpRepository.finnFraBehandlingId(any()) } returns
                listOf(
                    lagUtenlandskPeriodebeløp(
                        fom = LocalDate.now().toYearMonth(),
                        beløp = BigDecimal.valueOf(100),
                        intervall = Intervall.MÅNEDLIG,
                        valutakode = "SEK",
                        utbetalingsland = "S",
                        barnAktører =
                            setOf(
                                randomAktør(),
                            ),
                    ),
                )
            every { valutakursRepository.finnFraBehandlingId(any()) } returns listOf(lagValutakurs())

            val exception = assertThrows<FunksjonellFeil> { behandlingsresultatSteg.utførStegOgAngiNeste(behandling, "") }
            assertEquals(
                "Kan ikke fullføre behandlingsresultat-steg før utbetalt i det andre landet og valutakurs er fylt ut for alle barn og perioder",
                exception.message,
            )
        }

        @Test
        fun `Skal ikke kaste exception dersom upb og valutakurser er utfylt`() {
            every { mockBehandlingsresultatService.utledBehandlingsresultat(any()) } returns Behandlingsresultat.FORTSATT_INNVILGET

            every {
                behandlingService.oppdaterBehandlingsresultat(
                    any(),
                    any(),
                )
            } returns behandling.copy(resultat = Behandlingsresultat.FORTSATT_INNVILGET)

            every { utenlandskPeriodebeløpRepository.finnFraBehandlingId(any()) } returns
                listOf(
                    lagUtenlandskPeriodebeløp(
                        fom = LocalDate.now().toYearMonth(),
                        beløp = BigDecimal.valueOf(100),
                        intervall = Intervall.MÅNEDLIG,
                        valutakode = "SEK",
                        utbetalingsland = "S",
                        barnAktører =
                            setOf(
                                randomAktør(),
                            ),
                    ),
                )
            every { valutakursRepository.finnFraBehandlingId(any()) } returns
                listOf(
                    lagValutakurs(
                        fom = LocalDate.now().toYearMonth(),
                        valutakode = "SEK",
                        valutakursdato = LocalDate.now(),
                        kurs = BigDecimal.valueOf(1.2),
                        barnAktører =
                            setOf(
                                randomAktør(),
                            ),
                        vurderingsform = Vurderingsform.MANUELL,
                    ),
                )

            every { beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(any()) } returns EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(any()) } returns lagBehandling()

            every { småbarnstilleggService.kanAutomatiskIverksetteSmåbarnstilleggEndring(any(), any()) } returns true

            every { behandlingService.oppdaterStatusPåBehandling(any(), any()) } returns lagBehandling()

            assertDoesNotThrow { behandlingsresultatSteg.utførStegOgAngiNeste(behandling, "") }
        }
    }

    @Nested
    inner class PreValiderStegTest {
        @Test
        fun `Skal kaste feil dersom det finnes kompetanser der Norge er sekundærland men aktivitetsland og bosted er satt til Norge`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
            val søker = lagPerson(type = PersonType.SØKER)
            val barn = lagPerson(type = PersonType.BARN)
            val personerIBehandling = listOf(barn.tilPersonEnkel(), søker.tilPersonEnkel())

            val personopplysningGrunnlag = mockk<PersonopplysningGrunnlag>()
            val vikårsvurderings = lagVilkårsvurdering(søkerAktør = søker.aktør, behandling = behandling, resultat = Resultat.OPPFYLT)
            val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling = behandling)
            val ugyldigKompetanse =
                lagKompetanse(
                    behandlingId = behandling.id,
                    kompetanseResultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                    søkersAktivitetsland = "NO",
                    barnetsBostedsland = "NO",
                    annenForeldersAktivitetsland = "NO",
                    barnAktører = setOf(barn.aktør),
                )

            every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns vikårsvurderings
            every { vilkårService.hentVilkårsvurdering(behandling.id) } returns vikårsvurderings
            every { personopplysningGrunnlag.søker } returns søker
            every { personopplysningGrunnlag.barna } returns listOf(barn)
            every { persongrunnlagService.hentBarna(any<Behandling>()) } returns emptyList()
            every { persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(any()) } returns personerIBehandling
            every { persongrunnlagService.hentAktivThrows(any()) } returns personopplysningGrunnlag
            every { beregningService.hentTilkjentYtelseForBehandling(behandling.id) } returns tilkjentYtelse
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns emptyList()
            every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id) } returns emptyList()
            every { kompetanseRepository.finnFraBehandlingId(behandling.id) } returns listOf(ugyldigKompetanse)

            // Act & Assert
            val feilmelding =
                assertThrows<FunksjonellFeil> {
                    behandlingsresultatSteg.preValiderSteg(behandling)
                }.melding

            assertThat(feilmelding).isEqualTo("Dersom Norge er sekundærland, må søkers aktivitetsland, annen forelders aktivitetsland eller barnets bostedsland være satt til noe annet enn Norge")
        }

        @Test
        fun `Skal ikke kaste feil dersom det ikke finnes kompetanser der Norge er sekundærland med aktivitetsland og bosted som er satt til Norge`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
            val søker = lagPerson(type = PersonType.SØKER)
            val barn = lagPerson(type = PersonType.BARN)
            val personerIBehandling = listOf(barn.tilPersonEnkel(), søker.tilPersonEnkel())

            val personopplysningGrunnlag = mockk<PersonopplysningGrunnlag>()
            val vikårsvurderings = lagVilkårsvurdering(søkerAktør = søker.aktør, behandling = behandling, resultat = Resultat.OPPFYLT)
            val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling = behandling)
            val ugyldigKompetanse =
                lagKompetanse(
                    behandlingId = behandling.id,
                    kompetanseResultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                    søkersAktivitetsland = "NO",
                    barnetsBostedsland = "SE",
                    annenForeldersAktivitetsland = "NO",
                    barnAktører = setOf(barn.aktør),
                )

            every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns vikårsvurderings
            every { vilkårService.hentVilkårsvurdering(behandling.id) } returns vikårsvurderings
            every { personopplysningGrunnlag.søker } returns søker
            every { personopplysningGrunnlag.barna } returns listOf(barn)
            every { persongrunnlagService.hentBarna(any<Behandling>()) } returns emptyList()
            every { persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(any()) } returns personerIBehandling
            every { persongrunnlagService.hentAktivThrows(any()) } returns personopplysningGrunnlag
            every { beregningService.hentTilkjentYtelseForBehandling(behandling.id) } returns tilkjentYtelse
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns emptyList()
            every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id) } returns emptyList()
            every { kompetanseRepository.finnFraBehandlingId(behandling.id) } returns listOf(ugyldigKompetanse)

            // Act && Assert
            assertDoesNotThrow { behandlingsresultatSteg.preValiderSteg(behandling) }
        }

        @Test
        fun `Skal validere andeler ved satsendring og kaste feil når endringene i andeler er relatert til noe annet enn endring i sats`() {
            val søker = lagPerson()
            val barn = lagPerson()
            val forrigeBehandling =
                lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING, årsak = BehandlingÅrsak.SØKNAD)
            val forrigeTilkjentYtelse = lagInitiellTilkjentYtelse(behandling = forrigeBehandling)
            forrigeTilkjentYtelse.andelerTilkjentYtelse.addAll(
                mutableSetOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        tilkjentYtelse = forrigeTilkjentYtelse,
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
                        tilkjentYtelse = forrigeTilkjentYtelse,
                        beløp =
                            SatsService.finnGjeldendeSatsForDato(
                                SatsType.ORBA,
                                YearMonth.of(2023, 3).førsteDagIInneværendeMåned(),
                            ),
                        aktør = barn.aktør,
                        prosent = BigDecimal(50),
                    ),
                ),
            )

            val behandling =
                lagBehandling(
                    fagsak = forrigeBehandling.fagsak,
                    behandlingType = BehandlingType.REVURDERING,
                    årsak = BehandlingÅrsak.SATSENDRING,
                )
            val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling = behandling)
            tilkjentYtelse.andelerTilkjentYtelse.addAll(
                mutableSetOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        tilkjentYtelse = forrigeTilkjentYtelse,
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
                        behandling = forrigeBehandling,
                        tilkjentYtelse = forrigeTilkjentYtelse,
                        beløp =
                            SatsService.finnGjeldendeSatsForDato(
                                SatsType.ORBA,
                                YearMonth.of(2023, 3).førsteDagIInneværendeMåned(),
                            ),
                        aktør = barn.aktør,
                        prosent = BigDecimal(100),
                    ),
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 7),
                        tom = YearMonth.of(2033, 1),
                        behandling = forrigeBehandling,
                        tilkjentYtelse = forrigeTilkjentYtelse,
                        beløp =
                            SatsService.finnGjeldendeSatsForDato(
                                SatsType.ORBA,
                                YearMonth.of(2023, 7).førsteDagIInneværendeMåned(),
                            ),
                        aktør = barn.aktør,
                        prosent = BigDecimal(50),
                    ),
                ),
            )
            lagMocksForPreValiderStegSatsendring(
                behandling = behandling,
                tilkjentYtelse = tilkjentYtelse,
                forrigeBehandling = forrigeBehandling,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                søker = søker,
                barn = listOf(barn),
            )

            assertThatThrownBy { behandlingsresultatSteg.preValiderSteg(behandling) }
                .isInstanceOf(SatsendringFeil::class.java)
                .hasMessageContaining("Satsendring kan ikke endre på prosenten til en andel")
        }

        @Test
        fun `Skal validere andeler ved satsendring og ikke kaste feil når endringene i andeler kun er relatert til endring i sats`() {
            val søker = lagPerson()
            val barn = lagPerson(type = PersonType.BARN)
            val forrigeBehandling =
                lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING, årsak = BehandlingÅrsak.SØKNAD)

            val forrigeTilkjentYtelse = lagInitiellTilkjentYtelse(behandling = forrigeBehandling)
            forrigeTilkjentYtelse.andelerTilkjentYtelse.addAll(
                mutableSetOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        tilkjentYtelse = forrigeTilkjentYtelse,
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
                        tilkjentYtelse = forrigeTilkjentYtelse,
                        beløp =
                            SatsService.finnGjeldendeSatsForDato(
                                SatsType.ORBA,
                                YearMonth.of(2023, 3).førsteDagIInneværendeMåned(),
                            ),
                        aktør = barn.aktør,
                    ),
                ),
            )

            val behandling =
                lagBehandling(
                    fagsak = forrigeBehandling.fagsak,
                    behandlingType = BehandlingType.REVURDERING,
                    årsak = BehandlingÅrsak.SATSENDRING,
                )
            val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling = behandling)
            tilkjentYtelse.andelerTilkjentYtelse.addAll(
                mutableSetOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2023, 2),
                        behandling = forrigeBehandling,
                        tilkjentYtelse = forrigeTilkjentYtelse,
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
                        behandling = forrigeBehandling,
                        tilkjentYtelse = forrigeTilkjentYtelse,
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
                        behandling = forrigeBehandling,
                        tilkjentYtelse = forrigeTilkjentYtelse,
                        beløp =
                            SatsService.finnGjeldendeSatsForDato(
                                SatsType.ORBA,
                                YearMonth.of(2023, 7).førsteDagIInneværendeMåned(),
                            ),
                        aktør = barn.aktør,
                    ),
                ),
            )
            lagMocksForPreValiderStegSatsendring(
                behandling = behandling,
                tilkjentYtelse = tilkjentYtelse,
                forrigeBehandling = forrigeBehandling,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                søker = søker,
                barn = listOf(barn),
            )

            behandlingsresultatSteg.preValiderSteg(behandling)

            assertThatCode { behandlingsresultatSteg.preValiderSteg(behandling) }.doesNotThrowAnyException()
        }
    }

    @Test
    fun `postValiderSteg - skal validere at behandlingsresultat ved omregning er uendret`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.OMREGNING_18ÅR)

        for (resultat in endretBehandlingsresultat()) {
            behandling.resultat = resultat
            assertThrows<Feil> {
                behandlingsresultatSteg.postValiderSteg(behandling)
            }
        }
        for (resultat in uendretBehandlingsresultat()) {
            behandling.resultat = resultat
            assertDoesNotThrow {
                behandlingsresultatSteg.postValiderSteg(behandling)
            }
        }
    }

    private fun lagMocksForPreValiderStegSatsendring(
        behandling: Behandling,
        tilkjentYtelse: TilkjentYtelse,
        forrigeBehandling: Behandling,
        forrigeTilkjentYtelse: TilkjentYtelse,
        søker: Person,
        barn: List<Person>,
    ) {
        val personopplysningGrunnlag = mockk<PersonopplysningGrunnlag>()
        val vikårsvurderings =
            lagVilkårsvurdering(søkerAktør = søker.aktør, behandling = behandling, resultat = Resultat.OPPFYLT)
        every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns vikårsvurderings
        every { vilkårService.hentVilkårsvurdering(behandling.id) } returns vikårsvurderings
        every { persongrunnlagService.hentBarna(any<Behandling>()) } returns emptyList()
        every { beregningService.hentTilkjentYtelseForBehandling(behandling.id) } returns tilkjentYtelse

        every { persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(any()) } returns barn.map { it.tilPersonEnkel() } + søker.tilPersonEnkel()

        every { persongrunnlagService.hentAktivThrows(any()) } returns personopplysningGrunnlag
        every { personopplysningGrunnlag.søker } returns søker
        every { personopplysningGrunnlag.barna } returns barn
        every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeTilkjentYtelse.andelerTilkjentYtelse.toList()
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id)
        } returns emptyList()

        every { beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomiTidslinje(behandling) } returns
            EndringIUtbetalingUtil.lagEndringIUtbetalingTidslinje(
                nåværendeAndeler = tilkjentYtelse.andelerTilkjentYtelse.toList(),
                forrigeAndeler = forrigeTilkjentYtelse.andelerTilkjentYtelse.toList(),
            )

        every {
            beregningService
                .hentAndelerFraForrigeIverksattebehandling(behandling)
        } returns emptyList()
    }

    private fun endretBehandlingsresultat() =
        listOf(
            Behandlingsresultat.ENDRET_UTBETALING,
            Behandlingsresultat.ENDRET_UTEN_UTBETALING,
            Behandlingsresultat.ENDRET_OG_OPPHØRT,
            Behandlingsresultat.OPPHØRT,
        )

    private fun uendretBehandlingsresultat() =
        listOf(
            Behandlingsresultat.FORTSATT_INNVILGET,
            Behandlingsresultat.FORTSATT_OPPHØRT,
        )
}
