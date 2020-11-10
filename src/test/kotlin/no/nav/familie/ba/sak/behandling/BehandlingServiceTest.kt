package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-oauth", "mock-pdl", "mock-arbeidsfordeling")
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BehandlingServiceTest(
        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val behandlingService: BehandlingService,

        @Autowired
        private val tilkjentYtelseRepository: TilkjentYtelseRepository,

        @Autowired
        private val databaseCleanupService: DatabaseCleanupService
) {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal hente forrige behandling`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        tilkjentYtelseRepository.save(lagInitiellTilkjentYtelse(behandling).also {
            it.utbetalingsoppdrag = "Utbetalingsoppdrag()"
        })
        ferdigstillBehandling(behandling)

        val revurderingInnvilgetBehandling =
                behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak = fagsak,
                                                                                   behandlingType = BehandlingType.REVURDERING))

        val forrigeBehandling = behandlingService.hentForrigeBehandlingSomErIverksatt(
                fagsakId = revurderingInnvilgetBehandling.fagsak.id,
                behandlingFørFølgende = revurderingInnvilgetBehandling
        )
        Assertions.assertNotNull(forrigeBehandling)
        Assertions.assertEquals(behandling.id, forrigeBehandling?.id)
    }

    private fun ferdigstillBehandling(behandling: Behandling) {
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.AVSLUTTET)
        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(behandling.id, StegType.BEHANDLING_AVSLUTTET)
    }
}

