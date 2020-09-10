package no.nav.familie.ba.sak.oppgave

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.oppgave.domene.DbOppgave
import no.nav.familie.ba.sak.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.kontrakter.felles.oppgave.*
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class OppgaveService(private val integrasjonClient: IntegrasjonClient,
                     private val personopplysningerService: PersonopplysningerService,
                     private val behandlingRepository: BehandlingRepository,
                     private val oppgaveRepository: OppgaveRepository,
                     private val arbeidsfordelingService: ArbeidsfordelingService) {

    fun opprettOppgave(behandlingId: Long,
                       oppgavetype: Oppgavetype,
                       fristForFerdigstillelse: LocalDate,
                       enhetId: String? = null,
                       tilordnetNavIdent: String? = null,
                       beskrivelse: String? = null): String {
        val behandling = behandlingRepository.finnBehandling(behandlingId)
        val fagsakId = behandling.fagsak.id

        val eksisterendeOppgave = oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(oppgavetype, behandling)

        return if (eksisterendeOppgave != null
                   && oppgavetype != Oppgavetype.Journalføring) {
            LOG.error("Fant eksisterende oppgave med samme oppgavetype som ikke er ferdigstilt ved opprettelse av ny oppgave ${eksisterendeOppgave}. " +
                      "Vi går videre, men kaster feil for å følge med på utviklingen.")

            eksisterendeOppgave.gsakId
        } else {
            val enhetsnummer = arbeidsfordelingService.hentBehandlendeEnhet(behandling.fagsak).firstOrNull()
            val aktorId = personopplysningerService.hentAktivAktørId(Ident(behandling.fagsak.hentAktivIdent().ident)).id
            val opprettOppgave = OpprettOppgaveRequest(
                    ident = OppgaveIdentV2(ident = aktorId, gruppe = IdentGruppe.AKTOERID),
                    saksId = fagsakId.toString(),
                    tema = Tema.BAR,
                    oppgavetype = oppgavetype,
                    fristFerdigstillelse = fristForFerdigstillelse,
                    beskrivelse = lagOppgaveTekst(fagsakId, beskrivelse),
                    enhetsnummer = enhetId ?: enhetsnummer?.enhetId,
                    behandlingstema = Behandlingstema.ORDINÆR_BARNETRYGD.kode,
                    tilordnetRessurs = tilordnetNavIdent
            )

            val opprettetOppgaveId = integrasjonClient.opprettOppgave(opprettOppgave)

            val oppgave = DbOppgave(gsakId = opprettetOppgaveId, behandling = behandling, type = oppgavetype)
            oppgaveRepository.save(oppgave)
            opprettetOppgaveId
        }
    }

    fun fordelOppgave(oppgaveId: Long, saksbehandler: String): String {
        return integrasjonClient.fordelOppgave(oppgaveId, saksbehandler)
    }

    fun tilbakestillFordelingPåOppgave(oppgaveId: Long): String {
        return integrasjonClient.fordelOppgave(oppgaveId, null)
    }

    fun hentOppgaveSomIkkeErFerdigstilt(oppgavetype: Oppgavetype, behandling: Behandling): DbOppgave? {
        return oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(oppgavetype, behandling)
    }

    fun hentOppgave(oppgaveId: Long): Oppgave {
        return integrasjonClient.finnOppgaveMedId(oppgaveId)
    }

    fun ferdigstillOppgave(behandlingId: Long, oppgavetype: Oppgavetype) {
        val oppgave = oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(oppgavetype,
                                                                                         behandlingRepository.finnBehandling(
                                                                                                 behandlingId))
                      ?: error("Finner ikke oppgave for behandling $behandlingId")
        integrasjonClient.ferdigstillOppgave(oppgave.gsakId.toLong())

        oppgave.erFerdigstilt = true
        oppgaveRepository.save(oppgave)
    }

    fun lagOppgaveTekst(fagsakId: Long, beskrivelse: String? = null): String {
        return if (beskrivelse != null) { beskrivelse + "\n" } else { "" } +
               "----- Opprettet av familie-ba-sak ${LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)} --- \n" +
               "https://barnetrygd.nais.adeo.no/fagsak/${fagsakId}"
    }

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto {
        return integrasjonClient.hentOppgaver(finnOppgaveRequest)
    }

    enum class Behandlingstema(val kode: String) {
        ORDINÆR_BARNETRYGD("ab0180"),
        BARNETRYGD_EØS("ab0058"),
        BARNETRYGD("ab0270"), //Kan brukes hvis man ikke vet om det er EØS, Utvidet eller Ordinært
        UTVIDET_BARNETRYGD("ab0096")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(this::class.java)
    }
}