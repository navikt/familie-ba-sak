package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util.vurderStatus
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
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
    private val featureToggleService: FeatureToggleService,
    private val kompetanseService: KompetanseService
) {
    @GetMapping(path = ["{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKompetanser(
        @PathVariable behandlingId: Long
    ): ResponseEntity<Ressurs<Collection<Kompetanse>>> {
        if (!featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS))
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()

        val kompetanser = kompetanseService.hentKompetanser(behandlingId).vurderStatus()
        return ResponseEntity.ok(Ressurs.success(kompetanser))
    }

    @PutMapping(path = ["{behandlingId}/{kompetanseId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterKompetanse(
        @PathVariable behandlingId: Long,
        @PathVariable kompetanseId: Long,
        @RequestBody kompetanse: Kompetanse
    ): ResponseEntity<Ressurs<Collection<Kompetanse>>> {
        if (!featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS))
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()

        val gjeldendeKompetanse = kompetanseService.hentKompetanse(kompetanseId)
        validerOppdatering(gjeldendeKompetanse, kompetanse)

        val oppdaterteKompetanser = kompetanseService.oppdaterKompetanse(kompetanseId, kompetanse)
            .vurderStatus()

        return ResponseEntity.ok(Ressurs.success(oppdaterteKompetanser))
    }

    @DeleteMapping(path = ["{behandlingId}/{kompetanseId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun slettKompetanse(
        @PathVariable behandlingId: Long,
        @PathVariable kompetanseId: Long
    ): ResponseEntity<Ressurs<Collection<Kompetanse>>> {
        if (!featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS))
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()

        val oppdaterteKompetanser = kompetanseService.slettKompetanse(kompetanseId)
            .vurderStatus()

        return ResponseEntity.ok(Ressurs.success(oppdaterteKompetanser))
    }

    private fun validerOppdatering(gjeldendeKompetanse: Kompetanse, oppdatertKompetanse: Kompetanse) {
        if (oppdatertKompetanse.fom == null)
            throw Feil("Manglende fra-og-med", httpStatus = HttpStatus.BAD_REQUEST)
        if (oppdatertKompetanse.fom > oppdatertKompetanse.tom)
            throw Feil("Fra-og-med er etter til-og-med", httpStatus = HttpStatus.BAD_REQUEST)
        if (oppdatertKompetanse.barnAktørIder.size == 0)
            throw Feil("Mangler barn", httpStatus = HttpStatus.BAD_REQUEST)
        if (oppdatertKompetanse.fom < gjeldendeKompetanse.fom)
            throw Feil("Setter fra-og-med tidligere", httpStatus = HttpStatus.BAD_REQUEST)
        if (!gjeldendeKompetanse.barnAktørIder.containsAll(oppdatertKompetanse.barnAktørIder))
            throw Feil("Oppdaterer barn som ikke er knyttet til kompetansen", httpStatus = HttpStatus.BAD_REQUEST)
    }
}
