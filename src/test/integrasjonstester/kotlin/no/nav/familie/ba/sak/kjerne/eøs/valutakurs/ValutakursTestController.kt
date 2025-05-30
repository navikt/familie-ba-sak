package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.slåSammen
import no.nav.familie.ba.sak.kjerne.eøs.util.ValutakursBuilder
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/test/valutakurser")
@ProtectedWithClaims(issuer = "azuread")
@Validated
@Profile("!prod")
class ValutakursTestController(
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val valutakursService: ValutakursService,
) {
    @PutMapping(path = ["{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun endreValutakurser(
        @PathVariable behandlingId: Long,
        @RequestBody restValutakurser: Map<LocalDate, String>,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val behandlingIdObjekt = BehandlingId(behandlingId)
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingIdObjekt.id)!!
        restValutakurser.tilValutakurser(behandlingIdObjekt, personopplysningGrunnlag).forEach {
            valutakursService.oppdaterValutakurs(behandlingIdObjekt, it)
        }

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingIdObjekt.id)))
    }
}

private fun Map<LocalDate, String>.tilValutakurser(
    behandlingId: BehandlingId,
    personopplysningGrunnlag: PersonopplysningGrunnlag,
): Collection<Valutakurs> =
    this
        .map { (dato, tidslinje) ->
            val person = personopplysningGrunnlag.personer.first { it.fødselsdato == dato }
            ValutakursBuilder(dato.toYearMonth(), behandlingId)
                .medKurs(tidslinje, "EUR", person)
                .bygg()
        }.flatten()
        .slåSammen()
