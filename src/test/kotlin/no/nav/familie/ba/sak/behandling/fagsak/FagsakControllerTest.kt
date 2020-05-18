package no.nav.familie.ba.sak.behandling.fagsak

import io.mockk.every
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.domene.IdentInformasjon
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
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
        private val fagsakController: FagsakController,

        @Autowired
        private val mockIntegrasjonClient: IntegrasjonClient
) {

    @Test
    @Tag("integration")
    fun `Skal opprette fagsak`() {
        val fnr = randomFnr()

        every {
            mockIntegrasjonClient.hentIdenter(fnr)
        } returns listOf(IdentInformasjon(ident = fnr, historisk = true, gruppe = "FOLKEREGISTERIDENT"))

        fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr))
        assertEquals(fnr, fagsakService.hent(PersonIdent(fnr))?.hentAktivIdent()?.ident)
    }

    @Test
    @Tag("integration")
    fun `Skal opprette fagsak med aktørid`() {
        val aktørId = randomAktørId()

        val response = fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = null, aktørId = aktørId.id))
        val restFagsak = response.body?.data
        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(FagsakStatus.OPPRETTET, restFagsak?.status)
        assertNotNull(restFagsak?.søkerFødselsnummer)
    }

    @Test
    @Tag("integration")
    fun `Skal returnere eksisterende fagsak på person som allerede finnes`() {
        val fnr = randomFnr()

        every {
            mockIntegrasjonClient.hentIdenter(fnr)
        } returns listOf(
                IdentInformasjon(ident = fnr, historisk = true, gruppe = "FOLKEREGISTERIDENT"))

        val nyRestFagsak = fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr))
        assertEquals(Ressurs.Status.SUKSESS, nyRestFagsak.body?.status)
        assertEquals(fnr, fagsakService.hent(PersonIdent(fnr))?.hentAktivIdent()?.ident)

        val eksisterendeRestFagsak = fagsakController.hentEllerOpprettFagsak(FagsakRequest(
                personIdent = fnr))
        assertEquals(Ressurs.Status.SUKSESS, eksisterendeRestFagsak.body?.status)
        assertEquals(eksisterendeRestFagsak.body!!.data!!.id, nyRestFagsak.body!!.data!!.id)
    }

    @Test
    @Tag("integration")
    fun `Skal returnere eksisterende fagsak på person som allerede finnes med gammel ident`() {
        val fnr = randomFnr()
        val nyttFnr = randomFnr()

        every {
            mockIntegrasjonClient.hentIdenter(fnr)
        } returns listOf(
                IdentInformasjon(ident = fnr, historisk = true, gruppe = "FOLKEREGISTERIDENT"))

        val nyRestFagsak = fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr))
        assertEquals(Ressurs.Status.SUKSESS, nyRestFagsak.body?.status)
        assertEquals(fnr, fagsakService.hent(PersonIdent(fnr))?.hentAktivIdent()?.ident)

        every {
            mockIntegrasjonClient.hentIdenter(nyttFnr)
        } returns listOf(
                IdentInformasjon(ident = fnr, historisk = true, gruppe = "FOLKEREGISTERIDENT"),
                IdentInformasjon(ident = nyttFnr, historisk = false, gruppe = "FOLKEREGISTERIDENT"))

        val eksisterendeRestFagsak = fagsakController.hentEllerOpprettFagsak(FagsakRequest(
                personIdent = nyttFnr))
        assertEquals(Ressurs.Status.SUKSESS, eksisterendeRestFagsak.body?.status)
        assertEquals(eksisterendeRestFagsak.body!!.data!!.id, nyRestFagsak.body!!.data!!.id)
        assertEquals(nyttFnr, eksisterendeRestFagsak.body!!.data?.søkerFødselsnummer)

    }

    @Test
    @Tag("integration")
    fun `Skal returnere eksisterende fagsak på person som allerede finnes basert på aktørid`() {
        val aktørId = randomAktørId()

        every {
            mockIntegrasjonClient.hentAktivPersonIdent(aktørId.id)
        } returns PersonIdent("123")

        val nyRestFagsak = fagsakController.hentEllerOpprettFagsak(
                FagsakRequest(personIdent = null, aktørId = aktørId.id)
        )
        assertEquals(Ressurs.Status.SUKSESS, nyRestFagsak.body?.status)
//        Assertions.assertEquals(aktørId,
//                fagsakService.hent(PersonIdent(nyRestFagsak.body?.data!!.søkerFødselsnummer))?.aktørId) //TODO bytte til å teste med fnr

        val eksisterendeRestFagsak = fagsakController.hentEllerOpprettFagsak(FagsakRequest(
                personIdent = null, aktørId = aktørId.id))
        assertEquals(Ressurs.Status.SUKSESS, eksisterendeRestFagsak.body?.status)
        assertEquals(eksisterendeRestFagsak.body!!.data!!.id, nyRestFagsak.body!!.data!!.id)
    }
}
