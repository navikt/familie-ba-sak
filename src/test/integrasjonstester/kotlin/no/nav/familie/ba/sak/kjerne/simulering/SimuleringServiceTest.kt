package no.nav.familie.ba.sak.kjerne.simulering

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.randomBarnFødselsdato
import no.nav.familie.ba.sak.datagenerator.randomSøkerFødselsdato
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjørbehandling.kjørStegprosessForFGB
import no.nav.familie.ba.sak.mock.simuleringMottakerMock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SimuleringServiceTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired private val persongrunnlagService: PersongrunnlagService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val simuleringService: SimuleringService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val brevmalService: BrevmalService,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `Skal verifisere at simulering blir lagert og oppdatert`() {
        val søkerFnr = leggTilPersonInfo(randomSøkerFødselsdato())
        val barnFnr = leggTilPersonInfo(randomBarnFødselsdato())
        val behandlingEtterVilkårsvurderingSteg =
            kjørStegprosessForFGB(
                tilSteg = StegType.VURDER_TILBAKEKREVING,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
            )

        val vedtakSimuleringMottakerMock =
            simuleringMottakerMock.map { it.tilBehandlingSimuleringMottaker(behandlingEtterVilkårsvurderingSteg) }

        assertEquals(
            vedtakSimuleringMottakerMock.size,
            simuleringService.oppdaterSimuleringPåBehandlingVedBehov(behandlingEtterVilkårsvurderingSteg.id).size,
        )

        assertEquals(
            vedtakSimuleringMottakerMock.size,
            simuleringService.oppdaterSimuleringPåBehandling(behandlingEtterVilkårsvurderingSteg).size,
        )
    }
}
