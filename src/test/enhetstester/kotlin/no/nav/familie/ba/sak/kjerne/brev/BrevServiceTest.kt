package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.sikkerhet.SaksbehandlerContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.YearMonth

class BrevServiceTest {
    val saksbehandlerContext = mockk<SaksbehandlerContext>()
    val brevmalService = mockk<BrevmalService>()
    val unleashService = mockk<UnleashNextMedContextService>()
    val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    val endretUtbetalingAndelRepository = mockk<EndretUtbetalingAndelRepository>()

    val brevService =
        BrevService(
            totrinnskontrollService = mockk(),
            persongrunnlagService = mockk(),
            arbeidsfordelingService = mockk(),
            simuleringService = mockk(),
            vedtaksperiodeService = vedtaksperiodeService,
            sanityService = mockk(),
            vilkårsvurderingService = mockk(),
            korrigertEtterbetalingService = mockk(),
            organisasjonService = mockk(),
            korrigertVedtakService = mockk(),
            saksbehandlerContext = saksbehandlerContext,
            brevmalService = brevmalService,
            refusjonEøsRepository = mockk(),
            integrasjonClient = mockk(),
            testVerktøyService = mockk(),
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            utenlandskPeriodebeløpRepository = mockk(),
            valutakursRepository = mockk(),
            kompetanseRepository = mockk(),
            endretUtbetalingAndelRepository = endretUtbetalingAndelRepository,
        )

    @BeforeEach
    fun setUp() {
        every { saksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "saksbehandlerNavn"
        every { unleashService.isEnabled(any()) } returns true
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
    fun `finnStarttidspunktForUtbetalingstabell returnerer endringstidspunkt for behandlingsårsak ÅRLIG_KONTROLL, dersom endringstidspunkt er tidligere enn første januar i fjor`() {
        every { vedtaksperiodeService.finnEndringstidspunktForBehandling(any()) } returns LocalDate.of(2020, 1, 1)

        val behandling = lagBehandling(årsak = BehandlingÅrsak.ÅRLIG_KONTROLL)

        val starttidspunkt = brevService.finnStarttidspunktForUtbetalingstabell(behandling)

        assertThat(starttidspunkt).isEqualTo(LocalDate.of(2020, 1, 1))
    }

    @Test
    fun `finnStarttidspunktForUtbetalingstabell returnerer tidligst første januar i fjor for behandlingsårsak ÅRLIG_KONTROLL, dersom endringstidspunkt er TIDENES_ENDE`() {
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

        assertThat(starttidspunkt).isEqualTo(LocalDate.now().minusYears(1).withDayOfYear(1))
    }

    @Test
    fun `finnStarttidspunktForUtbetalingstabell returnerer første januar i fjor for behandlingsårsak ÅRLIG_KONTROLL, selv om endringstidspunkt er senere`() {
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

        assertThat(starttidspunkt).isEqualTo(LocalDate.now().minusYears(1).withDayOfYear(1))
    }

    @Test
    fun `finnStarttidspunktForUtbetalingstabell returnerer første utbetalingstidspunkt hvis endringstidspunkt er TIDENES_ENDE`() {
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
}
