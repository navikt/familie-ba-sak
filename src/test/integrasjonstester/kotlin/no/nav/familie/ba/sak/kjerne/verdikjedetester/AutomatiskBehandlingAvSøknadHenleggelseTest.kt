package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.datagenerator.lagBarnetrygdSøknadV9
import no.nav.familie.ba.sak.fake.FakeIntegrasjonKlient
import no.nav.familie.ba.sak.fake.FakeTaskRepositoryWrapper
import no.nav.familie.ba.sak.fake.tilPayload
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.Søknadsinfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.ScenarioDto
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.ScenarioPersonDto
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.stubScenario
import no.nav.familie.ba.sak.task.BehandleAutomatiskSøknadTask
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.ba.sak.task.dto.BehandleAutomatiskSøknadTaskDTO
import no.nav.familie.ba.sak.task.dto.OpprettOppgaveTaskDTO
import no.nav.familie.kontrakter.ba.søknad.VersjonertBarnetrygdSøknadV9
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class AutomatiskBehandlingAvSøknadHenleggelseTest(
    @Autowired private val behandleAutomatiskSøknadTask: BehandleAutomatiskSøknadTask,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val fakeIntegrasjonKlient: FakeIntegrasjonKlient,
    @Autowired private val fakeTaskRepositoryWrapper: FakeTaskRepositoryWrapper,
) : AbstractVerdikjedetest() {
    @BeforeEach
    fun førHverTest() {
        System.setProperty(FeatureToggle.SKAL_BEHANDLE_SOKNAD_AUTOMATISK.navn, "true")
    }

    @AfterEach
    fun etterHverTest() {
        System.clearProperty(FeatureToggle.SKAL_BEHANDLE_SOKNAD_AUTOMATISK.navn)
    }

    @Test
    fun `skal opprette én BehandleSak-oppgave med filtreringsreglene som ikke er oppfylt når filtreringsreglene stopper automatisk behandling av søknad`() {
        // Arrange
        val nyBehandling = opprettFagsakMedSøknad(søknadInneholderVedlegg = true)

        // Act
        val behandlinger = behandleAutomatiskSøknad(nyBehandling)

        // Assert
        val manuellBehandling = behandlinger.single { !it.skalBehandlesAutomatisk }
        assertThat(behandlinger.single { it.skalBehandlesAutomatisk }.resultat).isEqualTo(Behandlingsresultat.HENLAGT_AUTOMATISK_SØKNAD)
        assertThat(manuellBehandling.opprettetÅrsak).isEqualTo(BehandlingÅrsak.SØKNAD)

        val behandleSakOppgaver = hentBehandleSakOppgaver(behandlinger)
        assertThat(behandleSakOppgaver).hasSize(1)
        val behandleSakOppgave = behandleSakOppgaver.single()
        assertThat(behandleSakOppgave.behandlingId).isEqualTo(manuellBehandling.id)
        assertThat(behandleSakOppgave.beskrivelse).isEqualTo("Søknaden inneholder vedlegg")
    }

    @Test
    fun `skal opprette én BehandleSak-oppgave med årsaken når automatisk behandling av søknad må behandles manuelt`() {
        // Arrange
        // Standardsimuleringen i FakeOppdragBackendKlient gir feilutbetaling, så søknaden må behandles manuelt
        val nyBehandling = opprettFagsakMedSøknad(søknadInneholderVedlegg = false)

        // Act
        val behandlinger = behandleAutomatiskSøknad(nyBehandling)

        // Assert
        val manuellBehandling = behandlinger.single { !it.skalBehandlesAutomatisk }
        assertThat(behandlinger.single { it.skalBehandlesAutomatisk }.resultat).isEqualTo(Behandlingsresultat.HENLAGT_AUTOMATISK_SØKNAD)

        val behandleSakOppgaver = hentBehandleSakOppgaver(behandlinger)
        assertThat(behandleSakOppgaver).hasSize(1)
        val behandleSakOppgave = behandleSakOppgaver.single()
        assertThat(behandleSakOppgave.behandlingId).isEqualTo(manuellBehandling.id)
        assertThat(behandleSakOppgave.beskrivelse).isEqualTo("Automatisk behandling av søknad fører til feilutbetaling.\nBehandling av søknad må håndteres manuelt.")
    }

    private fun opprettFagsakMedSøknad(søknadInneholderVedlegg: Boolean): NyBehandling {
        val scenario =
            ScenarioDto(
                søker = ScenarioPersonDto(fødselsdato = "1996-12-12", fornavn = "Mor", etternavn = "Søker"),
                barna =
                    listOf(
                        ScenarioPersonDto(
                            fødselsdato = LocalDate.now().minusMonths(6).toString(),
                            fornavn = "Barn",
                            etternavn = "Barnesen",
                        ),
                    ),
            ).also { stubScenario(it) }
        val barnasIdenter = scenario.barna.map { it.ident }
        val fagsakId = familieBaSakKlient().opprettFagsak(søkersIdent = scenario.søker.ident).data!!.id
        val journalpostId = "NAV31766-$fagsakId"

        fakeIntegrasjonKlient.leggTilVersjonertBarnetrygdSøknad(
            journalpostId = journalpostId,
            versjonertBarnetrygdSøknad =
                VersjonertBarnetrygdSøknadV9(
                    lagBarnetrygdSøknadV9(søkerFnr = scenario.søker.ident, barnFnr = barnasIdenter, inneholderVedlegg = søknadInneholderVedlegg),
                ),
        )

        return NyBehandling(
            kategori = BehandlingKategori.NASJONAL,
            underkategori = BehandlingUnderkategori.ORDINÆR,
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            behandlingÅrsak = BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD,
            barnasIdenter = barnasIdenter,
            søknadMottattDato = LocalDate.now(),
            søknadsinfo = Søknadsinfo(journalpostId = journalpostId, brevkode = "NAV 33-00.07", erDigital = true),
            fagsakId = fagsakId,
        )
    }

    private fun behandleAutomatiskSøknad(nyBehandling: NyBehandling): List<Behandling> {
        behandleAutomatiskSøknadTask.doTask(BehandleAutomatiskSøknadTask.opprettTask(BehandleAutomatiskSøknadTaskDTO(nyBehandling)))

        return behandlingHentOgPersisterService.hentBehandlinger(nyBehandling.fagsakId)
    }

    private fun hentBehandleSakOppgaver(behandlinger: List<Behandling>): List<OpprettOppgaveTaskDTO> =
        fakeTaskRepositoryWrapper
            .hentLagredeTaskerAvType(OpprettOppgaveTask.TASK_STEP_TYPE)
            .tilPayload<OpprettOppgaveTaskDTO>()
            .filter { it.oppgavetype == Oppgavetype.BehandleSak && it.behandlingId in behandlinger.map { behandling -> behandling.id } }
}
