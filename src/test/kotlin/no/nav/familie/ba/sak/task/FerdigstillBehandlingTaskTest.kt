package no.nav.familie.ba.sak.task

import io.mockk.every
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.saksstatistikk.domene.SaksstatistikkMellomlagringType
import no.nav.familie.ba.sak.task.dto.FerdigstillBehandlingDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-brev-klient", "mock-pdl", "mock-økonomi", "mock-infotrygd-feed", "mock-arbeidsfordeling")
@Tag("integration")
class FerdigstillBehandlingTaskTest {

    @Autowired
    private lateinit var ferdigstillBehandlingTask: FerdigstillBehandlingTask

    @Autowired
    private lateinit var vedtakService: VedtakService

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var persongrunnlagService: PersongrunnlagService

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var stegService: StegService

    @Autowired
    lateinit var vilkårsvurderingService: VilkårsvurderingService

    @Autowired
    lateinit var databaseCleanupService: DatabaseCleanupService

    @Autowired
    lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    lateinit var saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository

    @Autowired
    lateinit var envService: EnvService

    @BeforeEach
    fun init() {
        databaseCleanupService.truncate()
    }

    private fun initEnvServiceMock() {
        every {
            envService.skalIverksetteBehandling()
        } returns true
    }

    private fun lagTestTask(resultat: Resultat): Task {
        val fnr = randomFnr()
        val fnrBarn = ClientMocks.barnFnr[0]

        val behandling = kjørStegprosessForFGB(
                tilSteg = if (resultat == Resultat.OPPFYLT) StegType.DISTRIBUER_VEDTAKSBREV else StegType.REGISTRERE_SØKNAD,
                søkerFnr = fnr,
                barnasIdenter = listOf(fnrBarn),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService
        )

        val behandlingEtterVilkårsvurdering = if (resultat == Resultat.IKKE_OPPFYLT) {
            val vilkårsvurdering = lagVilkårsvurdering(fnr, behandling, resultat)

            vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
            val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandling)


            behandlingService.oppdaterStatusPåBehandling(behandlingEtterVilkårsvurdering.id, BehandlingStatus.IVERKSETTER_VEDTAK)
            behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(behandlingId = behandlingEtterVilkårsvurdering.id,
                                                                                  steg = StegType.FERDIGSTILLE_BEHANDLING)

            behandlingEtterVilkårsvurdering
        } else behandling

        return FerdigstillBehandlingTask.opprettTask(personIdent = fnr, behandlingsId = behandlingEtterVilkårsvurdering.id)
    }


    @Test
    fun `Skal ferdigstille behandling og fagsak blir til løpende`() {
        initEnvServiceMock()

        val testTask = lagTestTask(Resultat.OPPFYLT)

        val ferdigstillBehandlingDTO = objectMapper.readValue(testTask.payload, FerdigstillBehandlingDTO::class.java)

        ferdigstillBehandlingTask.doTask(testTask)
        ferdigstillBehandlingTask.onCompletion(testTask)

        val ferdigstiltBehandling = behandlingService.hent(behandlingId = ferdigstillBehandlingDTO.behandlingsId)
        assertEquals(BehandlingStatus.AVSLUTTET, ferdigstiltBehandling.status)
        assertEquals(FagsakStatus.AVSLUTTET.name,
                     saksstatistikkMellomlagringRepository.findByTypeAndTypeId(SaksstatistikkMellomlagringType.BEHANDLING,
                                                                               ferdigstiltBehandling.id)
                             .last().jsonToBehandlingDVH().behandlingStatus
        )

        val ferdigstiltFagsak = ferdigstiltBehandling.fagsak
        assertEquals(FagsakStatus.LØPENDE, ferdigstiltFagsak.status)

        assertEquals(FagsakStatus.LØPENDE.name,
                     saksstatistikkMellomlagringRepository.findByTypeAndTypeId(SaksstatistikkMellomlagringType.SAK,
                                                                               ferdigstiltFagsak.id)
                             .last().jsonToSakDVH().sakStatus
        )
    }

    @Test
    fun `Skal ferdigstille behandling og sette fagsak til stanset`() {
        initEnvServiceMock()

        val testTask = lagTestTask(Resultat.IKKE_OPPFYLT)

        val ferdigstillBehandlingDTO = objectMapper.readValue(testTask.payload, FerdigstillBehandlingDTO::class.java)

        ferdigstillBehandlingTask.doTask(testTask)
        ferdigstillBehandlingTask.onCompletion(testTask)

        val ferdigstiltBehandling = behandlingService.hent(behandlingId = ferdigstillBehandlingDTO.behandlingsId)
        assertEquals(BehandlingStatus.AVSLUTTET, ferdigstiltBehandling.status)

        val ferdigstiltFagsak = ferdigstiltBehandling.fagsak
        assertEquals(FagsakStatus.AVSLUTTET, ferdigstiltFagsak.status)
        assertEquals(FagsakStatus.AVSLUTTET.name,
                     saksstatistikkMellomlagringRepository.findByTypeAndTypeId(SaksstatistikkMellomlagringType.SAK,
                                                                               ferdigstiltFagsak.id)
                             .last().jsonToSakDVH().sakStatus
        )
    }
}