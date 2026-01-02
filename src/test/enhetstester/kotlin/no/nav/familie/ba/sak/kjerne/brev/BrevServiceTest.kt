package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.TestClockProvider
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.AvregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.brev.hjemler.HjemmeltekstUtleder
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.simulering.domene.AvregningPeriode
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning.TilbakekrevingsvedtakMotregning
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.sikkerhet.SaksbehandlerContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class BrevServiceTest {
    val saksbehandlerContext = mockk<SaksbehandlerContext>()
    val brevmalService = mockk<BrevmalService>()
    val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    val endretUtbetalingAndelRepository = mockk<EndretUtbetalingAndelRepository>()
    val hjemmeltekstUtleder = mockk<HjemmeltekstUtleder>()
    val mockPersongrunnlagService = mockk<PersongrunnlagService>()
    val mockTotrinnskontrollService = mockk<TotrinnskontrollService>()
    val mockArbeidsfordelingService = mockk<ArbeidsfordelingService>()
    val mockAvregningService = mockk<AvregningService>()

    val clockProvider = TestClockProvider.lagClockProviderMedFastTidspunkt(LocalDate.of(2025, 12, 1))
    val brevService =
        BrevService(
            totrinnskontrollService = mockTotrinnskontrollService,
            persongrunnlagService = mockPersongrunnlagService,
            arbeidsfordelingService = mockArbeidsfordelingService,
            simuleringService = mockk(),
            vedtaksperiodeService = vedtaksperiodeService,
            korrigertEtterbetalingService = mockk(),
            organisasjonService = mockk(),
            korrigertVedtakService = mockk(),
            saksbehandlerContext = saksbehandlerContext,
            brevmalService = brevmalService,
            kodeverkService = mockk(),
            testVerktøyService = mockk(),
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            utenlandskPeriodebeløpRepository = mockk(),
            valutakursRepository = mockk(),
            kompetanseRepository = mockk(),
            endretUtbetalingAndelRepository = endretUtbetalingAndelRepository,
            hjemmeltekstUtleder = hjemmeltekstUtleder,
            avregningService = mockAvregningService,
            behandlingHentOgPersisterService = mockk(),
            clockProvider = clockProvider,
        )

    @BeforeEach
    fun setUp() {
        every { saksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "saksbehandlerNavn"
        every { hjemmeltekstUtleder.utledHjemmeltekst(any(), any(), any()) } returns "Hjemmel 1, 2, 3"
    }

    @Test
    fun `Saksbehandler blir hentet fra sikkerhetscontext og beslutter viser placeholder tekst under behandling`() {
        val behandling = lagBehandling()

        val (saksbehandler, beslutter) =
            brevService.hentSaksbehandlerOgBeslutter(
                behandling = behandling,
                totrinnskontroll = null,
            )

        Assertions.assertEquals("saksbehandlerNavn", saksbehandler)
        Assertions.assertEquals("Beslutter", beslutter)
    }

    @Test
    fun `Saksbehandler blir hentet og beslutter er hentet fra sikkerhetscontext under beslutning`() {
        val behandling = lagBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.BESLUTTE_VEDTAK)

        val (saksbehandler, beslutter) =
            brevService.hentSaksbehandlerOgBeslutter(
                behandling = behandling,
                totrinnskontroll =
                    Totrinnskontroll(
                        behandling = behandling,
                        saksbehandler = "Mock Saksbehandler",
                        saksbehandlerId = "mock.saksbehandler@nav.no",
                    ),
            )

        Assertions.assertEquals("Mock Saksbehandler", saksbehandler)
        Assertions.assertEquals("saksbehandlerNavn", beslutter)
    }

    @Test
    fun `Saksbehandler blir hentet og beslutter viser placeholder tekst under beslutning`() {
        val behandling = lagBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.BESLUTTE_VEDTAK)

        val (saksbehandler, beslutter) =
            brevService.hentSaksbehandlerOgBeslutter(
                behandling = behandling,
                totrinnskontroll =
                    Totrinnskontroll(
                        behandling = behandling,
                        saksbehandler = "System",
                        saksbehandlerId = "systembruker",
                    ),
            )

        Assertions.assertEquals("System", saksbehandler)
        Assertions.assertEquals("saksbehandlerNavn", beslutter)
    }

    @Test
    fun `Saksbehandler og beslutter blir hentet etter at totrinnskontroll er besluttet`() {
        val behandling = lagBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.BESLUTTE_VEDTAK)

        val (saksbehandler, beslutter) =
            brevService.hentSaksbehandlerOgBeslutter(
                behandling = behandling,
                totrinnskontroll =
                    Totrinnskontroll(
                        behandling = behandling,
                        saksbehandler = "Mock Saksbehandler",
                        saksbehandlerId = "mock.saksbehandler@nav.no",
                        beslutter = "Mock Beslutter",
                        beslutterId = "mock.beslutter@nav.no",
                    ),
            )

        Assertions.assertEquals("Mock Saksbehandler", saksbehandler)
        Assertions.assertEquals("Mock Beslutter", beslutter)
    }

    @Test
    fun `sjekkOmDetErLøpendeDifferanseUtbetalingPåBehandling skal returnere false dersom det ikke er noe andeler i behandlingen`() {
        val behandling = lagBehandling()

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns emptyList()

        val erLøpendeDifferanseUtbetalingPåBehandling = brevService.sjekkOmDetErLøpendeDifferanseUtbetalingPåBehandling(behandling)

        assertThat(erLøpendeDifferanseUtbetalingPåBehandling).isFalse()
    }

    @Test
    fun `sjekkOmDetErLøpendeDifferanseUtbetalingPåBehandling skal returnere false dersom det ikke er noe løpende andeler i behandlingen`() {
        val behandling = lagBehandling()
        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusMonths(5),
                    tom = YearMonth.now().minusMonths(3),
                ),
            )

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns andeler

        val erLøpendeDifferanseUtbetalingPåBehandling = brevService.sjekkOmDetErLøpendeDifferanseUtbetalingPåBehandling(behandling)

        assertThat(erLøpendeDifferanseUtbetalingPåBehandling).isFalse()
    }

    @Test
    fun `sjekkOmDetErLøpendeDifferanseUtbetalingPåBehandling skal returnere false dersom det løpende andeler i behandlingen men ikke differanseberegnet`() {
        val behandling = lagBehandling()
        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusMonths(5),
                    tom = YearMonth.now().plusMonths(3),
                ),
            )

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns andeler

        val erLøpendeDifferanseUtbetalingPåBehandling = brevService.sjekkOmDetErLøpendeDifferanseUtbetalingPåBehandling(behandling)

        assertThat(erLøpendeDifferanseUtbetalingPåBehandling).isFalse()
    }

    @Test
    fun `sjekkOmDetErLøpendeDifferanseUtbetalingPåBehandling skal returnere true dersom det løpende andeler i behandlingen som er differanseberegnet`() {
        val behandling = lagBehandling()
        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusMonths(5),
                    tom = YearMonth.now().plusMonths(3),
                    differanseberegnetPeriodebeløp = 500,
                ),
            )

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns andeler

        val erLøpendeDifferanseUtbetalingPåBehandling = brevService.sjekkOmDetErLøpendeDifferanseUtbetalingPåBehandling(behandling)

        assertThat(erLøpendeDifferanseUtbetalingPåBehandling).isTrue()
    }

    @ParameterizedTest
    @EnumSource(BehandlingÅrsak::class, mode = EnumSource.Mode.EXCLUDE, names = ["ÅRLIG_KONTROLL"])
    fun `finnStarttidspunktForUtbetalingstabell returnerer endringstidspunkt for alle behandlingsårsaker utenom ÅRLIG_KONTROLL`(
        behandlingÅrsak: BehandlingÅrsak,
    ) {
        every { vedtaksperiodeService.finnEndringstidspunktForBehandling(any()) } returns LocalDate.of(2020, 1, 1)

        val behandling = lagBehandling(årsak = behandlingÅrsak)

        val starttidspunkt = brevService.finnStarttidspunktForUtbetalingstabell(behandling)

        assertThat(starttidspunkt).isEqualTo(LocalDate.of(2020, 1, 1))
    }

    @Test
    fun `finnStarttidspunktForUtbetalingstabell returnerer endringstidspunkt for behandlingsårsak ÅRLIG_KONTROLL, dersom endringstidspunkt er tidligere enn 1 januar i fjor`() {
        every { vedtaksperiodeService.finnEndringstidspunktForBehandling(any()) } returns LocalDate.of(2020, 1, 1)

        val behandling = lagBehandling(årsak = BehandlingÅrsak.ÅRLIG_KONTROLL)

        val starttidspunkt = brevService.finnStarttidspunktForUtbetalingstabell(behandling)

        assertThat(starttidspunkt).isEqualTo(LocalDate.of(2020, 1, 1))
    }

    @Test
    fun `finnStarttidspunktForUtbetalingstabell returnerer tidligst 1 januar året før for behandlingsårsak ÅRLIG_KONTROLL, dersom endringstidspunkt er TIDENES_ENDE`() {
        every { vedtaksperiodeService.finnEndringstidspunktForBehandling(any()) } returns TIDENES_ENDE
        every { endretUtbetalingAndelRepository.findByBehandlingId(any()) } returns emptyList()
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.now().plusYears(1),
                    ),
                )

        val behandling = lagBehandling(årsak = BehandlingÅrsak.ÅRLIG_KONTROLL)

        val starttidspunkt = brevService.finnStarttidspunktForUtbetalingstabell(behandling)

        assertThat(starttidspunkt).isEqualTo(LocalDate.now(clockProvider.get()).minusYears(1).withDayOfYear(1))
    }

    @Test
    fun `finnStarttidspunktForUtbetalingstabell returnerer 1 januar året før for behandlingsårsak ÅRLIG_KONTROLL, selv om endringstidspunkt er senere`() {
        every { vedtaksperiodeService.finnEndringstidspunktForBehandling(any()) } returns LocalDate.of(2024, 1, 1)
        every { endretUtbetalingAndelRepository.findByBehandlingId(any()) } returns emptyList()
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.now().plusYears(1),
                    ),
                )

        val behandling = lagBehandling(årsak = BehandlingÅrsak.ÅRLIG_KONTROLL)

        val starttidspunkt = brevService.finnStarttidspunktForUtbetalingstabell(behandling)

        assertThat(starttidspunkt).isEqualTo(LocalDate.now(clockProvider.get()).minusYears(1).withDayOfYear(1))
    }

    @Test
    fun `finnStarttidspunktForUtbetalingstabell returnerer første utbetalingstidspunkt ved ÅRLIG_KONTROLL dersom endringstidspunkt er TIDENES_ENDE og første utbetaling er etter 1 januar i fjor`() {
        every { vedtaksperiodeService.finnEndringstidspunktForBehandling(any()) } returns TIDENES_ENDE
        every { endretUtbetalingAndelRepository.findByBehandlingId(any()) } returns emptyList()
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2024, 5),
                        tom = YearMonth.now().plusYears(1),
                    ),
                )

        val behandling = lagBehandling(årsak = BehandlingÅrsak.ÅRLIG_KONTROLL)

        val starttidspunkt = brevService.finnStarttidspunktForUtbetalingstabell(behandling)

        assertThat(starttidspunkt).isEqualTo(LocalDate.of(2024, 5, 1))
    }

    @Nested
    inner class HentBrevForTilbakekrevingsvedtakMotregningTest {
        @Test
        fun `Skal returnere brev med riktig genererte flettefelter`() {
            val behandling = lagBehandling()
            val arbeidsfordelingPåBehandling = lagArbeidsfordelingPåBehandling(behandlingId = behandling.id)
            val søker = lagPerson(type = PersonType.SØKER)
            val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandlingId = behandling.id, søker)

            val tilbakekrevingsvedtakMotregning =
                TilbakekrevingsvedtakMotregning(
                    behandling = behandling,
                    samtykke = true,
                    årsakTilFeilutbetaling = "Årsak til feilutbetaling",
                    vurderingAvSkyld = "Vurdering av skyld",
                    varselDato = LocalDate.of(2025, 5, 6),
                    heleBeløpetSkalKrevesTilbake = true,
                    vedtakPdf = null,
                )

            val avregningperioder =
                listOf(
                    AvregningPeriode(
                        fom = LocalDate.of(2025, 5, 6),
                        tom = LocalDate.of(2025, 6, 6),
                        totalEtterbetaling = BigDecimal.ZERO,
                        totalFeilutbetaling = BigDecimal.valueOf(300),
                    ),
                    AvregningPeriode(
                        fom = LocalDate.of(2025, 7, 6),
                        tom = LocalDate.of(2025, 8, 6),
                        totalEtterbetaling = BigDecimal.ZERO,
                        totalFeilutbetaling = BigDecimal.valueOf(300),
                    ),
                )

            every { mockPersongrunnlagService.hentAktivThrows(any()) } returns personopplysningGrunnlag
            every { mockTotrinnskontrollService.hentAktivForBehandling(behandlingId = behandling.id) } returns null
            every { mockArbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns arbeidsfordelingPåBehandling
            every { mockAvregningService.hentPerioderMedAvregning(behandlingId = behandling.id) } returns avregningperioder

            val brev = brevService.hentBrevForTilbakekrevingsvedtakMotregning(tilbakekrevingsvedtakMotregning)

            assertThat(brev.data.flettefelter.aarsakTilFeilutbetaling).isEqualTo(listOf("Årsak til feilutbetaling"))
            assertThat(brev.data.flettefelter.vurderingAvSkyld).isEqualTo(listOf("Vurdering av skyld"))
            assertThat(brev.data.flettefelter.sumAvFeilutbetaling).isEqualTo(listOf("600"))
            assertThat(brev.data.flettefelter.avregningperioder).isEqualTo(listOf("mai 2025 til og med juni 2025", "juli 2025 til og med august 2025"))
        }

        @Test
        fun `Skal returnere brev med riktig genererte flettefelter når periodene bare er på 1 måned`() {
            val behandling = lagBehandling()
            val arbeidsfordelingPåBehandling = lagArbeidsfordelingPåBehandling(behandlingId = behandling.id)
            val søker = lagPerson(type = PersonType.SØKER)
            val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandlingId = behandling.id, søker)

            val tilbakekrevingsvedtakMotregning =
                TilbakekrevingsvedtakMotregning(
                    behandling = behandling,
                    samtykke = true,
                    årsakTilFeilutbetaling = "Årsak til feilutbetaling",
                    vurderingAvSkyld = "Vurdering av skyld",
                    varselDato = LocalDate.of(2025, 5, 6),
                    heleBeløpetSkalKrevesTilbake = true,
                    vedtakPdf = null,
                )

            val avregningperioder =
                listOf(
                    AvregningPeriode(
                        fom = LocalDate.of(2025, 5, 6),
                        tom = LocalDate.of(2025, 5, 6),
                        totalEtterbetaling = BigDecimal.ZERO,
                        totalFeilutbetaling = BigDecimal.valueOf(300),
                    ),
                    AvregningPeriode(
                        fom = LocalDate.of(2025, 6, 6),
                        tom = LocalDate.of(2025, 6, 6),
                        totalEtterbetaling = BigDecimal.ZERO,
                        totalFeilutbetaling = BigDecimal.valueOf(300),
                    ),
                )

            every { mockPersongrunnlagService.hentAktivThrows(any()) } returns personopplysningGrunnlag
            every { mockTotrinnskontrollService.hentAktivForBehandling(behandlingId = behandling.id) } returns null
            every { mockArbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns arbeidsfordelingPåBehandling
            every { mockAvregningService.hentPerioderMedAvregning(behandlingId = behandling.id) } returns avregningperioder

            val brev = brevService.hentBrevForTilbakekrevingsvedtakMotregning(tilbakekrevingsvedtakMotregning)

            assertThat(brev.data.flettefelter.aarsakTilFeilutbetaling).isEqualTo(listOf("Årsak til feilutbetaling"))
            assertThat(brev.data.flettefelter.vurderingAvSkyld).isEqualTo(listOf("Vurdering av skyld"))
            assertThat(brev.data.flettefelter.sumAvFeilutbetaling).isEqualTo(listOf("600"))
            assertThat(brev.data.flettefelter.avregningperioder).isEqualTo(listOf("mai 2025", "juni 2025"))
        }
    }
}
