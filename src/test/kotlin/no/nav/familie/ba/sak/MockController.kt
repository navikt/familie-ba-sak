package no.nav.familie.ba.sak

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.familie.ba.sak.oppgave.domene.DataForManuellJournalføring
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/mock")
@Unprotected
class MockController {
    val objectMapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())

    init{
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    @GetMapping(path = ["/oppgave/{oppgaveId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentDataForManuellJournalføring(@PathVariable(name = "oppgaveId") oppgaveId: Long)
            : ResponseEntity<Ressurs<DataForManuellJournalføring>> {
        val mockJournalpostJson = MockController::class.java.getResource("/journalføring/journalpost_453636379.json").readText()
        val mockJournalpost = objectMapper.readValue(mockJournalpostJson, Journalpost::class.java)
        return ResponseEntity.ok(Ressurs.success(DataForManuellJournalføring(
                oppgave = Oppgave(id = 0,),
                person = null,
                fagsak = null,
                journalpost = mockJournalpost
        )))
    }

    @GetMapping("/journalpost/{journalpostId}/hent/{dokumentInfoId}")
    fun hentDokument(@PathVariable journalpostId: String,
                     @PathVariable dokumentInfoId: String)
            : ResponseEntity<Ressurs<ByteArray>> {
        val mockDokument = MockController::class.java.getResource("/journalføring/mock_dokument_1.pdf").readBytes()
        return ResponseEntity.ok(Ressurs.success(mockDokument, "OK"))
    }
}