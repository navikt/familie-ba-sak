package no.nav.familie.ba.sak.kjerne.e2e

import io.mockk.mockk
import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRequest
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.postForEntity
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
        "mock-pdl-e2e",
        "mock-oauth",
        "mock-arbeidsfordeling",
        "mock-infotrygd-feed",
        "mock-infotrygd-barnetrygd",
)
class JournalførOgBehandleFørstegangssøknadNasjonalTest : WebSpringAuthTestRunner() {

    @Test
    fun nasjonalFlyt() {

        val fagsak =
                restTemplate.postForEntity<Ressurs<Fagsak>>(hentUrl("/api/fagsaker"), HttpEntity<FagsakRequest>(FagsakRequest(
                        personIdent = scenario.søker.personIdent
                ), hentHeaders())).body

        assertEquals(Ressurs.Status.SUKSESS, fagsak?.status)

        val behandling =
                restTemplate.postForEntity<Ressurs<Fagsak>>(hentUrl("/api/behandlinger"), HttpEntity<NyBehandling>(NyBehandling(
                        kategori = BehandlingKategori.NASJONAL,
                        underkategori = BehandlingUnderkategori.ORDINÆR,
                        søkersIdent = scenario.søker.personIdent,
                        behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                        behandlingÅrsak = BehandlingÅrsak.SØKNAD
                ), hentHeaders())).body

        assertEquals(Ressurs.Status.SUKSESS, behandling?.status)

    }
}

@Configuration
class JournalførOgBehandleTestConfiguration {

    @Bean
    @Profile("mock-pdl-e2e")
    @Primary
    fun mockPersonopplysningerService(): PersonopplysningerService {
        val mockPersonopplysningerService = mockk<PersonopplysningerService>(relaxed = false)

        return byggE2EMock(mockPersonopplysningerService, scenario)
    }
}
