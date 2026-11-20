package no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg

import jakarta.validation.Valid
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.infotrygd.Personident
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.VedtakOmOvergangsstønadTask
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/overgangsstonad")
@Validated
class VedtakOmOvergangsstønadController(
    private val taskRepository: TaskRepositoryWrapper,
    private val tilgangService: TilgangService,
) {
    @PostMapping
    fun håndterVedtakOmOvergangsstønad(
        @Valid
        @RequestBody personIdent: Personident,
    ): Ressurs<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SYSTEM,
            handling = "håndtere vedtak om overgangsstønad",
        )
        taskRepository.save(VedtakOmOvergangsstønadTask.opprettTask(personIdent.ident))
        return Ressurs.success("Ok", "Ok")
    }
}
