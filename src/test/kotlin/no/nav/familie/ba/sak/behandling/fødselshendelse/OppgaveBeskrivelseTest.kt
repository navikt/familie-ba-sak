package no.nav.familie.ba.sak.behandling.fødselshendelse

import io.mockk.mockk
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
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.specifications.Spesifikasjon
import org.junit.Assert
import org.junit.jupiter.api.Test

class OppgaveBeskrivelseTest{
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
    val fødselshendelseService = FødselshendelseService(infotrygdFeedServiceMock,
                                                        infotrygdBarnetrygdClientMock,
                                                        featureToggleServiceMock,
                                                        stegServiceMock,
                                                        vedtakServiceMock,
                                                        evaluerFiltreringsreglerForFødselshendelseMock,
                                                        taskRepositoryMock,
                                                        personopplysningerServiceMock,
                                                        behandlingResultatRepositoryMock,
                                                        persongrunnlagServiceMock,
                                                        behandlingRepositoryMock)

    @Test
    fun `hentBegrunnelseFraFiltreringsregler() skal returnere begrunnelse av første regel som feilet`() {
        val testEvaluering0 = testSpesifikasjoner.evaluer(TestFakta(morHarGyldigFodselsnummer = false, barnetHarGyldigFodselsnummer = false, barnetErUnder6Mnd = true))
        val beskrivelse0 = fødselshendelseService.hentBegrunnelseFraFiltreringsregler(testEvaluering0)
        Assert.assertEquals("mor har ikke gyldig fødselsnummer", beskrivelse0)

        val testEvaluering1 = testSpesifikasjoner.evaluer(TestFakta(morHarGyldigFodselsnummer = true, barnetHarGyldigFodselsnummer = false, barnetErUnder6Mnd = false))
        val beskrivelse1 = fødselshendelseService.hentBegrunnelseFraFiltreringsregler(testEvaluering1)
        Assert.assertEquals("barnet har ikke gyldig fødselsnummer", beskrivelse1)

        val testEvaluering2 = testSpesifikasjoner.evaluer(TestFakta(morHarGyldigFodselsnummer = true, barnetHarGyldigFodselsnummer = true, barnetErUnder6Mnd = false))
        val beskrivelse2 = fødselshendelseService.hentBegrunnelseFraFiltreringsregler(testEvaluering2)
        Assert.assertEquals("barnet er over 6 måneder", beskrivelse2)
    }

    @Test
    fun `hentBegrunnelseFraFiltreringsregler() skal returnere null dersom ingen regler feiler`(){
        val testEvaluering = testSpesifikasjoner.evaluer(TestFakta(morHarGyldigFodselsnummer = true, barnetHarGyldigFodselsnummer = true, barnetErUnder6Mnd = true))
        val beskrivelse = fødselshendelseService.hentBegrunnelseFraFiltreringsregler(testEvaluering)
        Assert.assertNull(beskrivelse)
    }

    class TestFakta(
        val morHarGyldigFodselsnummer: Boolean,
        val barnetHarGyldigFodselsnummer: Boolean,
        val barnetErUnder6Mnd: Boolean
    )

    companion object {

        private fun morHarGyldigFodselsnummer(input: TestFakta): Evaluering {
            return if (input.morHarGyldigFodselsnummer)
                Evaluering.ja("mor har gyldig fødselsnummer")
            else Evaluering.nei("mor har ikke gyldig fødselsnummer")
        }

        private fun barnetHarGyldigFodselsnummer(input: TestFakta): Evaluering {
            return if (input.barnetHarGyldigFodselsnummer)
                Evaluering.ja("barnet har gyldig fødselsnummer")
            else Evaluering.nei("barnet har ikke gyldig fødselsnummer")
        }

        private fun barnetErUnder6Mnd(input: TestFakta): Evaluering {
            return if (input.barnetErUnder6Mnd)
                Evaluering.ja("barnet er under 6 måneder")
            else Evaluering.nei("barnet er over 6 måneder")
        }

        val testSpesifikasjoner = Spesifikasjon<TestFakta>(
                "Test Regel 0",
                "MOR_HAR_GYLDIG_FOEDSELSNUMMER",
                implementasjon = {morHarGyldigFodselsnummer(this)}
        ) og
                Spesifikasjon<TestFakta>(
                        "Test Regel 1",
                        "BARNET_HAR_GYLDIG_FOEDSELSNUMMER",
                        implementasjon = {barnetHarGyldigFodselsnummer(this)}
                ) og
                Spesifikasjon<TestFakta>(
                        "Test Regel 2",
                        "BARNET_ER_UNDER_6_MND",
                        implementasjon = {barnetErUnder6Mnd(this)}
                )
    }
}