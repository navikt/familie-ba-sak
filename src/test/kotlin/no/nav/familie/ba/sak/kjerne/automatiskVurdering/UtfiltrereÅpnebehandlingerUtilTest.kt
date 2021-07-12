package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FødselshendelseServiceNy
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.harSøkerÅpneBehandlinger
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.steg.StegService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest(properties = ["FAMILIE_FAMILIE_TILBAKE_API_URL=http://localhost:28085/api"])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles(
        "dev",
        "postgress",
        "mock-pdl"
)
class UtfiltrereÅpnebehandlingerUtilTest(
        @Autowired
        val fødselhendelseserviceNy: FødselshendelseServiceNy,

        @Autowired
        val behandlingsService: BehandlingService,

        @Autowired
        val stegService: StegService

) {


    private val opprettetBehandling = lagBehandling().copy(status = BehandlingStatus.OPPRETTET)
    private val avsluttetBehandling = lagBehandling().copy(status = BehandlingStatus.AVSLUTTET)


    @Test
    fun `Mor har en åpen og en avsluttet behandling`() {
        Assertions.assertTrue(harSøkerÅpneBehandlinger(listOf(opprettetBehandling, avsluttetBehandling)))
    }

    @Test
    fun `Mor har en avsluttet og ingen åpne behandling`() {
        Assertions.assertFalse(harSøkerÅpneBehandlinger(listOf(avsluttetBehandling)))
    }

    @Test
    fun `Mor har to åpne behandlinger`() {
        Assertions.assertTrue(harSøkerÅpneBehandlinger(listOf(opprettetBehandling, opprettetBehandling)))
    }

    @Test
    fun `Mor har to avsluttede og ingen åpne behandlinger`() {
        Assertions.assertFalse(harSøkerÅpneBehandlinger(listOf(avsluttetBehandling, avsluttetBehandling)))
    }

    @Test
    fun `Mor har åpen behandling i fødselshendelseservice`() {

        //val nyBehandling = NyBehandlingHendelse(enMorsIdent, listOf(etBarnsIdent))

        val søkerFnr = "11211211211"
        val barnasIdenter = "10987654321"

        val nyBehandling = NyBehandlingHendelse(
                morsIdent = søkerFnr,
                barnasIdenter = listOf(barnasIdenter)
        )
        val behandling = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
        behandling.status = BehandlingStatus.OPPRETTET

        /*kjørStegprosessForFGB(
                tilSteg = StegType.BEHANDLING_AVSLUTTET,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(ClientMocks.barnFnr[0]),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                tilbakekrevingService = tilbakekrevingService,
                vedtaksperiodeService = vedtaksperiodeService,
        )*/

        Assertions.assertTrue(fødselhendelseserviceNy.harMorÅpenBehandlingIBASAK(nyBehandling))
    }
}