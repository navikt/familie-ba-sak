package no.nav.familie.ba.sak.kjerne.behandling.settpåvent

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.config.MockPersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.datagenerator.randomBarnFnr
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.SettPåMaskinellVentÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.forenklettilbakekrevingsvedtak.ForenkletTilbakekrevingsvedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjørbehandling.kjørStegprosessForFGB
import no.nav.familie.ba.sak.task.TaBehandlingerEtterVentefristAvVentTask
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate

@Tag("integration")
class SettPåVentServiceTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val databaseCleanupService: DatabaseCleanupService,
    @Autowired private val stegService: StegService,
    @Autowired private val settPåVentService: SettPåVentService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val persongrunnlagService: PersongrunnlagService,
    @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val settPåVentRepository: SettPåVentRepository,
    @Autowired private val taBehandlingerEtterVentefristAvVentTask: TaBehandlingerEtterVentefristAvVentTask,
    @Autowired private val brevmalService: BrevmalService,
    @Autowired private val snikeIKøenService: SnikeIKøenService,
    @Autowired private val forenkletTilbakekrevingsvedtakService: ForenkletTilbakekrevingsvedtakService,
) : AbstractSpringIntegrationTest() {
    private val barnFnr = leggTilPersonInfo(randomBarnFnr())
    private val frist = LocalDate.now().plusDays(DAGER_FRIST_FOR_AVVENTER_SAMTYKKE_ULOVFESTET_MOTREGNING)

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @ParameterizedTest
    @EnumSource(value = SettPåVentÅrsak::class)
    fun `Kan sette en behandling på vent hvis statusen er utredes`(årsak: SettPåVentÅrsak) {
        val behandling = opprettBehandling()

        val settBehandlingPåVent =
            settPåVentService.settBehandlingPåVent(
                behandling.id,
                frist,
                årsak,
            )

        assertThat(settBehandlingPåVent.behandling.id).isEqualTo(behandling.id)
        assertThat(settBehandlingPåVent.frist).isEqualTo(frist)
        assertThat(settBehandlingPåVent.årsak).isEqualTo(årsak)
        assertThat(settBehandlingPåVent.aktiv).isTrue()

        assertThat(behandlingRepository.finnBehandling(behandling.id).status).isEqualTo(BehandlingStatus.SATT_PÅ_VENT)
    }

    @ParameterizedTest
    @EnumSource(value = SettPåVentÅrsak::class)
    fun `gjenopprett behandling skal sette status til utredes på nytt`(årsak: SettPåVentÅrsak) {
        val behandling = opprettBehandling()

        settPåVentService.settBehandlingPåVent(
            behandling.id,
            frist,
            årsak,
        )
        val behandlingEtterSattPåVent = behandlingRepository.finnBehandling(behandling.id).status
        val gjenopptattSettPåVent = settPåVentService.gjenopptaBehandling(behandling.id)

        assertThat(gjenopptattSettPåVent.aktiv).isFalse()
        assertThat(behandling.status).isEqualTo(BehandlingStatus.UTREDES)
        assertThat(behandlingEtterSattPåVent).isEqualTo(BehandlingStatus.SATT_PÅ_VENT)
        assertThat(behandlingRepository.finnBehandling(behandling.id).status).isEqualTo(BehandlingStatus.UTREDES)
    }

    @ParameterizedTest
    @EnumSource(
        value = BehandlingStatus::class,
        names = ["UTREDES"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `Skal ikke kunne sette en behandling på vent hvis den ikke har status utredes`(status: BehandlingStatus) {
        assertThatThrownBy {
            settPåVentService.settBehandlingPåVent(
                opprettBehandling(status).id,
                LocalDate.now().plusDays(3),
                SettPåVentÅrsak.AVVENTER_DOKUMENTASJON,
            )
        }.hasMessageContaining("har status=$status og kan ikke settes på vent")
    }

    @ParameterizedTest
    @EnumSource(value = SettPåVentÅrsak::class)
    fun `Kan ikke endre på behandling etter at den er satt på vent`(årsak: SettPåVentÅrsak) {
        val behandlingEtterVilkårsvurderingSteg =
            kjørStegprosessForFGB(
                barnasIdenter = listOf(barnFnr),
                tilSteg = StegType.VILKÅRSVURDERING,
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
            )

        val behandlingId = behandlingEtterVilkårsvurderingSteg.id
        settPåVentService.settBehandlingPåVent(
            behandlingId = behandlingId,
            frist = frist,
            årsak = årsak,
        )

        assertThrows<FunksjonellFeil> {
            stegService.håndterBehandlingsresultat(behandlingRepository.finnBehandling(behandlingId))
        }
    }

    @ParameterizedTest
    @EnumSource(value = SettPåVentÅrsak::class)
    fun `Kan endre på behandling etter venting er deaktivert`(årsak: SettPåVentÅrsak) {
        val behandlingEtterVilkårsvurderingSteg =
            kjørStegprosessForFGB(
                barnasIdenter = listOf(barnFnr),
                tilSteg = StegType.VILKÅRSVURDERING,
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
            )

        settPåVentService.settBehandlingPåVent(
            behandlingId = behandlingEtterVilkårsvurderingSteg.id,
            frist = frist,
            årsak = årsak,
        )

        val nå = LocalDate.now()

        val settPåVent =
            settPåVentService.gjenopptaBehandling(
                behandlingId = behandlingEtterVilkårsvurderingSteg.id,
                nå = nå,
            )

        Assertions.assertEquals(
            nå,
            settPåVentRepository.findByIdOrNull(settPåVent.id)!!.tidTattAvVent,
        )

        assertDoesNotThrow {
            stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurderingSteg)
        }
    }

    @ParameterizedTest
    @EnumSource(value = SettPåVentÅrsak::class)
    fun `Kan ikke sette ventefrist til før dagens dato`(årsak: SettPåVentÅrsak) {
        val behandlingEtterVilkårsvurderingSteg =
            kjørStegprosessForFGB(
                barnasIdenter = listOf(barnFnr),
                tilSteg = StegType.VILKÅRSVURDERING,
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
            )

        assertThrows<FunksjonellFeil> {
            settPåVentService.settBehandlingPåVent(
                behandlingId = behandlingEtterVilkårsvurderingSteg.id,
                frist = LocalDate.now().minusDays(1),
                årsak = årsak,
            )
        }
    }

    @ParameterizedTest
    @EnumSource(value = SettPåVentÅrsak::class)
    fun `Kan oppdatere sett på vent på behandling`(årsak: SettPåVentÅrsak) {
        val behandlingEtterVilkårsvurderingSteg =
            kjørStegprosessForFGB(
                barnasIdenter = listOf(barnFnr),
                tilSteg = StegType.VILKÅRSVURDERING,
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
            )

        val behandlingId = behandlingEtterVilkårsvurderingSteg.id

        val settPåVent =
            settPåVentRepository.save(
                SettPåVent(
                    behandling = behandlingEtterVilkårsvurderingSteg,
                    frist = frist,
                    årsak = årsak,
                ),
            )

        Assertions.assertEquals(frist, settPåVentService.finnAktivSettPåVentPåBehandling(behandlingId)!!.frist)

        val nyFrist = LocalDate.now().plusDays(9)

        settPåVent.frist = nyFrist
        settPåVentRepository.save(settPåVent)

        Assertions.assertEquals(nyFrist, settPåVentService.finnAktivSettPåVentPåBehandling(behandlingId)!!.frist)
    }

    @ParameterizedTest
    @EnumSource(value = SettPåVentÅrsak::class)
    fun `Skal gjennopta behandlinger etter ventefristen`(årsak: SettPåVentÅrsak) {
        val behandling1 =
            kjørStegprosessForFGB(
                barnasIdenter = listOf(barnFnr),
                tilSteg = StegType.VILKÅRSVURDERING,
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
            )

        val behandling2 =
            kjørStegprosessForFGB(
                barnasIdenter = listOf(barnFnr),
                tilSteg = StegType.VILKÅRSVURDERING,
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
            )

        settPåVentService
            .settBehandlingPåVent(
                behandlingId = behandling1.id,
                frist = frist,
                årsak = årsak,
            ).let {
                settPåVentRepository.save(it.copy(frist = LocalDate.now().minusDays(1)))
            }

        settPåVentService.settBehandlingPåVent(
            behandlingId = behandling2.id,
            frist = frist,
            årsak = årsak,
        )

        taBehandlingerEtterVentefristAvVentTask.doTask(
            Task(
                type = TaBehandlingerEtterVentefristAvVentTask.TASK_STEP_TYPE,
                payload = "",
            ),
        )

        Assertions.assertNull(settPåVentRepository.findByBehandlingIdAndAktiv(behandling1.id, true))
        Assertions.assertNotNull(settPåVentRepository.findByBehandlingIdAndAktiv(behandling2.id, true))
    }

    @ParameterizedTest
    @EnumSource(value = SettPåVentÅrsak::class)
    fun `Skal ikke kunne gjenoppta behandlingen hvis den er satt på maskinell vent`(årsak: SettPåVentÅrsak) {
        val behandling = opprettBehandling()

        settPåVentService.settBehandlingPåVent(
            behandling.id,
            frist,
            årsak,
        )
        snikeIKøenService.settAktivBehandlingPåMaskinellVent(behandling.id, SettPåMaskinellVentÅrsak.SATSENDRING)

        val throwable =
            catchThrowable {
                settPåVentService.gjenopptaBehandling(behandling.id)
            }
        assertThat(throwable).isInstanceOf(FunksjonellFeil::class.java)
        assertThat((throwable as FunksjonellFeil).frontendFeilmelding)
            .isEqualTo("Behandlingen er under maskinell vent, og kan gjenopptas senere.")
    }

    @Test
    fun `Skal kaste feil for årsak AVVENTER_SAMTYKKE_ULOVFESTET_MOTREGNING med annen frist enn 5 dager`() {
        val behandling = opprettBehandling()

        val feil =
            assertThrows<Feil> {
                settPåVentService.settBehandlingPåVent(
                    behandlingId = behandling.id,
                    frist = LocalDate.now().plusDays(1),
                    årsak = SettPåVentÅrsak.AVVENTER_SAMTYKKE_ULOVFESTET_MOTREGNING,
                )
            }

        assertThat(feil.message).isEqualTo("Uventet frist for SettPåVent med årsak AVVENTER_SAMTYKKE_ULOVFESTET_MOTREGNING for behandling ${behandling.id}. Forventet frist er 5 dager, faktisk frist er 1 dager.")
    }

    @Test
    fun `Skal opprette forenklet tilbakekrevingsvedtak for behandlinger som settes på vent med årsak AVVENTER_SAMTYKKE_ULOVFESTET_MOTREGNING`() {
        val behandling = opprettBehandling()

        val settPåVent =
            settPåVentService.settBehandlingPåVent(
                behandlingId = behandling.id,
                frist = frist,
                årsak = SettPåVentÅrsak.AVVENTER_SAMTYKKE_ULOVFESTET_MOTREGNING,
            )

        assertThat(settPåVent.årsak).isEqualTo(SettPåVentÅrsak.AVVENTER_SAMTYKKE_ULOVFESTET_MOTREGNING)
        assertThat(settPåVent.frist).isEqualTo(frist)

        val forenkletTilbakekrevingsvedtak = forenkletTilbakekrevingsvedtakService.finnForenkletTilbakekrevingsvedtak(behandling.id)

        assertThat(forenkletTilbakekrevingsvedtak).isNotNull()
        assertThat(forenkletTilbakekrevingsvedtak!!.behandling.id).isEqualTo(behandling.id)
    }

    @ParameterizedTest
    @EnumSource(value = SettPåVentÅrsak::class, mode = EnumSource.Mode.EXCLUDE, names = ["AVVENTER_SAMTYKKE_ULOVFESTET_MOTREGNING"])
    fun `Skal ikke opprette forenklet tilbakekrevingsvedtak for behandlinger som settes på vent med annen årsak enn AVVENTER_SAMTYKKE_ULOVFESTET_MOTREGNING`(årsak: SettPåVentÅrsak) {
        val behandling = opprettBehandling()

        settPåVentService.settBehandlingPåVent(
            behandlingId = behandling.id,
            frist = frist,
            årsak = årsak,
        )

        val forenkletTilbakekrevingsvedtak = forenkletTilbakekrevingsvedtakService.finnForenkletTilbakekrevingsvedtak(behandling.id)

        assertThat(forenkletTilbakekrevingsvedtak).isNull()
    }

    private fun opprettBehandling(status: BehandlingStatus = BehandlingStatus.UTREDES): Behandling {
        val fagsak = fagsakService.hentEllerOpprettFagsak(randomFnr())
        val behandling =
            Behandling(
                fagsak = fagsak,
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                type = BehandlingType.REVURDERING,
                opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                status = status,
            ).initBehandlingStegTilstand()
        return behandlingService.lagreNyOgDeaktiverGammelBehandling(behandling)
    }
}
