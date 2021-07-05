package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FødselshendelseServiceNy
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.VelgFagSystemService
import no.nav.familie.ba.sak.kjerne.behandling.Fødselshendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakPerson
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

//@ActiveProfiles("dev", "mock-pdl", "mock-arbeidsfordeling", "mock-infotrygd-barnetrygd")

@ActiveProfiles(
        "postgres",
        "mock-pdl-verdikjede-førstegangssøknad-nasjonal",
        "mock-oauth",
        "mock-arbeidsfordeling",
        "mock-tilbakekreving-klient",
        "mock-brev-klient",
        "mock-økonomi",
        "mock-infotrygd-feed",
        "mock-infotrygd-barnetrygd",
)
class AutomatiskVurderingAvFødselshendelseTestWebSpringAuthTestRunner : WebSpringAuthTestRunner() {

    private val barnFnr = "21111777001"
    private val søkerFnr = "04086226621"
    private val søker = mockSøkerAutomatiskBehandling
    private val nyBehandling = nyOrdinærBehandling(søkerFnr, BehandlingÅrsak.FØDSELSHENDELSE)

    private val mockPdlRestClient: PdlRestClient = mockk(relaxed = true)
    private val mockVelgFagSystemService = mockk<VelgFagSystemService>()
    private val mockFødselshendelseServiceNy = mockk<FødselshendelseServiceNy>()
    private val mockFagsakService = mockk<FagsakService>()
    //private val infotrygdBarnetrygdClientMock = mockk<InfotrygdBarnetrygdClient>()
    //private val personopplysningerServiceMock = mockk<PersonopplysningerService>()
    //private val infotrygdFeedServiceMock = mockk<InfotrygdFeedService>()
    //private val envServiceMock = mockk<EnvService>()
    //private val stegServiceMock = mockk<StegService>()
    //private val vedtakServiceMock = mockk<VedtakService>()
    //private val evaluerFiltreringsreglerForFødselshendelseMock = mockk<EvaluerFiltreringsreglerForFødselshendelse>()
    //private val evaluerFiltreringsreglerServiceMock = mockk<FiltreringsreglerService>()
    //private val taskRepositoryMock = mockk<TaskRepository>()
    //private val behandlingResultatRepositoryMock = mockk<VilkårsvurderingRepository>()
    //private val persongrunnlagServiceMock = mockk<PersongrunnlagService>(relaxed = true)
    //private val behandlingRepositoryMock = mockk<BehandlingRepository>()
    //private val gdprServiceMock = mockk<GDPRService>()

    //Husk å endre familiebasakklient tilbake!
    fun fødselshendelseKlient() = FødselshendelseKlient(
            baSakUrl = hentUrl(""),
            restOperations = restOperations,
            headers = hentHeaders(groups = listOf(BehandlerRolle.SYSTEM.toString()))
    )

    @Test
    fun `Starter en ny behandling`() {
        every {
            mockVelgFagSystemService.velgFagsystem(Fødselshendelse(søkerFnr, listOf(barnFnr)))
        } returns VelgFagSystemService.FagsystemRegelVurdering.SEND_TIL_BA
        every {
            mockFødselshendelseServiceNy.sjekkOmMorHarÅpentBehandlingIBASak(Fødselshendelse(søkerFnr, listOf(barnFnr)))
        } returns false
        every {
            mockFødselshendelseServiceNy.sjekkOmMorHarÅpentBehandlingIBASak(Fødselshendelse(søkerFnr, listOf(barnFnr)))
        } returns true

        val testFagsak = Fagsak(1).also {
            it.søkerIdenter =
                    setOf(FagsakPerson(fagsak = it,
                                       personIdent = PersonIdent("04086226621"),
                                       opprettetTidspunkt = LocalDateTime.now()))
        }
        every {
            mockFagsakService.hentEllerOpprettFagsakForPersonIdent(nyBehandling.søkersIdent, true)
        } returns testFagsak
        
        every { mockPdlRestClient.hentPerson("21111777001", any()) } returns mockBarnAutomatiskBehandling
        every { mockPdlRestClient.hentPerson("04086226621", any()) } returns mockSøkerAutomatiskBehandling
        fødselshendelseKlient().sendTilSak(Fødselshendelse(søkerFnr, listOf(barnFnr)))
    }
}