package no.nav.familie.ba.sak.vedtak

import no.nav.familie.ba.sak.behandling.FagsakController
import no.nav.familie.kontrakt.Ressurs
import no.nav.familie.sikkerhet.OIDCUtil
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
@Unprotected
class ForhåndsvisVedtakController(
        private val oidcUtil: OIDCUtil
) {

    @GetMapping(path = ["/vedtaksbrev/{fagsakId}"])
    fun hentVedtaksBrevHtml(@PathVariable fagsakId: Long): Ressurs<String> {
        val saksbehandlerId = oidcUtil.getClaim("preferred_username")
        FagsakController.logger.info("{} henter vedtaksbrev", saksbehandlerId ?: "VL")

        //TODO: integration with DokGen

        return Ressurs.success("<H1>Vedtaksbrev</H1><br/>FagsakID= $fagsakId<br /><P>Backend API not implemented yet</P>");
    }
}