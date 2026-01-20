package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import jakarta.validation.Valid
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.UtvidetBehandlingDto
import no.nav.familie.ba.sak.ekstern.restDomene.ValutakursDto
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
    ): ResponseEntity<Ressurs<UtvidetBehandlingDto>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.BESLUTTER,
            handling = "Oppdaterer valutakurser og simulering automatisk",
        )
        tilgangService.validerErPåBeslutteVedtakSteg(behandlingId)

        automatiskOppdaterValutakursService.oppdaterValutakurserOgSimulering(BehandlingId(behandlingId))

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = behandlingId)))
    }

    @PutMapping(path = ["{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterValutakurs(
        @PathVariable behandlingId: Long,
        @Valid @RequestBody valutakursDto: ValutakursDto,
    ): ResponseEntity<Ressurs<UtvidetBehandlingDto>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppdaterer valutakurs",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        val barnAktører = valutakursDto.barnIdenter.map { personidentService.hentAktør(it) }

        val valutaKurs =
            if (skalManueltSetteValutakurs(valutakursDto)) {
                valutakursDto.tilValutakurs(barnAktører)
            } else {
                oppdaterValutakursMedKursFraECB(valutakursDto, valutakursDto.tilValutakurs(barnAktører = barnAktører))
            }

        valutakursService.oppdaterValutakurs(BehandlingId(behandlingId), valutaKurs)
        automatiskOppdaterValutakursService.oppdaterValutakurserEtterEndringstidspunkt(BehandlingId(behandlingId))
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = behandlingId)))
    }

    @DeleteMapping(path = ["{behandlingId}/{valutakursId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun slettValutakurs(
        @PathVariable behandlingId: Long,
        @PathVariable valutakursId: Long,
    ): ResponseEntity<Ressurs<UtvidetBehandlingDto>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.DELETE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Sletter valutakurs",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        valutakursService.slettValutakurs(BehandlingId(behandlingId), valutakursId)

        automatiskOppdaterValutakursService.oppdaterValutakurserEtterEndringstidspunkt(BehandlingId(behandlingId))

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = behandlingId)))
    }

    private fun oppdaterValutakursMedKursFraECB(
        valutakursDto: ValutakursDto,
        valutakurs: Valutakurs,
    ) = if (valutakursErEndret(valutakursDto, valutakursService.hentValutakurs(valutakursDto.id))) {
        valutakurs.copy(
            kurs =
                ecbService.hentValutakurs(
                    valutakursDto.valutakode!!,
                    valutakursDto.valutakursdato!!,
                ),
        )
    } else {
        valutakurs
    }

    /**
     * Sjekker om valuta er Islandske Kroner og kursdato er før 01.02.2018
     */
    private fun skalManueltSetteValutakurs(valutakursDto: ValutakursDto): Boolean =
        valutakursDto.valutakursdato != null &&
            valutakursDto.valutakode == "ISK" &&
            valutakursDto.valutakursdato.isBefore(
                LocalDate.of(2018, 2, 1),
            )

    /**
     * Sjekker om *valutakursDto* inneholder nødvendige verdier og sammenligner disse med *eksisterendeValutakurs*
     */
    private fun valutakursErEndret(
        valutakursDto: ValutakursDto,
        eksisterendeValutakurs: Valutakurs,
    ): Boolean = valutakursDto.valutakode != null && valutakursDto.valutakursdato != null && (eksisterendeValutakurs.valutakursdato != valutakursDto.valutakursdato || eksisterendeValutakurs.valutakode != valutakursDto.valutakode)

    @PutMapping(path = ["behandlinger/{behandlingId}/endre-vurderingsstrategi-til/{vurderingsstrategiForValutakurser}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    private fun endreVurderingsstrategiForValutakurser(
        @PathVariable behandlingId: Long,
        @PathVariable vurderingsstrategiForValutakurser: VurderingsstrategiForValutakurser,
    ): ResponseEntity<Ressurs<UtvidetBehandlingDto>> {
        automatiskOppdaterValutakursService.endreVurderingsstrategiForValutakurser(behandlingId = BehandlingId(behandlingId), nyStrategi = vurderingsstrategiForValutakurser)
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = behandlingId)))
    }
}
