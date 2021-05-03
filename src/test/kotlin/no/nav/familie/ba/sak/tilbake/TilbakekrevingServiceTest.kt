package no.nav.familie.ba.sak.tilbake

import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.tilbakekreving.TilbakekrevingRepository
import no.nav.familie.ba.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest(properties = ["FAMILIE_FAMILIE_TILBAKE_API_URL=http://localhost:28085/api"])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles(
        "postgres",
        "mock-brev-klient",
        "mock-oauth",
        "mock-pdl",
        "mock-arbeidsfordeling",
        "mock-familie-tilbake",
        "mock-infotrygd-feed",
        "mock-økonomi",
        "mock-tilbakekreving-klient",
        "mock-infotrygd-barnetrygd"
)
@Tag("integration")
@AutoConfigureWireMock(port = 28085)
class TilbakekrevingServiceTest(
        @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
        @Autowired private val vedtakService: VedtakService,
        @Autowired private val persongrunnlagService: PersongrunnlagService,
        @Autowired private val fagsakService: FagsakService,
        @Autowired private val stegService: StegService,
        @Autowired private val tilbakekrevingService: TilbakekrevingService,
        @Autowired private val tilbakekrevingRepository: TilbakekrevingRepository,
) {

    @Test
    @Tag("integration")
    fun `Opprett tilbakekreving`() {
        val behandling = kjørStegprosessForFGB(
                tilSteg = StegType.IVERKSETT_MOT_FAMILIE_TILBAKE,
                søkerFnr = "12345678910",
                barnasIdenter = listOf(ClientMocks.barnFnr[0]),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                tilbakekrevingService = tilbakekrevingService
        )

        val tilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandling.id)

        assertEquals(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL, tilbakekreving?.valg)
        assertEquals("id1", tilbakekreving?.tilbakekrevingsbehandlingId)
        assertEquals("Varsel", tilbakekreving?.varsel)
    }
}