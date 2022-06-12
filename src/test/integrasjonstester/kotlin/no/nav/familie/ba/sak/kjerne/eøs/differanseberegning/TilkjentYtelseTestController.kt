package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/test/tilkjentytelse")
@ProtectedWithClaims(issuer = "azuread")
@Validated
@Profile("!prod")
class TilkjentYtelseTestController(
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val beregningService: BeregningService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
) {

    @PostMapping(path = ["{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun lagInitiellTilkjentYtelse(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)!!

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @PutMapping(path = ["{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterEndretUtebetalingAndeler(
        @PathVariable behandlingId: Long,
        @RequestBody restDeltBosted: Map<LocalDate, String>
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)!!

        val tilkjentYtelse = beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        restDeltBosted.tilEndretUtbetalingAndeler(personopplysningGrunnlag, tilkjentYtelse).forEach {
            endretUtbetalingAndelService.opprettTomEndretUtbetalingAndelOgOppdaterTilkjentYtelse(behandling)
            beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag, it)
        }

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }
}

private fun Map<LocalDate, String>.tilEndretUtbetalingAndeler(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    tilkjentYtelse: TilkjentYtelse
): Collection<EndretUtbetalingAndel> {
    return this.map { (dato, tidslinje) ->
        val person = personopplysningGrunnlag.personer.first { it.fødselsdato == dato }
        DeltBostedBuilder(dato.tilMånedTidspunkt(), tilkjentYtelse)
            .medDeltBosted(tidslinje, person)
            .bygg().tilEndreteUtebetalingAndeler()
    }.flatten()
}
