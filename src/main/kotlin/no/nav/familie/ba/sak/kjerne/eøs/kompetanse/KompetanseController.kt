package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.ekstern.restDomene.RestKompetanse
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.tilKompetanse
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/kompetanse")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class KompetanseController(
    private val kompetanseService: KompetanseService,
    private val personidentService: PersonidentService,
    private val utvidetBehandlingService: UtvidetBehandlingService
) {

    @PutMapping(path = ["{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterKompetanse(
        @PathVariable behandlingId: Long,
        @RequestBody restKompetanse: RestKompetanse
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val barnAktører = restKompetanse.barnIdenter.map { personidentService.hentAktør(it) }
        val kompetanse = restKompetanse.tilKompetanse(barnAktører = barnAktører)

        validerOppdatering(kompetanse)

        kompetanseService.oppdaterKompetanse(BehandlingId(behandlingId), kompetanse)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @DeleteMapping(path = ["{behandlingId}/{kompetanseId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun slettKompetanse(
        @PathVariable behandlingId: Long,
        @PathVariable kompetanseId: Long
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        kompetanseService.slettKompetanse(kompetanseId)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    private fun validerOppdatering(oppdatertKompetanse: Kompetanse) {
        if (oppdatertKompetanse.fom == null) {
            throw FunksjonellFeil("Manglende fra-og-med", httpStatus = HttpStatus.BAD_REQUEST)
        }
        if (oppdatertKompetanse.tom != null && oppdatertKompetanse.fom > oppdatertKompetanse.tom) {
            throw FunksjonellFeil("Fra-og-med er etter til-og-med", httpStatus = HttpStatus.BAD_REQUEST)
        }
        if (oppdatertKompetanse.barnAktører.isEmpty()) {
            throw FunksjonellFeil("Mangler barn", httpStatus = HttpStatus.BAD_REQUEST)
        }
    }
}
