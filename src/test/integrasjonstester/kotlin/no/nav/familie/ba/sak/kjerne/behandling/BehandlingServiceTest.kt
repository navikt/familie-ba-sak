package no.nav.familie.ba.sak.kjerne.behandling

import lagBehandling
import lagInitiellTilkjentYtelse
import lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
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
import nyOrdinærBehandling
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.client.HttpClientErrorException
import randomFnr
import java.time.LocalDate
import java.time.LocalDateTime

@Tag("integration")
class BehandlingServiceTest(
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
    private val databaseCleanupService: DatabaseCleanupService,
    @Autowired
    private val behandlingRepository: BehandlingRepository,
    @Autowired
    private val stegService: StegService,
) : AbstractSpringIntegrationTest() {
    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal rulle tilbake behandling om noe feiler etter opprettelse`() {
        databaseCleanupService.truncate()

        val forventetFeilmelding = "404 Fant ikke forespurte data på person."

        val fnr = "00000000000"

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val error =
            assertThrows<HttpClientErrorException> {
                stegService.håndterNyBehandlingOgSendInfotrygdFeed(
                    nyOrdinærBehandling(
                        søkersIdent = fnr,
                        fagsakId = fagsak.id,
                    ),
                )
            }

        assertEquals(forventetFeilmelding, error.message)

        val behandlinger = behandlingRepository.finnBehandlinger(fagsakId = fagsak.id)
        assertEquals(0, behandlinger.size)
    }

    @Test
    fun `Skal svare med behandling som er opprettet før X tid`() {
        databaseCleanupService.truncate()

        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val behandlingerSomErOpprettetFørIMorgen =
            behandlingRepository.finnÅpneBehandlinger(LocalDateTime.now().plusDays(1))

        assertEquals(1, behandlingerSomErOpprettetFørIMorgen.size)
        assertEquals(behandling.id, behandlingerSomErOpprettetFørIMorgen.single().id)
    }

    @Test
    fun `Skal hente forrige behandling`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        tilkjentYtelseRepository.save(
            lagInitiellTilkjentYtelse(behandling).also {
                it.utbetalingsoppdrag = "Utbetalingsoppdrag()"
            },
        )
        ferdigstillBehandling(behandling)

        val revurderingInnvilgetBehandling =
            behandlingService.lagreNyOgDeaktiverGammelBehandling(
                lagBehandling(
                    fagsak = fagsak,
                    behandlingType = BehandlingType.REVURDERING,
                ),
            )

        val forrigeBehandling =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling = revurderingInnvilgetBehandling)
        Assertions.assertNotNull(forrigeBehandling)
        assertEquals(behandling.id, forrigeBehandling?.id)
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

        assertEquals(
            0,
            beregningService.finnBarnFraBehandlingMedTilkjentYtelse(behandlingId = behandling.id).size,
        )
    }

    private fun ferdigstillBehandling(behandling: Behandling) {
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.AVSLUTTET)
        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
            behandling.id,
            StegType.BEHANDLING_AVSLUTTET,
        )
    }
}
