package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.steg.StegType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

@Tag("integration")
class BehandlingServiceTest(
    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,

    @Autowired
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,

    @Autowired
    private val databaseCleanupService: DatabaseCleanupService
) : AbstractSpringIntegrationTest() {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal hente forrige behandling`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        tilkjentYtelseRepository.save(
            lagInitiellTilkjentYtelse(behandling).also {
                it.utbetalingsoppdrag = "Utbetalingsoppdrag()"
            }
        )
        ferdigstillBehandling(behandling)

        val revurderingInnvilgetBehandling =
            behandlingService.lagreNyOgDeaktiverGammelBehandling(
                lagBehandling(
                    fagsak = fagsak,
                    behandlingType = BehandlingType.REVURDERING
                )
            )

        val forrigeBehandling =
            behandlingService.hentForrigeBehandlingSomErIverksatt(behandling = revurderingInnvilgetBehandling)
        Assertions.assertNotNull(forrigeBehandling)
        Assertions.assertEquals(behandling.id, forrigeBehandling?.id)
    }

    @Test
    fun `Skal bare hente barn med tilkjentytelse for den aktuelle behandlingen`() {
        val søker = randomFnr()
        val barn = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        tilkjentYtelseRepository.save(
            TilkjentYtelse(
                behandling = behandling,
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now(),
                andelerTilkjentYtelse = mutableSetOf()
            )
        )

        val testPersonopplysningsGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker,
            barnasIdenter = listOf(barn)
        )
        personopplysningGrunnlagRepository.save(testPersonopplysningsGrunnlag)

        Assertions.assertEquals(
            0,
            behandlingService.finnBarnFraBehandlingMedTilkjentYtsele(behandlingId = behandling.id).size
        )
    }

    private fun ferdigstillBehandling(behandling: Behandling) {
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.AVSLUTTET)
        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
            behandling.id,
            StegType.BEHANDLING_AVSLUTTET
        )
    }
}
