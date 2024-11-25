package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.sikkerhet.SaksbehandlerContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth

class BrevServiceTest {
    val saksbehandlerContext = mockk<SaksbehandlerContext>()
    val brevmalService = mockk<BrevmalService>()
    val unleashService = mockk<UnleashNextMedContextService>()
    val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    val endretUtbetalingAndelRepository = mockk<EndretUtbetalingAndelRepository>()
    val vedtaksbrevFellesfelterService = mockk<VedtaksbrevFellesfelterService>()
    val opprettGrunnlagOgSignaturDataService = mockk<OpprettGrunnlagOgSignaturDataService>()
    val starttidspunktForUtbetalingstabellService = mockk<StarttidspunktForUtbetalingstabellService>()

    val brevService =
        BrevService(
            simuleringService = mockk(),
            vedtaksperiodeService = vedtaksperiodeService,
            korrigertEtterbetalingService = mockk(),
            organisasjonService = mockk(),
            korrigertVedtakService = mockk(),
            brevmalService = brevmalService,
            integrasjonClient = mockk(),
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            utenlandskPeriodebeløpRepository = mockk(),
            valutakursRepository = mockk(),
            kompetanseRepository = mockk(),
            endretUtbetalingAndelRepository = endretUtbetalingAndelRepository,
            vedtaksbrevFellesfelterService = vedtaksbrevFellesfelterService,
            opprettGrunnlagOgSignaturDataService = opprettGrunnlagOgSignaturDataService,
            starttidspunktForUtbetalingstabellService = starttidspunktForUtbetalingstabellService,
        )

    @BeforeEach
    fun setUp() {
        every { saksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "saksbehandlerNavn"
        every { unleashService.isEnabled(any()) } returns true
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
}
