package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.randomBarnFødselsdato
import no.nav.familie.ba.sak.datagenerator.randomSøkerFødselsdato
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjørbehandling.kjørStegprosessForFGB
import no.nav.familie.ba.sak.kjørbehandling.kjørStegprosessForRevurderingÅrligKontroll
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FagsakDtoTest(
    @Autowired
    private val stegService: StegService,
    @Autowired
    private val vedtakService: VedtakService,
    @Autowired
    private val persongrunnlagService: PersongrunnlagService,
    @Autowired
    private val fagsakService: FagsakService,
    @Autowired
    private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired
    private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired
    private val brevmalService: BrevmalService,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `Skal sjekke at gjeldende utbetalingsperioder kommer med i fagsak dto`() {
        val søkerFnr = leggTilPersonInfo(randomSøkerFødselsdato())
        val barnFnr = leggTilPersonInfo(randomBarnFødselsdato(10))

        val førstegangsbehandling =
            kjørStegprosessForFGB(
                tilSteg = StegType.BEHANDLING_AVSLUTTET,
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

        kjørStegprosessForRevurderingÅrligKontroll(
            tilSteg = StegType.BEHANDLINGSRESULTAT,
            søkerFnr = søkerFnr,
            barnasIdenter = listOf(barnFnr),
            vedtakService = vedtakService,
            stegService = stegService,
            fagsakId = førstegangsbehandling.fagsak.id,
            brevmalService = brevmalService,
            vedtaksperiodeService = vedtaksperiodeService,
        )

        val fagsakDto = fagsakService.hentFagsakDto(fagsakId = førstegangsbehandling.fagsak.id)

        assertThat(fagsakDto.data?.gjeldendeUtbetalingsperioder).isNotEmpty
    }
}
