package no.nav.familie.ba.sak.kjerne.behandling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingMigreringsinfoRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

class BehandlingServiceEnhetstest {
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()

    private val behandlingstemaService: BehandlingstemaService = mockk()

    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService = mockk()

    private val behandlingMigreringsinfoRepository: BehandlingMigreringsinfoRepository = mockk()

    private val behandlingMetrikker: BehandlingMetrikker = mockk()

    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher = mockk()

    private val fagsakRepository: FagsakRepository = mockk()

    private val vedtakRepository: VedtakRepository = mockk()

    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository = mockk()

    private val loggService: LoggService = mockk()

    private val arbeidsfordelingService: ArbeidsfordelingService = mockk()

    private val infotrygdService: InfotrygdService = mockk()

    private val vedtaksperiodeService: VedtaksperiodeService = mockk()

    private val taskRepository: TaskRepositoryWrapper = mockk()

    private val vilkårsvurderingService: VilkårsvurderingService = mockk()

    private val unleashService: UnleashNextMedContextService = mockk()

    private val behandlingService: BehandlingService =
        BehandlingService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            behandlingstemaService = behandlingstemaService,
            behandlingSøknadsinfoService = behandlingSøknadsinfoService,
            behandlingMigreringsinfoRepository = behandlingMigreringsinfoRepository,
            behandlingMetrikker = behandlingMetrikker,
            saksstatistikkEventPublisher = saksstatistikkEventPublisher,
            fagsakRepository = fagsakRepository,
            vedtakRepository = vedtakRepository,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            loggService = loggService,
            arbeidsfordelingService = arbeidsfordelingService,
            infotrygdService = infotrygdService,
            vedtaksperiodeService = vedtaksperiodeService,
            taskRepository = taskRepository,
            vilkårsvurderingService = vilkårsvurderingService,
            unleashService = unleashService,
        )

    @Test
    fun `erLøpende - skal returnere true dersom det finnes andeler i en behandling hvor tom er etter YearMonth now`() {
        val behandling = lagBehandling()

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns
            listOf(
                lagAndelTilkjentYtelse(YearMonth.now().minusYears(1), YearMonth.now().minusMonths(6)),
                lagAndelTilkjentYtelse(YearMonth.now().minusMonths(6), YearMonth.now().minusMonths(3)),
                lagAndelTilkjentYtelse(YearMonth.now().minusMonths(3), YearMonth.now().plusMonths(3)),
            )
        assertThat(behandlingService.erLøpende(behandling)).isTrue
    }

    @Test
    fun `erLøpende - skal returnere false dersom det finnes andeler i en behandling hvor tom er det samme som YearMonth now`() {
        val behandling = lagBehandling()

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns
            listOf(
                lagAndelTilkjentYtelse(YearMonth.now().minusYears(1), YearMonth.now().minusMonths(6)),
                lagAndelTilkjentYtelse(YearMonth.now().minusMonths(6), YearMonth.now().minusMonths(3)),
                lagAndelTilkjentYtelse(YearMonth.now().minusMonths(3), YearMonth.now()),
            )
        assertThat(behandlingService.erLøpende(behandling)).isFalse
    }

    @Test
    fun `erLøpende - skal returnere false dersom alle andeler i en behandling har tom før YearMonth now`() {
        val behandling = lagBehandling()

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns
            listOf(
                lagAndelTilkjentYtelse(YearMonth.now().minusYears(1), YearMonth.now().minusMonths(6)),
                lagAndelTilkjentYtelse(YearMonth.now().minusMonths(6), YearMonth.now().minusMonths(3)),
                lagAndelTilkjentYtelse(YearMonth.now().minusMonths(3), YearMonth.now().minusMonths(1)),
            )
        assertThat(behandlingService.erLøpende(behandling)).isFalse
    }
}
