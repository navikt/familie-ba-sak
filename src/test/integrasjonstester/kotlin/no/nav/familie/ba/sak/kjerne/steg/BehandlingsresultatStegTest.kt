package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.mockk
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.randomBarnFødselsdato
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning.TilbakekrevingsvedtakMotregningService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjørbehandling.kjørStegprosessForFGB
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BehandlingsresultatStegTest(
    @Autowired private val stegService: StegService,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val persongrunnlagService: PersongrunnlagService,
    @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired private val tilbakekrevingsvedtakMotregningService: TilbakekrevingsvedtakMotregningService,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `skal slette Tilbakekrevingsvedtak motregning hvis behandling ikke lenger gjør avregning`() {
        // Arrange
        val barnFnr = leggTilPersonInfo(randomBarnFødselsdato(alder = 6))

        val behandling =
            kjørStegprosessForFGB(
                tilSteg = StegType.VILKÅRSVURDERING,
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

        val tilbakekrevingsvedtakMotregning =
            tilbakekrevingsvedtakMotregningService.opprettTilbakekrevingsvedtakMotregning(behandlingId = behandling.id)

        assertThat(tilbakekrevingsvedtakMotregning.behandling.id).isEqualTo(behandling.id)

        // Act
        stegService.håndterBehandlingsresultat(behandling)

        // Assert
        val tilbakekrevingsvedtakMotregningEtterBehandlingsresultatSteg =
            tilbakekrevingsvedtakMotregningService.finnTilbakekrevingsvedtakMotregning(behandlingId = behandling.id)

        assertThat(tilbakekrevingsvedtakMotregningEtterBehandlingsresultatSteg).isNull()
    }
}
