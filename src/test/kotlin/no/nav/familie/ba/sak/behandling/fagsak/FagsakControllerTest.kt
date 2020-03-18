package no.nav.familie.ba.sak.behandling.fagsak

import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
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

    @Test
    @Tag("integration")
    fun `Skal opprette fagsak`() {
        val fnr = randomFnr()

        fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr))
        Assertions.assertEquals(fnr, fagsakService.hent(PersonIdent(fnr))?.personIdent?.ident)
    }

    @Test
    @Tag("integration")
    fun `Skal returnere eksisterende fagsak på person som allerede finnes`() {
        val fnr = randomFnr()

        val nyRestFagsak = fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr))
        Assertions.assertEquals(Ressurs.Status.SUKSESS, nyRestFagsak.body?.status)
        Assertions.assertEquals(fnr, fagsakService.hent(PersonIdent(fnr))?.personIdent?.ident)

        val eksisterendeRestFagsak = fagsakController.hentEllerOpprettFagsak(FagsakRequest(
                personIdent = fnr))
        Assertions.assertEquals(Ressurs.Status.SUKSESS, eksisterendeRestFagsak.body?.status)
        Assertions.assertEquals(eksisterendeRestFagsak.body!!.data!!.id, nyRestFagsak.body!!.data!!.id)
    }
}
