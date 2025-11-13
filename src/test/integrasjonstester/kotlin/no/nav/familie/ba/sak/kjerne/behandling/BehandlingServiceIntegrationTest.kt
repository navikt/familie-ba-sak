package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.nyOrdinærBehandling
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.lagMinimalUtbetalingsoppdragString
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

class BehandlingServiceIntegrationTest(
    @Autowired
    private val fagsakService: FagsakService,
    @Autowired
    private val behandlingService: BehandlingService,
    @Autowired
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired
    private val beregningService: BeregningService,
    @Autowired
    private val personidentService: PersonidentService,
    @Autowired
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    @Autowired
    private val behandlingRepository: BehandlingRepository,
    @Autowired
    private val stegService: StegService,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `Skal rulle tilbake behandling om noe feiler etter opprettelse`() {
        // Arrange
        val søkerFnr = "12345678901" // Ugyldig fnr for å trigge feil
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)

        // Act & Assert
        val error =
            assertThrows<IllegalStateException> {
                stegService.håndterNyBehandlingOgSendInfotrygdFeed(
                    nyOrdinærBehandling(
                        søkersIdent = søkerFnr,
                        fagsakId = fagsak.id,
                    ),
                )
            }
        assertThat(error.message).isEqualTo("12345678901")

        val behandlinger = behandlingRepository.finnBehandlinger(fagsakId = fagsak.id)
        assertThat(behandlinger).isEmpty()
    }

    @Test
    fun `Skal svare med behandling som er opprettet før X tid`() {
        // Arrange
        val fnr = randomFnr()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)

        // Act
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsak))

        // Assert
        val behandlingerSomErOpprettetFørIMorgen =
            behandlingRepository.finnÅpneBehandlinger(LocalDateTime.now().plusDays(1))
        assertThat(behandlingerSomErOpprettetFørIMorgen.map { it.id }).contains(behandling.id)
    }

    @Test
    fun `Skal hente forrige behandling`() {
        // Arrange
        val fnr = randomFnr()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val førstegangsbehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsak))

        tilkjentYtelseRepository.save(
            lagInitiellTilkjentYtelse(
                behandling = førstegangsbehandling,
                utbetalingsoppdrag = lagMinimalUtbetalingsoppdragString(behandlingId = førstegangsbehandling.id),
            ),
        )
        ferdigstillBehandling(førstegangsbehandling)

        // Act
        val revurderingInnvilgetBehandling =
            behandlingService.lagreNyOgDeaktiverGammelBehandling(
                lagBehandlingUtenId(
                    fagsak = fagsak,
                    behandlingType = BehandlingType.REVURDERING,
                ),
            )

        // Arrange
        val forrigeBehandling =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling = revurderingInnvilgetBehandling)
        assertThat(forrigeBehandling).isNotNull
        assertThat(forrigeBehandling?.id).isEqualTo(førstegangsbehandling.id)
    }

    @Test
    fun `Skal bare hente barn med tilkjentytelse for den aktuelle behandlingen`() {
        // Arrange
        val søker = randomFnr()
        val barn = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsak))

        tilkjentYtelseRepository.save(
            TilkjentYtelse(
                behandling = behandling,
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now(),
                andelerTilkjentYtelse = mutableSetOf(),
            ),
        )

        val barnAktør = personidentService.hentOgLagreAktørIder(listOf(barn), true)
        val testPersonopplysningsGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søker,
                barnasIdenter = listOf(barn),
                søkerAktør = fagsak.aktør,
                barnAktør = barnAktør,
            )
        personopplysningGrunnlagRepository.save(testPersonopplysningsGrunnlag)

        // Act & Assert
        assertThat(beregningService.finnBarnFraBehandlingMedTilkjentYtelse(behandlingId = behandling.id)).hasSize(0)
    }

    private fun ferdigstillBehandling(behandling: Behandling) {
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.AVSLUTTET)
        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
            behandling.id,
            StegType.BEHANDLING_AVSLUTTET,
        )
    }
}
