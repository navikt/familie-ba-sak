package no.nav.familie.ba.sak.task

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.MockPersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.datagenerator.randomBarnFødselsdato
import no.nav.familie.ba.sak.datagenerator.randomSøkerFødselsdato
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.AutovedtakSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.SettPåMaskinellVentÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentÅrsak
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.overgangsstønad.OvergangsstønadService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.småbarnstillegg.SmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjørbehandling.kjørStegprosessForFGB
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class FerdigstillBehandlingTaskTest(
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val persongrunnlagService: PersongrunnlagService,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val stegService: StegService,
    @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired private val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val brevmalService: BrevmalService,
    @Autowired private val snikeIKøenService: SnikeIKøenService,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val overgangsstønadService: OvergangsstønadService,
    @Autowired private val småbarnstilleggService: SmåbarnstilleggService,
    @Autowired private val taskService: TaskService,
    @Autowired private val autovedtakService: AutovedtakService,
    @Autowired private val oppgaveService: OppgaveService,
    @Autowired private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    @Autowired private val settPåVentService: SettPåVentService,
    @Autowired private val clockProvider: ClockProvider,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `Skal ferdigstille behandling og fagsak blir til løpende`() {
        // Arrange
        val (søkerFnr, barnasIdenter) = lagSøkerOgBarn()
        val behandling = kjørSteg(søkerFnr = søkerFnr, barnasIdenter = barnasIdenter, resultat = Resultat.OPPFYLT)

        // Act
        val ferdigstiltBehandling = stegService.håndterFerdigstillBehandling(behandling)

        // Assert
        assertEquals(BehandlingStatus.AVSLUTTET, ferdigstiltBehandling.status)
        assertEquals(
            FagsakStatus.AVSLUTTET.name,
            saksstatistikkMellomlagringRepository
                .findByTypeAndTypeId(
                    SaksstatistikkMellomlagringType.BEHANDLING,
                    ferdigstiltBehandling.id,
                ).last()
                .jsonToBehandlingDVH()
                .behandlingStatus,
        )

        val ferdigstiltFagsak = ferdigstiltBehandling.fagsak
        assertEquals(FagsakStatus.LØPENDE, ferdigstiltFagsak.status)
        assertEquals(
            FagsakStatus.LØPENDE.name,
            saksstatistikkMellomlagringRepository
                .findByTypeAndTypeId(
                    SaksstatistikkMellomlagringType.SAK,
                    ferdigstiltFagsak.id,
                ).last()
                .jsonToSakDVH()
                .sakStatus,
        )
    }

    @Test
    fun `Skal ferdigstille behandling og sette fagsak til stanset`() {
        // Arrange
        val (søkerFnr, barnasIdenter) = lagSøkerOgBarn()
        val behandling = kjørSteg(søkerFnr = søkerFnr, barnasIdenter = barnasIdenter, resultat = Resultat.IKKE_OPPFYLT)

        // Act
        val ferdigstiltBehandling = stegService.håndterFerdigstillBehandling(behandling)

        // Assert
        assertEquals(BehandlingStatus.AVSLUTTET, ferdigstiltBehandling.status)

        val ferdigstiltFagsak = ferdigstiltBehandling.fagsak
        assertEquals(FagsakStatus.AVSLUTTET, ferdigstiltFagsak.status)
        assertEquals(
            FagsakStatus.AVSLUTTET.name,
            saksstatistikkMellomlagringRepository
                .findByTypeAndTypeId(
                    SaksstatistikkMellomlagringType.SAK,
                    ferdigstiltFagsak.id,
                ).last()
                .jsonToSakDVH()
                .sakStatus,
        )
    }

    @Nested
    inner class BehandlingPåMaskinellVent {
        private val autovedtakSmåbarnstilleggService =
            AutovedtakSmåbarnstilleggService(
                behandlingService = behandlingService,
                fagsakService = fagsakService,
                behandlingHentOgPersisterService = behandlingHentOgPersisterService,
                vedtakService = vedtakService,
                vedtaksperiodeService = vedtaksperiodeService,
                overgangsstønadService = overgangsstønadService,
                taskService = taskService,
                autovedtakService = autovedtakService,
                oppgaveService = oppgaveService,
                vedtaksperiodeHentOgPersisterService = vedtaksperiodeHentOgPersisterService,
                clockProvider = clockProvider,
                påVentService = settPåVentService,
                stegService = stegService,
                småbarnstilleggService = småbarnstilleggService,
            )

        @Test
        fun `skal henlegge behandling hvis vi ikke kan behandle automatisk`() {
            // Arrange
            val (søkerFnr, barnasIdenter) = lagSøkerOgBarn()
            val opprinneligÅpenBehandling = opprettBehandling(søkerFnr = søkerFnr, status = BehandlingStatus.UTREDES)
            settPåMaskinellVent(opprinneligÅpenBehandling)

            // Act
            val automatiskBehandling = kjørSteg(søkerFnr = søkerFnr, barnasIdenter = barnasIdenter, resultat = Resultat.IKKE_OPPFYLT)
            autovedtakSmåbarnstilleggService.kanIkkeBehandleAutomatisk(automatiskBehandling, Metrics.counter("test"), meldingIOppgave = "test")

            // Assert
            val automatiskBehandlingEtterHenleggelse = behandlingRepository.finnBehandling(automatiskBehandling.id)
            assertThat(automatiskBehandlingEtterHenleggelse.resultat).isEqualTo(Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE)
            assertThat(automatiskBehandlingEtterHenleggelse.aktiv).isFalse()

            val opprinneligBehandlingEtterHenleggelse = behandlingRepository.finnBehandling(opprinneligÅpenBehandling.id)
            assertThat(opprinneligBehandlingEtterHenleggelse.aktiv).isTrue()
            assertThat(opprinneligBehandlingEtterHenleggelse.status).isEqualTo(BehandlingStatus.UTREDES)
        }

        @Test
        fun `skal henlegge behandling og sette behandling tilbake til på vent hvis den var på vent i utgangspunktet`() {
            // Arrange
            val (søkerFnr, barnasIdenter) = lagSøkerOgBarn()
            val opprinneligÅpenBehandling = opprettBehandling(søkerFnr = søkerFnr, status = BehandlingStatus.UTREDES)
            settPåVentService.settBehandlingPåVent(opprinneligÅpenBehandling.id, LocalDate.now().plusMonths(1), SettPåVentÅrsak.AVVENTER_DOKUMENTASJON)
            settPåMaskinellVent(opprinneligÅpenBehandling)

            // Act
            val automatiskBehandling = kjørSteg(søkerFnr = søkerFnr, barnasIdenter = barnasIdenter, resultat = Resultat.IKKE_OPPFYLT)
            val meldingIOppgave = autovedtakSmåbarnstilleggService.kanIkkeBehandleAutomatisk(automatiskBehandling, Metrics.counter("test"), meldingIOppgave = "test")

            // Assert
            assertThat(meldingIOppgave).isEqualTo("test")

            val automatiskBehandlingEtterHenleggelse = behandlingRepository.finnBehandling(automatiskBehandling.id)
            assertThat(automatiskBehandlingEtterHenleggelse.resultat).isEqualTo(Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE)
            assertThat(automatiskBehandlingEtterHenleggelse.aktiv).isFalse()

            val opprinneligBehandlingEtterHenleggelse = behandlingRepository.finnBehandling(opprinneligÅpenBehandling.id)
            assertThat(opprinneligBehandlingEtterHenleggelse.aktiv).isTrue()
            assertThat(opprinneligBehandlingEtterHenleggelse.status).isEqualTo(BehandlingStatus.SATT_PÅ_VENT)
        }

        @Test
        fun `skal reaktivere en behandling som er på maskinell vent`() {
            // Arrange
            val (søkerFnr, barnasIdenter) = lagSøkerOgBarn()
            val behandling1 = opprettBehandling(søkerFnr = søkerFnr, status = BehandlingStatus.UTREDES)
            settPåMaskinellVent(behandling1)

            // Act
            val behandling2 = kjørSteg(søkerFnr = søkerFnr, barnasIdenter = barnasIdenter, resultat = Resultat.OPPFYLT)
            stegService.håndterFerdigstillBehandling(behandling2)

            // Assert
            assertThat(behandlingRepository.finnBehandling(behandling2.id).aktiv).isFalse()
            assertThat(behandlingRepository.finnBehandling(behandling1.id).aktiv).isTrue()
        }

        @Test
        fun `skal reaktivere en behandling etter ferdigstilling av henlagt behandling`() {
            // Arrange
            val (søkerFnr, barnasIdenter) = lagSøkerOgBarn()
            val behandling1 = opprettBehandling(søkerFnr = søkerFnr, status = BehandlingStatus.UTREDES)
            settPåMaskinellVent(behandling1)

            // Act
            val behandling2 = kjørSteg(søkerFnr = søkerFnr, barnasIdenter = barnasIdenter, resultat = Resultat.OPPFYLT)
            behandling2.resultat = Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET
            stegService.håndterFerdigstillBehandling(behandling2)

            // Assert
            assertThat(behandlingRepository.finnBehandling(behandling2.id).aktiv).isFalse()
            assertThat(behandlingRepository.finnBehandling(behandling1.id).aktiv).isTrue()
        }

        @Test
        fun `skal reaktivere en behandling etter ferdigstilling av henlagt behandling som har en tidligere iverksatt behandling`() {
            // Arrange
            val (søkerFnr, barnasIdenter) = lagSøkerOgBarn()
            val behandling = kjørSteg(søkerFnr = søkerFnr, barnasIdenter = barnasIdenter, resultat = Resultat.OPPFYLT)
            stegService.håndterFerdigstillBehandling(behandling)
            assertThat(behandlingRepository.finnBehandling(behandling.id).aktiv).isTrue()

            val behandling2 = opprettBehandling(søkerFnr = søkerFnr, status = BehandlingStatus.UTREDES)
            settPåMaskinellVent(behandling2)

            val behandling3 =
                opprettBehandling(
                    søkerFnr = søkerFnr,
                    status = BehandlingStatus.FATTER_VEDTAK,
                    resultat = Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET,
                )

            // Act
            stegService.håndterFerdigstillBehandling(behandling3)

            // Assert
            assertThat(behandlingRepository.finnBehandling(behandling2.id).aktiv).isTrue()
        }

        private fun opprettBehandling(
            søkerFnr: String,
            status: BehandlingStatus = BehandlingStatus.IVERKSETTER_VEDTAK,
            resultat: Behandlingsresultat = Behandlingsresultat.INNVILGET,
        ): Behandling {
            val behandling =
                Behandling(
                    fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr),
                    opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                    type = BehandlingType.REVURDERING,
                    kategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    status = status,
                    resultat = resultat,
                ).initBehandlingStegTilstand()
            val ferdigstillSteg =
                BehandlingStegTilstand(
                    behandling = behandling,
                    behandlingSteg = StegType.FERDIGSTILLE_BEHANDLING,
                )
            behandling.behandlingStegTilstand.add(ferdigstillSteg)
            return behandlingService.lagreNyOgDeaktiverGammelBehandling(behandling)
        }

        private fun settPåMaskinellVent(behandling: Behandling) {
            snikeIKøenService.settAktivBehandlingPåMaskinellVent(
                behandling.id,
                SettPåMaskinellVentÅrsak.SATSENDRING,
            )
        }
    }

    private fun kjørSteg(
        søkerFnr: String,
        barnasIdenter: List<String>,
        resultat: Resultat,
    ): Behandling {
        val behandling =
            kjørStegprosessForFGB(
                tilSteg = if (resultat == Resultat.OPPFYLT) StegType.DISTRIBUER_VEDTAKSBREV else StegType.REGISTRERE_SØKNAD,
                søkerFnr = søkerFnr,
                barnasIdenter = barnasIdenter,
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
            )

        return if (resultat == Resultat.IKKE_OPPFYLT) {
            val vilkårsvurdering =
                vilkårsvurderingService.hentAktivForBehandling(behandling.id)!!.kopier().apply {
                    personResultater.first { it.erSøkersResultater() }.vilkårResultater.forEach { it.resultat = Resultat.IKKE_OPPFYLT }
                }

            vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
            val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandling)

            behandlingService.oppdaterStatusPåBehandling(
                behandlingEtterVilkårsvurdering.id,
                BehandlingStatus.IVERKSETTER_VEDTAK,
            )
            behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
                behandlingId = behandlingEtterVilkårsvurdering.id,
                steg = StegType.FERDIGSTILLE_BEHANDLING,
            )
        } else {
            behandling
        }
    }

    private fun lagSøkerOgBarn(): Pair<String, List<String>> {
        val søkerFnr = leggTilPersonInfo(fødselsdato = randomSøkerFødselsdato())
        val barnFnr = leggTilPersonInfo(fødselsdato = randomBarnFødselsdato())
        return Pair(søkerFnr, listOf(barnFnr))
    }
}
