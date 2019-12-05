package no.nav.familie.ba.sak.mottak

import no.nav.familie.ba.sak.behandling.BehandlingslagerService
import no.nav.familie.ba.sak.behandling.FagsakController
import no.nav.familie.ba.sak.behandling.FagsakService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.Fagsak
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakt.Ressurs
import no.nav.familie.sikkerhet.OIDCUtil
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.ThreadLocalRandom
import kotlin.streams.asSequence

@RestController
@RequestMapping("/api")
@ProtectedWithClaims( issuer = "azuread" )
class MottakController (
        private val oidcUtil: OIDCUtil,
        private val behandlingslagerService: BehandlingslagerService,
        private val fagsakService: FagsakService,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository
) {
    const val STRING_LENGTH = 10;
    private val charPool : List<Char> = ('0'..'9')

    @PostMapping(path = ["/behandling/opprett"])
    fun opprettBehandling(@RequestBody nyBehandling: NyBehandling): Ressurs<RestFagsak> {
        val saksbehandlerId = oidcUtil.getClaim("preferred_username")
        FagsakController.logger.info("{} oppretter ny behandling", saksbehandlerId ?: "VL")

        //final var søkerAktørId = oppslagTjeneste.hentAktørId(fødselsnummer);

        val søkerPersonIdent = PersonIdent(nyBehandling.fødselsnummer)
        val fagsak = when(val it = fagsakService.hentFagsakForPersonident(søkerPersonIdent)) {
            null -> Fagsak(null, AktørId("1"), søkerPersonIdent)
            else -> it
        }

        fagsakService.lagreFagsak(fagsak)
        val behandling = Behandling(null, fagsak, nyBehandling.journalpostID, ThreadLocalRandom.current()
                .ints(STRING_LENGTH.toLong(), 0, charPool.size)
                .asSequence()
                .map(charPool::get)
                .joinToString(""))

        behandlingslagerService.lagreBehandling(behandling)

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandling.id)

        val søker = Person( personIdent = PersonIdent(nyBehandling.fødselsnummer), type = PersonType.SØKER)
        personopplysningGrunnlag.leggTilPerson(søker)

        nyBehandling.barnasFødselsnummer.map {
            personopplysningGrunnlag.leggTilPerson( Person( personIdent = PersonIdent(it), type = PersonType.BARN) )
        }
        personopplysningGrunnlag.setAktiv(true)
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag);

        return fagsakService.hentRestFagsak(fagsakId = fagsak.id)
    }
}

data class NyBehandling(val fødselsnummer: String, val barnasFødselsnummer: Array<String>, val journalpostID: String)