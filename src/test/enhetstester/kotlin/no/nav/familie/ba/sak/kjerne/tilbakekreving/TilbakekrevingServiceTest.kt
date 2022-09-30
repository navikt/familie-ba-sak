package no.nav.familie.ba.sak.kjerne.tilbakekreving

import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.opprettRestTilbakekreving
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.ekstern.restDomene.InstitusjonInfo
import no.nav.familie.ba.sak.ekstern.restDomene.VergeInfo
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
import java.util.Properties

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
    fun `tilbakekreving skal bli OPPRETT_TILBAKEKREVING_MED_VARSEL når man oppretter tilbakekreving med varsel`() {
        val behandling = kjørStegprosessForFGB(
            tilSteg = StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
            søkerFnr = randomFnr(),
            barnasIdenter = listOf(ClientMocks.barnFnr[0]),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService
        )

        val restTilbakekreving = opprettRestTilbakekreving()
        tilbakekrevingService.validerRestTilbakekreving(restTilbakekreving, behandling.id)
        tilbakekrevingService.lagreTilbakekreving(restTilbakekreving, behandling.id)

        stegService.håndterIverksettMotFamilieTilbake(behandling, Properties())

        val tilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandling.id)

        assertEquals(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL, tilbakekreving?.valg)
        assertEquals("id1", tilbakekreving?.tilbakekrevingsbehandlingId)
        assertEquals("Varsel", tilbakekreving?.varsel)
    }

    @Test
    @Tag("integration")
    fun `tilbakekreving skal bli OPPRETT_TILBAKEKREVING_MED_VARSEL når man oppretter tilbakekreving med varsel for institusjon`() {
        val behandling = kjørStegprosessForFGB(
            tilSteg = StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
            søkerFnr = "09121079074",
            barnasIdenter = listOf("09121079074"),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            institusjon = InstitusjonInfo(orgNummer = "998765432", tssEksternId = "8000000")
        )

        val restTilbakekreving = opprettRestTilbakekreving()
        tilbakekrevingService.validerRestTilbakekreving(restTilbakekreving, behandling.id)
        tilbakekrevingService.lagreTilbakekreving(restTilbakekreving, behandling.id)

        stegService.håndterIverksettMotFamilieTilbake(behandling, Properties())

        val tilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandling.id)

        assertEquals(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL, tilbakekreving?.valg)
        assertEquals("id1", tilbakekreving?.tilbakekrevingsbehandlingId)
        assertEquals("Varsel", tilbakekreving?.varsel)
    }

    @Test
    @Tag("integration")
    fun `tilbakekreving skal bli OPPRETT_TILBAKEKREVING_MED_VARSEL når man oppretter tilbakekreving med varsel for mindreårig med verge`() {
        val behandling = kjørStegprosessForFGB(
            tilSteg = StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
            søkerFnr = "10031000033",
            barnasIdenter = listOf("10031000033"),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            verge = VergeInfo("04068203010")
        )

        val restTilbakekreving = opprettRestTilbakekreving()
        tilbakekrevingService.validerRestTilbakekreving(restTilbakekreving, behandling.id)
        tilbakekrevingService.lagreTilbakekreving(restTilbakekreving, behandling.id)

        stegService.håndterIverksettMotFamilieTilbake(behandling, Properties())

        val tilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandling.id)

        assertEquals(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL, tilbakekreving?.valg)
        assertEquals("id1", tilbakekreving?.tilbakekrevingsbehandlingId)
        assertEquals("Varsel", tilbakekreving?.varsel)
    }
}
