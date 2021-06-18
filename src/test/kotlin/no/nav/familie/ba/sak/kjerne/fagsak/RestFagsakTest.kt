package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.kjørStegprosessForRevurderingÅrligKontroll
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles(
        "dev",
        "mock-totrinnkontroll",
        "mock-brev-klient",
        "mock-økonomi",
        "mock-pdl",
        "mock-infotrygd-feed",
        "mock-tilbakekreving-klient",
        "mock-infotrygd-barnetrygd",
        "mock-task-repository"
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RestFagsakTest(
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
        private val databaseCleanupService: DatabaseCleanupService,

        @Autowired
        private val tilbakekrevingService: TilbakekrevingService,

        @Autowired
        private val vedtaksperiodeService: VedtaksperiodeService,

        ) {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal sjekke at gjeldende utbetalingsperioder kommer med i restfagsak`() {
        val søkerFnr = randomFnr()
        val barnFnr = ClientMocks.barnFnr[0]

        val førstegangsbehandling = kjørStegprosessForFGB(
                tilSteg = StegType.BEHANDLING_AVSLUTTET,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                tilbakekrevingService = tilbakekrevingService,
                vedtaksperiodeService,
        )

        kjørStegprosessForRevurderingÅrligKontroll(
                tilSteg = StegType.VILKÅRSVURDERING,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                vedtakService = vedtakService,
                stegService = stegService,
                tilbakekrevingService = tilbakekrevingService
        )

        val restfagsak = fagsakService.hentRestFagsak(fagsakId = førstegangsbehandling.fagsak.id)

        assertEquals(1, restfagsak.data?.gjeldendeUtbetalingsperioder?.size)
    }
}