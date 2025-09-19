package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.Fødselsnummer
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Status.KLAR_TIL_PLUKK
import no.nav.familie.prosessering.domene.Status.UBEHANDLET
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/svalbardtillegg")
@ProtectedWithClaims(issuer = "azuread")
class SvalbardtilleggController(
    private val tilgangService: TilgangService,
    private val opprettTaskService: OpprettTaskService,
    private val fagsakService: FagsakService,
    private val personidentService: PersonidentService,
    private val taskService: TaskService,
) {
    @PostMapping("/vurder-svalbardtillegg")
    fun vurderSvalbardtillegg(
        @RequestBody personIdent: PersonIdent,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.validerTilgangTilPersoner(
            personIdenter = listOf(personIdent.ident),
            event = AuditLoggerEvent.UPDATE,
        )

        // Valider personident
        Fødselsnummer(personIdent.ident)

        val aktør = personidentService.hentAktør(personIdent.ident)
        fagsakService
            .finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(aktør)
            .filter { it.type in FAGSAKTYPER_DER_SVALBARDTILLEGG_KAN_AUTOVEDTAS }
            .forEach { fagsak ->
                val finnesUkjørtSvalbardtilleggTaskForFagsak =
                    taskService
                        .finnAlleTaskerMedPayloadOgType(payload = fagsak.id.toString(), type = AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE)
                        .any { it.status in setOf(UBEHANDLET, KLAR_TIL_PLUKK) }

                if (!finnesUkjørtSvalbardtilleggTaskForFagsak) {
                    opprettTaskService.opprettAutovedtakSvalbardtilleggTasker(listOf(fagsak.id))
                }
            }

        secureLogger.info("Trigget vurdering av Svalbardtillegg for personIdent: ${personIdent.ident}")

        return ResponseEntity.ok(Ressurs.success("Trigget vurdering av Svalbardtillegg"))
    }
}
