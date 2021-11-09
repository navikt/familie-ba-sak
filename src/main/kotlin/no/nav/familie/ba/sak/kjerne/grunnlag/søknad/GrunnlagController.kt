package no.nav.familie.ba.sak.kjerne.grunnlag.søknad

import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/behandlinger")
@ProtectedWithClaims(issuer = "azuread")
class GrunnlagController(
    private val behandlingService: BehandlingService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val persongrunnlagService: PersongrunnlagService,
    private val tilbakestillService: TilbakestillBehandlingService
) {

    @PostMapping(path = ["/{behandlingId}/legg-til-barn"])
    fun leggTilBarnIPersonopplysningsgrunnlag(
        @PathVariable behandlingId: Long,
        @RequestBody
        leggTilBarnDto: LeggTilBarnDto
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val behandling = behandlingService.hent(behandlingId = behandlingId)
        persongrunnlagService.leggTilBarnIPersonopplysningsgrunnlag(
            behandling = behandling,
            nyttBarnIdent = leggTilBarnDto.barnIdent
        )
        tilbakestillService.initierOgSettBehandlingTilVilårsvurdering(behandling)
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandling.fagsak.id)))
    }

    class LeggTilBarnDto(val barnIdent: String)
}
