package no.nav.familie.ba.sak.simulering

import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.simuleringMottakerMock
import no.nav.familie.ba.sak.tilbakekreving.TilbakekrevingService
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

@SpringBootTest(properties = ["FAMILIE_INTEGRASJONER_API_URL=http://localhost:28085/api"])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles(
        "postgres",
        "mock-dokgen-klient",
        "mock-arbeidsfordeling",
        "mock-økonomi",
        "mock-oauth",
        "mock-pdl",
        "mock-task-repository",
        "mock-infotrygd-barnetrygd",
        "mock-tilbakekreving-klient",
)
@Tag("integration")
@AutoConfigureWireMock(port = 28085)
class SimuleringServiceTest(
        @Autowired private val fagsakService: FagsakService,
        @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
        @Autowired private val persongrunnlagService: PersongrunnlagService,
        @Autowired private val vedtakService: VedtakService,
        @Autowired private val stegService: StegService,
        @Autowired private val simuleringService: SimuleringService,
        @Autowired private val tilbakekrevingService: TilbakekrevingService,
) {

    @Test
    fun `Skal verifisere at simulering blir lagert og oppdatert`() {
        val behandlingEtterVilkårsvurderingSteg = kjørStegprosessForFGB(
                tilSteg = StegType.VURDER_TILBAKEKREVING,
                søkerFnr = ClientMocks.søkerFnr[0],
                barnasIdenter = listOf(ClientMocks.barnFnr[0]),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                tilbakekrevingService = tilbakekrevingService
        )

        val vedtakSimuleringMottakerMock =
                simuleringMottakerMock.map { it.tilBehandlingSimuleringMottaker(behandlingEtterVilkårsvurderingSteg) }

        assertEquals(vedtakSimuleringMottakerMock.size,
                     simuleringService.oppdaterSimuleringPåBehandlingVedBehov(behandlingEtterVilkårsvurderingSteg.id).size)

        assertEquals(vedtakSimuleringMottakerMock.size,
                     simuleringService.oppdaterSimuleringPåBehandling(behandlingEtterVilkårsvurderingSteg).size)
    }
}