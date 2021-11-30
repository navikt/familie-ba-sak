package no.nav.familie.ba.sak.integrasjoner.infotrygd

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.task.VedtakOmOvergangsstønadTask
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/infotrygd")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class InfotrygdController(
    private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
    private val infotrygdService: InfotrygdService,
    private val taskRepository: TaskRepositoryWrapper
) {

    @PostMapping(path = ["/hent-infotrygdsaker-for-soker"])
    fun hentInfotrygdsakerForSøker(@RequestBody personIdent: Personident): ResponseEntity<Ressurs<RestInfotrygdsaker>> {
        val infotrygdsaker = infotrygdService.hentMaskertRestInfotrygdsakerVedManglendeTilgang(personIdent.ident)
            ?: RestInfotrygdsaker(infotrygdService.hentInfotrygdsakerForSøker(personIdent.ident).bruker)

        return ResponseEntity.ok(Ressurs.success(infotrygdsaker))
    }

    @PostMapping(path = ["/hent-infotrygdstonader-for-soker"])
    fun hentInfotrygdstønaderForSøker(@RequestBody personIdent: Personident): ResponseEntity<Ressurs<RestInfotrygdstønader>> {
        val infotrygdstønader = infotrygdService.hentMaskertRestInfotrygdstønaderVedManglendeTilgang(personIdent.ident)
            ?: RestInfotrygdstønader(infotrygdService.hentInfotrygdstønaderForSøker(personIdent.ident).bruker)

        return ResponseEntity.ok(Ressurs.success(infotrygdstønader))
    }

    @PostMapping(path = ["/har-lopende-sak"])
    fun harLøpendeSak(@RequestBody personIdent: Personident): ResponseEntity<Ressurs<RestLøpendeSak>> {
        val harLøpendeSak = infotrygdBarnetrygdClient.harLøpendeSakIInfotrygd(listOf(personIdent.ident))
        return ResponseEntity.ok(Ressurs.success(RestLøpendeSak(harLøpendeSak)))
    }

    @PostMapping(path = ["/overgangsstonad"])
    fun håndterVedtakOmOvergangsstønad(@RequestBody personIdent: Personident): Ressurs<String> {
        taskRepository.save(VedtakOmOvergangsstønadTask.opprettTask(personIdent.ident))
        return Ressurs.success("Ok", "Ok")
    }
}

class Personident(val ident: String)

class RestInfotrygdsaker(
    val saker: List<Sak> = emptyList(),
    val adressebeskyttelsegradering: ADRESSEBESKYTTELSEGRADERING? = null,
    val harTilgang: Boolean = true
)

class RestInfotrygdstønader(
    val stønader: List<Stønad> = emptyList(),
    val adressebeskyttelsegradering: ADRESSEBESKYTTELSEGRADERING? = null,
    val harTilgang: Boolean = true
)

class RestLøpendeSak(val harLøpendeSak: Boolean)
