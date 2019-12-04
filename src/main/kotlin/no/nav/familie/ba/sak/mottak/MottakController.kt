package no.nav.familie.ba.sak.mottak

import no.nav.familie.ba.sak.behandling.BehandlingslagerService
import no.nav.familie.ba.sak.behandling.FagsakController
import no.nav.familie.ba.sak.behandling.FagsakService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.Fagsak
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakt.Ressurs
import no.nav.familie.sikkerhet.OIDCUtil
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@ProtectedWithClaims( issuer = "azuread" )
class MottakController (
        private val oidcUtil: OIDCUtil,
        private val behandlingslagerService: BehandlingslagerService,
        private val fagsakService: FagsakService
) {
    @PostMapping(path = ["/behandling/opprett"])
    fun nyBehandling(@RequestBody nyBehandling: NyBehandling): Ressurs<Fagsak> {
        val saksbehandlerId = oidcUtil.getClaim("preferred_username")
        FagsakController.logger.info("{} oppretter ny behandling", saksbehandlerId ?: "VL")

        //final var søkerAktørId = oppslagTjeneste.hentAktørId(fødselsnummer);

        val personIdent = PersonIdent(nyBehandling.fødselsnummer)
        val fagsak = when(val it = fagsakService.hentFagsakForPersonident(personIdent)) {
            null -> Fagsak(null, AktørId("1"), personIdent)
            else -> it
        }

        fagsakService.lagreFagsak(fagsak)
        val behandling = Behandling(null, fagsak, nyBehandling.journalpostID, "LagMeg")
        behandlingslagerService.lagreBehandling(behandling)

        return Ressurs.success( data = fagsak )
    }
}

data class NyBehandling(val fødselsnummer: String, val fødselsnummerBarn: String, val journalpostID: String)