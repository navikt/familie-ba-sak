package no.nav.familie.ba.sak.task

import io.mockk.every
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FerdigstillBehandlingTaskTest : AbstractSpringIntegrationTest() {

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

    @Autowired
    lateinit var tilbakekrevingService: TilbakekrevingService

    @Autowired
    lateinit var vedtaksperiodeService: VedtaksperiodeService

    @BeforeEach
    fun init() {
        databaseCleanupService.truncate()
    }

    private fun initEnvServiceMock() {
        every {
            envService.skalIverksetteBehandling()
        } returns true
    }

    private fun kjørSteg(resultat: Resultat): Behandling {
        val fnr = randomFnr()
        val aktørId = randomAktørId()
        val fnrBarn = ClientMocks.barnFnr[0]

        val behandling = kjørStegprosessForFGB(
            tilSteg = if (resultat == Resultat.OPPFYLT) StegType.DISTRIBUER_VEDTAKSBREV else StegType.REGISTRERE_SØKNAD,
            søkerFnr = fnr,
            barnasIdenter = listOf(fnrBarn),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
        )

        return if (resultat == Resultat.IKKE_OPPFYLT) {
            val vilkårsvurdering = lagVilkårsvurdering(fnr, aktørId, behandling, resultat)

            vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
            val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandling)

            behandlingService.oppdaterStatusPåBehandling(
                behandlingEtterVilkårsvurdering.id,
                BehandlingStatus.IVERKSETTER_VEDTAK
            )
            behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
                behandlingId = behandlingEtterVilkårsvurdering.id,
                steg = StegType.FERDIGSTILLE_BEHANDLING
            )
        } else behandling
    }

    @Test
    fun `Skal ferdigstille behandling og fagsak blir til løpende`() {
        initEnvServiceMock()

        val behandling = kjørSteg(Resultat.OPPFYLT)

        val ferdigstiltBehandling = stegService.håndterFerdigstillBehandling(behandling)

        assertEquals(BehandlingStatus.AVSLUTTET, ferdigstiltBehandling.status)
        assertEquals(
            FagsakStatus.AVSLUTTET.name,
            saksstatistikkMellomlagringRepository.findByTypeAndTypeId(
                SaksstatistikkMellomlagringType.BEHANDLING,
                ferdigstiltBehandling.id
            )
                .last().jsonToBehandlingDVH().behandlingStatus
        )

        val ferdigstiltFagsak = ferdigstiltBehandling.fagsak
        assertEquals(FagsakStatus.LØPENDE, ferdigstiltFagsak.status)

        assertEquals(
            FagsakStatus.LØPENDE.name,
            saksstatistikkMellomlagringRepository.findByTypeAndTypeId(
                SaksstatistikkMellomlagringType.SAK,
                ferdigstiltFagsak.id
            )
                .last().jsonToSakDVH().sakStatus
        )
    }

    @Test
    fun `Skal ferdigstille behandling og sette fagsak til stanset`() {
        initEnvServiceMock()

        val behandling = kjørSteg(Resultat.IKKE_OPPFYLT)

        val ferdigstiltBehandling = stegService.håndterFerdigstillBehandling(behandling)
        assertEquals(BehandlingStatus.AVSLUTTET, ferdigstiltBehandling.status)

        val ferdigstiltFagsak = ferdigstiltBehandling.fagsak
        assertEquals(FagsakStatus.AVSLUTTET, ferdigstiltFagsak.status)
        assertEquals(
            FagsakStatus.AVSLUTTET.name,
            saksstatistikkMellomlagringRepository.findByTypeAndTypeId(
                SaksstatistikkMellomlagringType.SAK,
                ferdigstiltFagsak.id
            )
                .last().jsonToSakDVH().sakStatus
        )
    }
}
