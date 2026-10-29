package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.mockk
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.randomBarnFødselsdato
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjørbehandling.kjørStegprosessForFGB
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

class TilbakestillBehandlingIntegrasjonTest(
    @Autowired private val stegService: StegService,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val persongrunnlagService: PersongrunnlagService,
    @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val tilbakestillBehandlingService: TilbakestillBehandlingService,
    @Autowired private val tilbakestillBehandlingTilBehandlingsresultatService: TilbakestillBehandlingTilBehandlingsresultatService,
    @Autowired transactionManager: PlatformTransactionManager,
) : AbstractSpringIntegrationTest() {
    private val transactionTemplate = TransactionTemplate(transactionManager)

    @Test
    fun `skal kun ha vilkårsvurdering som ikke utført steg når behandling på vilkårsvurdering tilbakestilles til behandlingsresultat og vilkårsvurdering i samme transaksjon`() {
        // Arrange
        val barnFnr = leggTilPersonInfo(randomBarnFødselsdato(alder = 6))

        val behandling =
            kjørStegprosessForFGB(
                tilSteg = StegType.REGISTRERE_SØKNAD,
                søkerFnr = randomFnr(),
                barnasIdenter = listOf(barnFnr),
                stegService = stegService,
                fagsakService = fagsakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                vedtakService = mockk(),
                brevmalService = mockk(),
                vedtaksperiodeService = mockk(),
            )

        assertThat(behandling.steg).isEqualTo(StegType.VILKÅRSVURDERING)

        // Act
        transactionTemplate.executeWithoutResult {
            tilbakestillBehandlingTilBehandlingsresultatService.tilbakestillBehandlingTilBehandlingsresultat(behandling.id)
            tilbakestillBehandlingService.resettStegVedEndringPåVilkår(behandling.id)
        }

        // Assert
        val behandlingStegTilstand = behandlingHentOgPersisterService.hent(behandling.id).behandlingStegTilstand

        assertThat(behandlingStegTilstand.map { it.behandlingSteg }).doesNotContain(StegType.BEHANDLINGSRESULTAT)
        assertThat(behandlingStegTilstand.filter { it.behandlingStegStatus == BehandlingStegStatus.IKKE_UTFØRT }.map { it.behandlingSteg })
            .containsExactly(StegType.VILKÅRSVURDERING)
    }
}
