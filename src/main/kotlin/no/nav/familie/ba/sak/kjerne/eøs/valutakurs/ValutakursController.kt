package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import jakarta.validation.Valid
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.RestValutakurs
import no.nav.familie.ba.sak.ekstern.restDomene.tilValutakurs
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/differanseberegning/valutakurs")
@ProtectedWithClaims(issuer = "azuread")
class ValutakursController(
    private val tilgangService: TilgangService,
    private val valutakursService: ValutakursService,
    private val personidentService: PersonidentService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val ecbService: ECBService,
    private val automatiskOppdaterValutakursService: AutomatiskOppdaterValutakursService,
) {
    @PutMapping(path = ["{behandlingId}/oppdater-valutakurser-og-simulering-automatisk"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterValutakurserOgSimuleringAutomatisk(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.BESLUTTER,
            handling = "Oppdaterer valutakurser og simulering automatisk",
        )
        tilgangService.validerErPåBeslutteVedtakSteg(behandlingId)

        automatiskOppdaterValutakursService.oppdaterValutakurserOgSimulering(BehandlingId(behandlingId))

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @PutMapping(path = ["{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterValutakurs(
        @PathVariable behandlingId: Long,
        @Valid @RequestBody restValutakurs: RestValutakurs,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppdaterer valutakurs",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        val barnAktører = restValutakurs.barnIdenter.map { personidentService.hentAktør(it) }

        val valutaKurs =
            if (skalManueltSetteValutakurs(restValutakurs)) {
                restValutakurs.tilValutakurs(barnAktører)
            } else {
                oppdaterValutakursMedKursFraECB(restValutakurs, restValutakurs.tilValutakurs(barnAktører = barnAktører))
            }

        valutakursService.oppdaterValutakurs(BehandlingId(behandlingId), valutaKurs)
        automatiskOppdaterValutakursService.oppdaterValutakurserEtterEndringstidspunkt(BehandlingId(behandlingId))
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @DeleteMapping(path = ["{behandlingId}/{valutakursId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun slettValutakurs(
        @PathVariable behandlingId: Long,
        @PathVariable valutakursId: Long,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.DELETE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Sletter valutakurs",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        valutakursService.slettValutakurs(BehandlingId(behandlingId), valutakursId)

        automatiskOppdaterValutakursService.oppdaterValutakurserEtterEndringstidspunkt(BehandlingId(behandlingId))

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    private fun oppdaterValutakursMedKursFraECB(
        restValutakurs: RestValutakurs,
        valutakurs: Valutakurs,
    ) = if (valutakursErEndret(restValutakurs, valutakursService.hentValutakurs(restValutakurs.id))) {
        valutakurs.copy(
            kurs =
                ecbService.hentValutakurs(
                    restValutakurs.valutakode!!,
                    restValutakurs.valutakursdato!!,
                ),
        )
    } else {
        valutakurs
    }

    /**
     * Sjekker om valuta er Islandske Kroner og kursdato er før 01.02.2018
     */
    private fun skalManueltSetteValutakurs(restValutakurs: RestValutakurs): Boolean =
        restValutakurs.valutakursdato != null &&
            restValutakurs.valutakode == "ISK" &&
            restValutakurs.valutakursdato.isBefore(
                LocalDate.of(2018, 2, 1),
            )

    /**
     * Sjekker om *restValutakurs* inneholder nødvendige verdier og sammenligner disse med *eksisterendeValutakurs*
     */
    private fun valutakursErEndret(
        restValutakurs: RestValutakurs,
        eksisterendeValutakurs: Valutakurs,
    ): Boolean = restValutakurs.valutakode != null && restValutakurs.valutakursdato != null && (eksisterendeValutakurs.valutakursdato != restValutakurs.valutakursdato || eksisterendeValutakurs.valutakode != restValutakurs.valutakode)

    @PutMapping(path = ["behandlinger/{behandlingId}/endre-vurderingsstrategi-til/{vurderingsstrategiForValutakurser}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    private fun endreVurderingsstrategiForValutakurser(
        @PathVariable behandlingId: Long,
        @PathVariable vurderingsstrategiForValutakurser: VurderingsstrategiForValutakurser,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        automatiskOppdaterValutakursService.endreVurderingsstrategiForValutakurser(behandlingId = BehandlingId(behandlingId), nyStrategi = vurderingsstrategiForValutakurser)
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }
}
