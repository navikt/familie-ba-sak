package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestKompetanse
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.tilKompetanse
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MAX_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MIN_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
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
    private val featureToggleService: FeatureToggleService,
    private val kompetanseService: KompetanseService,
    private val personidentService: PersonidentService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val tilkjentYtelseRepository: AndelTilkjentYtelseRepository
) {

    @PutMapping(path = ["{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterKompetanse(
        @PathVariable behandlingId: Long,
        @RequestBody restKompetanse: RestKompetanse
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val barnAktører = restKompetanse.barnIdenter.map { personidentService.hentAktør(it) }
        val kompetanse = restKompetanse.tilKompetanse(barnAktører = barnAktører)

        validerUtvidedEøsOgSekundærland(kompetanse, behandlingId)
        validerOppdatering(kompetanse)

        kompetanseService.oppdaterKompetanse(BehandlingId(behandlingId), kompetanse)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @Deprecated("Bruk endepunkt som bare krever behandlingId")
    @PutMapping(path = ["{behandlingId}/{kompetanseId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterKompetanseOld(
        @PathVariable behandlingId: Long,
        @PathVariable kompetanseId: Long,
        @RequestBody restKompetanse: RestKompetanse
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val barnAktører = restKompetanse.barnIdenter.map { personidentService.hentAktør(it) }
        val kompetanse = restKompetanse.tilKompetanse(barnAktører = barnAktører)

        val gjeldendeKompetanse = kompetanseService.hentKompetanse(kompetanseId)
        validerOppdatering(gjeldendeKompetanse, kompetanse)

        val behandlingId = BehandlingId(gjeldendeKompetanse.behandlingId)
        kompetanseService.oppdaterKompetanse(behandlingId, kompetanse)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId.id)))
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
        if (oppdatertKompetanse.resultat == KompetanseResultat.TO_PRIMÆRLAND && !featureToggleService.isEnabled(
                FeatureToggleConfig.KAN_BEHANDLE_EØS_TO_PRIMERLAND
            )
        ) {
            throw FunksjonellFeil("To primærland er ikke støttet", httpStatus = HttpStatus.BAD_REQUEST)
        }
    }

    @Deprecated("Unødvendig med validering av gjeldende kompetanse")
    private fun validerOppdatering(gjeldendeKompetanse: Kompetanse, oppdatertKompetanse: Kompetanse) {
        validerOppdatering(oppdatertKompetanse)

        if (oppdatertKompetanse.fom!! < (gjeldendeKompetanse.fom ?: MIN_MÅNED)) {
            throw FunksjonellFeil("Setter fra-og-med tidligere", httpStatus = HttpStatus.BAD_REQUEST)
        }
        if ((oppdatertKompetanse.tom ?: MAX_MÅNED) > (gjeldendeKompetanse.tom ?: MAX_MÅNED)) {
            throw FunksjonellFeil("Setter til-og-med senere ", httpStatus = HttpStatus.BAD_REQUEST)
        }
        if (!gjeldendeKompetanse.barnAktører.containsAll(oppdatertKompetanse.barnAktører)) {
            throw FunksjonellFeil(
                "Oppdaterer barn som ikke er knyttet til kompetansen",
                httpStatus = HttpStatus.BAD_REQUEST
            )
        }
    }

    private fun validerUtvidedEøsOgSekundærland(kompetanse: Kompetanse, behandlingId: Long) {
        if (kompetanse.resultat == KompetanseResultat.NORGE_ER_SEKUNDÆRLAND && !featureToggleService.isEnabled(
                FeatureToggleConfig.KAN_BEHANDLE_UTVIDET_EØS_SEKUNDÆRLAND
            )
        ) {
            val utvidetEllerSmåbarnstillegg =
                tilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)
                    .any { andelTilkjentYtelse -> andelTilkjentYtelse.erSøkersAndel() }
            if (utvidetEllerSmåbarnstillegg) {
                throw FunksjonellFeil(
                    "Støtter foreløpig ikke utvidet barnetrygd og/eller småbarnstillegg i kombinasjon med sekundærland.",
                    "Søker har utvidet barnetrygd og/eller småbarnstillegg. Dette er ikke støttet i sekundærlandssaker enda. Ta kontakt med Team Familie",
                    HttpStatus.BAD_REQUEST
                )
            }
        }
    }
}
