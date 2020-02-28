package no.nav.familie.ba.sak.behandling.fagsak

import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.util.randomFnr
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev", "mock-dokgen")
@Tag("integration")
class FagsakControllerTest(
        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val fagsakController: FagsakController
) {

    @MockBean
    lateinit var integrasjonTjeneste: IntegrasjonTjeneste

    @BeforeEach
    fun setup() {
        Mockito.`when`(integrasjonTjeneste.hentAktørId(ArgumentMatchers.anyString())).thenReturn(AktørId("1"))
    }

    @Test
    @Tag("integration")
    fun `Skal opprette fagsak`() {
        val fnr = randomFnr()

        fagsakController.nyFagsak(NyFagsak(personIdent = fnr))
        Assertions.assertEquals(fnr, fagsakService.hentForPersonident(PersonIdent(fnr))?.personIdent?.ident)
    }

    @Test
    @Tag("integration")
    fun `Skal kaste feil ved opprettelse av fagsak på person som allerede finnes`() {
        val fnr = randomFnr()

        val restFagsak = fagsakController.nyFagsak(NyFagsak(personIdent = fnr))
        Assertions.assertEquals(Ressurs.Status.SUKSESS, restFagsak.body?.status)
        Assertions.assertEquals(fnr, fagsakService.hentForPersonident(PersonIdent(fnr))?.personIdent?.ident)

        val feilendeRestFagsak = fagsakController.nyFagsak(NyFagsak(
                personIdent = fnr))
        Assertions.assertEquals(Ressurs.Status.FEILET, feilendeRestFagsak.body?.status)
        Assertions.assertEquals(
                "Kan ikke opprette fagsak på person som allerede finnes. Gå til fagsak ${restFagsak.body?.data?.id} for å se på saken",
                feilendeRestFagsak.body?.melding)
    }
}
