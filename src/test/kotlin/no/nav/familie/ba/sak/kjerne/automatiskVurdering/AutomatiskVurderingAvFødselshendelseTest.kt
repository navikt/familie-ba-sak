package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FiltreringsreglerService
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FødselshendelseServiceNy
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.VelgFagSystemService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fødselshendelse.EvaluerFiltreringsreglerForFødselshendelse
import no.nav.familie.ba.sak.kjerne.fødselshendelse.gdpr.GDPRService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles

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
    private val fødselshendelseServiceNy = mockk<FødselshendelseServiceNy>()
    private val infotrygdBarnetrygdClientMock = mockk<InfotrygdBarnetrygdClient>()
    private val personopplysningerServiceMock = mockk<PersonopplysningerService>()
    private val infotrygdFeedServiceMock = mockk<InfotrygdFeedService>()
    private val envServiceMock = mockk<EnvService>()
    private val stegServiceMock = mockk<StegService>()
    private val vedtakServiceMock = mockk<VedtakService>()
    private val evaluerFiltreringsreglerForFødselshendelseMock = mockk<EvaluerFiltreringsreglerForFødselshendelse>()
    private val evaluerFiltreringsreglerServiceMock = mockk<FiltreringsreglerService>()
    private val taskRepositoryMock = mockk<TaskRepository>()
    private val behandlingResultatRepositoryMock = mockk<VilkårsvurderingRepository>()
    private val persongrunnlagServiceMock = mockk<PersongrunnlagService>(relaxed = true)
    private val behandlingRepositoryMock = mockk<BehandlingRepository>()
    private val gdprServiceMock = mockk<GDPRService>()

    //Husk å endre familiebasakklient tilbake!
    fun fødselshendelseKlient() = FødselshendelseKlient(
            baSakUrl = hentUrl(""),
            restOperations = restOperations,
            headers = hentHeaders()
    )

    @Test
    fun `Starter en ny behandling`() {
        every {
            mockVelgFagSystemService.velgFagsystem(NyBehandlingHendelse(søkerFnr,
                                                                        listOf(barnFnr)))
        } returns VelgFagSystemService.FagsystemRegelVurdering.SEND_TIL_BA
 
        every { mockPdlRestClient.hentPerson("21111777001", any()) } returns mockBarnAutomatiskBehandling
        every { mockPdlRestClient.hentPerson("04086226621", any()) } returns mockSøkerAutomatiskBehandling
        fødselshendelseKlient().sendTilSak(nyBehandling)
    }
}