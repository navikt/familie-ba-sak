package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.Satskjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

@RestController
@RequestMapping("/api/satsendring")
@ProtectedWithClaims(issuer = "azuread")
class SatsendringController(
    private val satskjøringRepository: SatskjøringRepository,
    private val opprettTaskService: OpprettTaskService
) {

    private val logger = LoggerFactory.getLogger(SatsendringController::class.java)

    @GetMapping(path = ["/kjorsatsendring/{fagsakId}"])
    fun utførSatsendringPåFagsak(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<String>> {
        satskjøringRepository.save(Satskjøring(fagsakId = fagsakId))
        opprettTaskService.opprettSatsendringTask(fagsakId, YearMonth.of(2023, 3))
        return ResponseEntity.ok(Ressurs.success("Trigget satsendring for fagsak $fagsakId"))
    }
}
