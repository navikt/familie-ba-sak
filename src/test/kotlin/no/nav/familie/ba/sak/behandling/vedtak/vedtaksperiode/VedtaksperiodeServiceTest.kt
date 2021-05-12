package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.kjørStegprosessForRevurderingÅrligKontroll
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.tilbakekreving.TilbakekrevingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles(
        "postgres",
        "mock-oauth",
        "mock-pdl",
        "mock-arbeidsfordeling",
        "mock-infotrygd-barnetrygd",
        "mock-infotrygd-feed",
        "mock-økonomi",
        "mock-brev-klient",
        "mock-tilbakekreving-klient"
)
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VedtaksperiodeServiceTest(
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
        private val tilbakekrevingService: TilbakekrevingService,

        @Autowired
        private val databaseCleanupService: DatabaseCleanupService,

        @Autowired
        private val vedtaksperiodeService: VedtaksperiodeService
) {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal hente ut vedtaksperiode ved fortsatt innvilget som resultat`() {
        val søkerFnr = randomFnr()
        kjørStegprosessForFGB(
                tilSteg = StegType.BEHANDLING_AVSLUTTET,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(ClientMocks.barnFnr[0]),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                tilbakekrevingService = tilbakekrevingService
        )

        val revurdering = kjørStegprosessForRevurderingÅrligKontroll(
                tilSteg = StegType.VILKÅRSVURDERING,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(ClientMocks.barnFnr[0]),
                vedtakService = vedtakService,
                stegService = stegService,
                tilbakekrevingService = tilbakekrevingService
        )

        assertEquals(BehandlingResultat.FORTSATT_INNVILGET, revurdering.resultat)

        val vedtaksperioder = vedtaksperiodeService.hentVedtaksperioder(revurdering)

        assertEquals(1, vedtaksperioder.size)
        assertEquals(Vedtaksperiodetype.FORTSATT_INNVILGET, vedtaksperioder.first().vedtaksperiodetype)
        assertEquals(inneværendeMåned().førsteDagIInneværendeMåned(), vedtaksperioder.first().periodeFom)
    }
}