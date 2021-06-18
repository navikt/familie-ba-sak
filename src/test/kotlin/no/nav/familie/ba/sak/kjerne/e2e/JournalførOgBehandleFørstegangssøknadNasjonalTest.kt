package no.nav.familie.ba.sak.kjerne.e2e

import io.mockk.mockk
import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.NavnOgIdent
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

val scenario = Scenario(
        søker = ScenarioPerson(fødselsdato = LocalDate.parse("1996-01-12"), fornavn = "Mor", etternavn = "Søker"),
        barna = listOf(
                ScenarioPerson(fødselsdato = LocalDate.now().minusMonths(2),
                               fornavn = "Barn",
                               etternavn = "Barnesen")
        )
).byggRelasjoner()

@ActiveProfiles(
        "postgres",
        "mock-pdl-e2e-førstegangssøknad-nasjonal",
        "mock-oauth",
        "mock-arbeidsfordeling",
        "mock-infotrygd-feed",
        "mock-infotrygd-barnetrygd",
)
class JournalførOgBehandleFørstegangssøknadNasjonalTest : WebSpringAuthTestRunner() {

    fun familieBaSakKlient(): FamilieBaSakKlient = FamilieBaSakKlient(
            baSakUrl = hentUrl(""),
            restTemplate = restTemplate,
            headers = hentHeaders()
    )

    @Test
    fun nasjonalFlyt() {
        val fagsakId: Ressurs<String> = familieBaSakKlient().journalfør(
                journalpostId = "1234",
                oppgaveId = "5678",
                journalførendeEnhet = "4833",
                restJournalføring = lagMockRestJournalføring(bruker = NavnOgIdent(
                        navn = scenario.søker.navn,
                        id = scenario.søker.personIdent
                ))
        )

        assertEquals(Ressurs.Status.SUKSESS, fagsakId.status)

        val restFagsakEtterJournalføring = familieBaSakKlient().hentFagsak(fagsakId = fagsakId.data?.toLong()!!)
        generellAssertFagsak(restFagsak = restFagsakEtterJournalføring,
                             fagsakStatus = FagsakStatus.OPPRETTET,
                             behandlingStegType = StegType.REGISTRERE_SØKNAD)

        val aktivBehandling = hentAktivBehandling(restFagsak = restFagsakEtterJournalføring.data!!)
        val restRegistrerSøknad = RestRegistrerSøknad(søknad = lagSøknadDTO(søkerIdent = scenario.søker.personIdent,
                                                                            barnasIdenter = scenario.barna.map { it.personIdent }),
                                                      bekreftEndringerViaFrontend = false)
        val restFagsakEtterRegistrertSøknad: Ressurs<RestFagsak> =
                familieBaSakKlient().registrererSøknad(
                        behandlingId = aktivBehandling!!.behandlingId,
                        restRegistrerSøknad = restRegistrerSøknad
                )
        generellAssertFagsak(restFagsak = restFagsakEtterRegistrertSøknad,
                             fagsakStatus = FagsakStatus.OPPRETTET,
                             behandlingStegType = StegType.VILKÅRSVURDERING)

    }
}

@Configuration
class E2ETestConfiguration {

    @Bean
    @Profile("mock-pdl-e2e-førstegangssøknad-nasjonal")
    @Primary
    fun mockPersonopplysningerService(): PersonopplysningerService {
        val mockPersonopplysningerService = mockk<PersonopplysningerService>(relaxed = false)

        return byggE2EPersonopplysningerServiceMock(mockPersonopplysningerService, scenario)
    }
}