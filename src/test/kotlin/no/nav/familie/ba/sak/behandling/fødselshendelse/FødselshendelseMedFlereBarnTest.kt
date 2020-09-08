package no.nav.familie.ba.sak.behandling.fødselshendelse

import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatRepository
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.BeforeClass
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles


@SpringBootTest
@ActiveProfiles("dev", "mock-pdl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FødselshendelseMedFlereBarnTest(
        @Autowired
        private val fødselshendelseService: FødselshendelseService
){
    val infotrygdBarnetrygdClientMock = mockk<InfotrygdBarnetrygdClient>()
    val personopplysningerServiceMock = mockk<PersonopplysningerService>()
    val infotrygdFeedServiceMock = mockk<InfotrygdFeedService>()
    val featureToggleServiceMock = mockk<FeatureToggleService>()
    val stegServiceMock = mockk<StegService>()
    val vedtakServiceMock = mockk<VedtakService>()
    val evaluerFiltreringsreglerForFødselshendelseMock = mockk<EvaluerFiltreringsreglerForFødselshendelse>()
    val taskRepositoryMock = mockk<TaskRepository>()
    val behandlingResultatRepositoryMock = mockk<BehandlingResultatRepository>()
    val persongrunnlagServiceMock = mockk<PersongrunnlagService>()
    val behandlingRepositoryMock = mockk<BehandlingRepository>()

    val søkerFnr = "12345678910"
    val barn1Fnr = "12345678911"
    val barn2Fnr = "12345678912"


    /*val fødselshendelseService = FødselshendelseService(infotrygdFeedServiceMock,
                                                        infotrygdBarnetrygdClientMock,
                                                        featureToggleServiceMock,
                                                        stegServiceMock,
                                                        vedtakServiceMock,
                                                        evaluerFiltreringsreglerForFødselshendelseMock,
                                                        taskRepositoryMock,
                                                        personopplysningerServiceMock,
                                                        behandlingResultatRepositoryMock,
                                                        persongrunnlagServiceMock,
                                                        behandlingRepositoryMock)*/

    @BeforeClass
    fun initMockk() {

    }

    @Test
    fun `Fødselshendelse med flere barn skal bli handlet riktig`(){
       fødselshendelseService.opprettBehandlingOgKjørReglerForFødselshendelse(NyBehandlingHendelse(
               søkersIdent = søkerFnr,
               morsIdent = søkerFnr,
               barnasIdenter = listOf(barn1Fnr, barn2Fnr)
       ))
    }
}