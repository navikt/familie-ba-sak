package no.nav.familie.ba.sak

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.RestPersonInfo
import no.nav.familie.ba.sak.common.RessursUtils
import no.nav.familie.ba.sak.oppgave.domene.DataForManuellJournalføring
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.validering.PersontilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/mock")
@Unprotected
class MockController {

    val objectMapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())

    init {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    @GetMapping(path = ["/oppgave/{oppgaveId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentDataForManuellJournalføring(@PathVariable(name = "oppgaveId") oppgaveId: Long)
            : ResponseEntity<Ressurs<DataForManuellJournalføring>> {
        val mockJournalpostJson = MockController::class.java.getResource("/journalføring/journalpost_453636379.json").readText()
        val mockJournalpost = objectMapper.readValue(mockJournalpostJson, Journalpost::class.java)
        val dataForManuellJournalføring = DataForManuellJournalføring(
                oppgave = Oppgave(id = 0),
                person = null,
                fagsak = null,
                journalpost = mockJournalpost
        )
        LOG.info(objectMapper.writeValueAsString(dataForManuellJournalføring))
        return ResponseEntity.ok(Ressurs.success(dataForManuellJournalføring))
    }

    var counter = 0

    @GetMapping("/journalpost/{journalpostId}/hent/{dokumentInfoId}")
    fun hentDokument(@PathVariable journalpostId: String,
                     @PathVariable dokumentInfoId: String)
            : ResponseEntity<Ressurs<ByteArray>> {
        val mockDokument = MockController::class.java.getResource("/journalføring/mock_dokument_1.pdf").readBytes()
        if (counter++ % 3 == 2) {
            return ResponseEntity.ok(Ressurs.failure("Error", "Artificial error"))
        }

        return ResponseEntity.ok(Ressurs.success(mockDokument, "OK"))
    }

    @PostMapping(path = ["/feature"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentToggles(@RequestBody toggles: List<String>): ResponseEntity<Ressurs<Map<String, Boolean>>> {
        return RessursUtils.ok(toggles.fold(mutableMapOf()) { acc, toggleId ->
            acc[toggleId] = true
            acc
        })
    }

    @GetMapping(path = ["/person"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentPerson(@RequestHeader personIdent: String): ResponseEntity<Ressurs<RestPersonInfo>> {
        if (counter++ % 3 == 2) {
            return ResponseEntity.ok(Ressurs.failure("Error", "Artificial error"))
        }
        return RessursUtils.ok(RestPersonInfo(personIdent = personIdent, fødselsdato = LocalDate.of(1990, 1, 1),
        navn = "Laks Norge", kjønn = Kjønn.MANN, familierelasjoner = emptyList()))
    }

    @GetMapping(path = ["/fagsaker/restfagsak"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentRestFagsak(@RequestHeader personIdent: String)
            : ResponseEntity<Ressurs<RestFagsak?>> {

        return Result.runCatching {
            // TODO: assumes correct personident. Throw an error if person does not exist.
            Ressurs.success<RestFagsak?>(RestFagsak(
                    opprettetTidspunkt = LocalDateTime.now(),
                    id = 23,
                    søkerFødselsnummer = personIdent,
                    status = FagsakStatus.OPPRETTET,
                    underBehandling = false,
                    behandlinger = emptyList(),
            ))
        }.fold(
                onSuccess = { return ResponseEntity.ok().body(it) },
                onFailure = { RessursUtils.illegalState("Ukjent feil ved henting data for manuell journalføring.", it) }
        )
    }
    companion object{
        val LOG = LoggerFactory.getLogger(MockController::class.java)
    }
}