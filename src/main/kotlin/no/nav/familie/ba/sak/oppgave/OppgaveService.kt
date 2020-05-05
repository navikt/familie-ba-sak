package no.nav.familie.ba.sak.oppgave

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.oppgave.domene.DbOppgave
import no.nav.familie.ba.sak.oppgave.domene.OppgaveRepository
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppgave.*

import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class OppgaveService(private val integrasjonClient: IntegrasjonClient,
                     private val behandlingRepository: BehandlingRepository,
                     private val oppgaveRepository: OppgaveRepository,
                     private val arbeidsfordelingService: ArbeidsfordelingService) {

    fun opprettOppgave(behandlingId: Long,
                       oppgavetype: Oppgavetype,
                       fristForFerdigstillelse: LocalDate,
                       enhetId: String? = null,
                       tilordnetNavIdent: String? = null): String {
        val behandling = behandlingRepository.finnBehandling(behandlingId)
        val fagsakId = behandling.fagsak.id

        if (oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(oppgavetype, behandling) !== null
            && oppgavetype !== Oppgavetype.Journalføring) {
            error("Det finnes allerede en oppgave av typen $oppgavetype på behandling ${behandling.id} som ikke er ferdigstilt. Kan ikke opprette ny oppgave")
        }
        val enhetsnummer = arbeidsfordelingService.hentBehandlendeEnhet(behandling.fagsak).firstOrNull()

        val opprettOppgave = OpprettOppgave(
                ident = OppgaveIdent(ident = behandling.fagsak.aktørId.id, type = IdentType.Aktør),
                saksId = fagsakId.toString(),
                tema = Tema.BAR,
                oppgavetype = oppgavetype,
                fristFerdigstillelse = fristForFerdigstillelse,
                beskrivelse = lagOppgaveTekst(fagsakId),
                enhetsnummer = enhetId ?: enhetsnummer?.enhetId,
                behandlingstema = Behandlingstema.ORDINÆR_BARNETRYGD.kode,
                tilordnetRessurs = tilordnetNavIdent
        )

        val opprettetOppgaveId = integrasjonClient.opprettOppgave(opprettOppgave)

        val oppgave = DbOppgave(gsakId = opprettetOppgaveId, behandling = behandling, type = oppgavetype)
        oppgaveRepository.save(oppgave)
        return opprettetOppgaveId
    }

    fun opprettOppgave(request: OpprettOppgave): String {
        return integrasjonClient.opprettOppgave(request)
    }

    fun fordelOppgave(oppgaveId: Long, saksbehandler: String): String {
        return integrasjonClient.fordelOppgave(oppgaveId, saksbehandler)
    }
    fun tilbakestillFordelingPåOppgave(oppgaveId: Long): String {
        return integrasjonClient.fordelOppgave(oppgaveId, null)
    }

    fun hentOppgave(oppgaveId: Long): Ressurs<Oppgave> {
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

    fun lagOppgaveTekst(fagsakId: Long): String {
        return "----- Opprettet av familie-ba-sak ${LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)} --- \n" +
               "https://barnetrygd.nais.adeo.no/fagsak/${fagsakId}"
    }

    fun finnOppgaverKnyttetTilSaksbehandlerOgEnhet(behandlingstema: String?,
                                                   oppgavetype: String?,
                                                   enhet: String?,
                                                   saksbehandler: String?)
            : List<Oppgave> {

        return integrasjonClient.finnOppgaverKnyttetTilSaksbehandlerOgEnhet(behandlingstema, oppgavetype, enhet, saksbehandler)
    }

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): List<Oppgave> {
        val oppgaver = integrasjonClient.hentOppgaver(finnOppgaveRequest)
        return when {
            finnOppgaveRequest.prioritet != null -> oppgaver.filter { finnOppgaveRequest.prioritet == it.prioritet }
            else -> oppgaver
        }
    }

    enum class Behandlingstema(val kode: String) {
        ORDINÆR_BARNETRYGD("ab0180"),
        BARNETRYGD_EØS("ab0058"),
        BARNETRYGD("ab0270"), //Kan brukes hvis man ikke vet om det er EØS, Utvidet eller Ordinært
        UTVIDET_BARNETRYGD("ab0096")
    }
}