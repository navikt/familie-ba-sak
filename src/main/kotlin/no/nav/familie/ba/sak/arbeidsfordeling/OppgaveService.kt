package no.nav.familie.ba.sak.arbeidsfordeling

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.kontrakter.felles.oppgave.*
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.BehandleSak
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class OppgaveService(private val integrasjonClient: IntegrasjonClient,
                     private val behandlingRepository: BehandlingRepository,
                     private val arbeidsfordelingService: ArbeidsfordelingService) {

    fun opprettOppgaveForNyBehandling(behandlingsId: Long): String {
        val behandling = behandlingRepository.finnBehandling(behandlingsId)
        val fagsakId = behandling.fagsak.id

        val aktørId = integrasjonClient.hentAktørId(behandling.fagsak.personIdent.ident).id
        val enhetsnummer = arbeidsfordelingService.hentBehandlendeEnhet(behandling.fagsak).firstOrNull()

        val opprettOppgave = OpprettOppgave(ident = OppgaveIdent(ident = aktørId, type = IdentType.Aktør),
                                            saksId = fagsakId.toString(),
                                            tema = Tema.BAR,
                                            oppgavetype = BehandleSak,
                                            fristFerdigstillelse = LocalDate.now().plusDays(1), //TODO få denne til å funke på helg og eventuellle andre helligdager
                                            beskrivelse = lagOppgaveTekst(fagsakId),
                                            enhetsnummer = enhetsnummer?.enhetId,
                                            behandlingstema = Behandlingstema.ORDINÆR_BARNETRYGD.kode)

        val opprettetOppgaveId = integrasjonClient.opprettOppgave(opprettOppgave)
        behandlingRepository.save(behandling.copy(oppgaveId = opprettetOppgaveId))
        return opprettetOppgaveId
    }

    fun ferdigstillOppgave(behandlingsId: Long) {
        val oppgaveId = behandlingRepository.finnBehandling(behandlingsId).oppgaveId?.toLong()
                        ?: error("Kan ikke finne oppgave for behandlingId $behandlingsId")
        integrasjonClient.ferdigstillOppgave(oppgaveId)
    }


    private fun lagOppgaveTekst(fagsakId: Long): String {
        //TODO Tekst skal oppdateres når man får et forslag
        var oppgaveTekst =
                "----- Opprettet av familie-ba-sak ${LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)} --- \n"
        oppgaveTekst += "Ny sak om behandling av ordinær barnetrygd \n"
        oppgaveTekst += "https://barnetrygd.nais.adeo.no/fagsak/${fagsakId}"
        return oppgaveTekst
    }


    enum class Behandlingstema(val kode: String) {
        ORDINÆR_BARNETRYGD("ab0180"),
        BARNETRYGD_EØS("ab0058"),
        BARNETRYGD("ab0270"), //Kan brukes hvis man ikke vet om det er EØS, Utvidet eller Ordinært
        UTVIDET_BARNETRYGD("ab0096")
    }
}