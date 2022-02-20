package no.nav.familie.ba.sak.kjerne.behandling.settpåvent

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate

@Tag("integration")
class SettPåVentServiceTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val databaseCleanupService: DatabaseCleanupService,
    @Autowired private val stegService: StegService,
    @Autowired private val settPåVentService: SettPåVentService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val persongrunnlagService: PersongrunnlagService,
    @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val settPåVentRepository: SettPåVentRepository,
) : AbstractSpringIntegrationTest() {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Kan ikke endre på behandling etter at den er satt på vent`() {
        val behandlingEtterVilkårsvurderingSteg = kjørStegprosessForFGB(
            tilSteg = StegType.VILKÅRSVURDERING,
            søkerFnr = randomFnr(),
            barnasIdenter = listOf(randomFnr()),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
        )

        settPåVentRepository.save(
            SettPåVent(
                behandling = behandlingEtterVilkårsvurderingSteg,
                frist = LocalDate.now().plusDays(21),
                årsak = SettPåVentÅrsak.AVVENTER_DOKUMENTASJON
            )
        )

        assertThrows<FunksjonellFeil> {
            stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurderingSteg)
        }
    }

    @Test
    fun `Kan endre på behandling etter venting er deaktivert`() {
        val behandlingEtterVilkårsvurderingSteg = kjørStegprosessForFGB(
            tilSteg = StegType.VILKÅRSVURDERING,
            søkerFnr = randomFnr(),
            barnasIdenter = listOf(randomFnr()),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
        )

        settPåVentRepository.save(
            SettPåVent(
                behandling = behandlingEtterVilkårsvurderingSteg,
                frist = LocalDate.now().plusDays(21),
                årsak = SettPåVentÅrsak.AVVENTER_DOKUMENTASJON
            )
        )

        val nå = LocalDate.now()

        val settPåVent = settPåVentService.gjenopptaBehandling(
            behandlingId = behandlingEtterVilkårsvurderingSteg.id,
            nå = nå
        )

        Assertions.assertEquals(
            nå,
            settPåVentRepository.findByIdOrNull(settPåVent.id)!!.tidTattAvVent
        )

        assertDoesNotThrow {
            stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurderingSteg)
        }
    }

    @Test
    fun `Kan ikke sette ventefrist til før dagens dato`() {
        val behandlingEtterVilkårsvurderingSteg = kjørStegprosessForFGB(
            tilSteg = StegType.VILKÅRSVURDERING,
            søkerFnr = randomFnr(),
            barnasIdenter = listOf(randomFnr()),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
        )

        assertThrows<FunksjonellFeil> {
            settPåVentService.settBehandlingPåVent(
                behandlingId = behandlingEtterVilkårsvurderingSteg.id,
                frist = LocalDate.now().minusDays(1),
                årsak = SettPåVentÅrsak.AVVENTER_DOKUMENTASJON
            )
        }
    }

    @Test
    fun `Kan oppdatare set på vent på behandling`() {
        val behandlingEtterVilkårsvurderingSteg = kjørStegprosessForFGB(
            tilSteg = StegType.VILKÅRSVURDERING,
            søkerFnr = randomFnr(),
            barnasIdenter = listOf(randomFnr()),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
        )

        val behandlingId = behandlingEtterVilkårsvurderingSteg.id

        val frist1 = LocalDate.now().plusDays(21)

        val settPåVent = settPåVentRepository.save(
            SettPåVent(
                behandling = behandlingEtterVilkårsvurderingSteg,
                frist = frist1,
                årsak = SettPåVentÅrsak.AVVENTER_DOKUMENTASJON
            )
        )

        Assertions.assertEquals(frist1, settPåVentService.finnAktivSettPåVentPåBehandlingThrows(behandlingId).frist)

        val frist2 = LocalDate.now().plusDays(9)

        settPåVent.frist = frist2
        settPåVentRepository.save(settPåVent)

        Assertions.assertEquals(frist2, settPåVentService.finnAktivSettPåVentPåBehandlingThrows(behandlingId).frist)
    }
}
