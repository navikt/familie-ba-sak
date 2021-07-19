package no.nav.familie.ba.sak.kjerne.tilbakekreving

import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TilbakekrevingServiceTest(
        @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
        @Autowired private val vedtakService: VedtakService,
        @Autowired private val persongrunnlagService: PersongrunnlagService,
        @Autowired private val fagsakService: FagsakService,
        @Autowired private val stegService: StegService,
        @Autowired private val tilbakekrevingService: TilbakekrevingService,
        @Autowired private val tilbakekrevingRepository: TilbakekrevingRepository,
        @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
        @Autowired private val databaseCleanupService: DatabaseCleanupService
) : AbstractSpringIntegrationTest() {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    @Tag("integration")
    fun `Opprett tilbakekreving`() {
        val behandling = kjørStegprosessForFGB(
                tilSteg = StegType.IVERKSETT_MOT_FAMILIE_TILBAKE,
                søkerFnr = randomFnr(),
                barnasIdenter = listOf(ClientMocks.barnFnr[0]),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                tilbakekrevingService = tilbakekrevingService,
                vedtaksperiodeService = vedtaksperiodeService,
        )

        val tilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandling.id)

        assertEquals(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL, tilbakekreving?.valg)
        assertEquals("id1", tilbakekreving?.tilbakekrevingsbehandlingId)
        assertEquals("Varsel", tilbakekreving?.varsel)
    }
}