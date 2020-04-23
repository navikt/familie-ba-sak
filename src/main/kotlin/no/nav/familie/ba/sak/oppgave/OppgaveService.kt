package no.nav.familie.ba.sak.oppgave

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.oppgave.domene.Oppgave
import no.nav.familie.ba.sak.oppgave.domene.OppgaveDto
import no.nav.familie.ba.sak.oppgave.domene.OppgaveRepository
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppgave.*
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.BehandleSak
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.GodkjenneVedtak

import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class OppgaveService(private val integrasjonClient: IntegrasjonClient,
                     private val behandlingRepository: BehandlingRepository,
                     private val oppgaveRepository: OppgaveRepository,
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
                                            fristFerdigstillelse = LocalDate.now()
                                                    .plusDays(1), //TODO få denne til å funke på helg og eventuellle andre helligdager
                                            beskrivelse = lagOppgaveTekst(fagsakId, "sak om behandling"),
                                            enhetsnummer = enhetsnummer?.enhetId,
                                            behandlingstema = Behandlingstema.ORDINÆR_BARNETRYGD.kode)

        val opprettetOppgaveId = integrasjonClient.opprettOppgave(opprettOppgave)

        val oppgave = Oppgave(gsakId = opprettetOppgaveId, behandling = behandling, type = BehandleSak)
        oppgaveRepository.save(oppgave)
        behandlingRepository.save(behandling.copy(oppgaveId = opprettetOppgaveId))
        return opprettetOppgaveId
    }

    fun opprettOppgaveForGodkjenneVedtak(behandlingId: Long): String {
        val behandling = behandlingRepository.finnBehandling(behandlingId)
        val fagsakId = behandling.fagsak.id

        val aktørId = integrasjonClient.hentAktørId(behandling.fagsak.personIdent.ident).id
        val enhetsnummer = arbeidsfordelingService.hentBehandlendeEnhet(behandling.fagsak).firstOrNull()

        val opprettOppgave = OpprettOppgave(
                ident = OppgaveIdent(ident = aktørId, type = IdentType.Aktør),
                saksId = fagsakId.toString(),
                tema = Tema.BAR,
                oppgavetype = GodkjenneVedtak,
                fristFerdigstillelse = LocalDate.now(),
                beskrivelse = lagOppgaveTekst(fagsakId, "godkjenning av vedtak"),
                enhetsnummer = enhetsnummer?.enhetId,
                behandlingstema = Behandlingstema.ORDINÆR_BARNETRYGD.kode
        )

        val opprettetOppgaveId = integrasjonClient.opprettOppgave(opprettOppgave)
        val oppgave = Oppgave(gsakId = opprettetOppgaveId, behandling = behandling, type = GodkjenneVedtak)
        oppgaveRepository.save(oppgave)
        return opprettetOppgaveId
    }

    fun hentOppgave(oppgaveId: Long): Ressurs<OppgaveDto> {
        return integrasjonClient.finnOppgaveMedId(oppgaveId)
    }

    fun ferdigstillOppgave(behandlingsId: Long, oppgavetype: Oppgavetype) {
        val oppgave = oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(oppgavetype, behandlingRepository.finnBehandling(behandlingsId))
                ?: error("Finner ikke oppgave for behandling $behandlingsId")
        integrasjonClient.ferdigstillOppgave(oppgave.gsakId.toLong())

        oppgave.erFerdigstilt = true
        oppgaveRepository.save(oppgave)
    }

    private fun lagOppgaveTekst(fagsakId: Long, oppgavetype: String): String {
        //TODO Tekst skal oppdateres når man får et forslag
        var oppgaveTekst =
                "----- Opprettet av familie-ba-sak ${LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)} --- \n"
        oppgaveTekst += "Ny $oppgavetype for ordinær barnetrygd \n"
        oppgaveTekst += "https://barnetrygd.nais.adeo.no/fagsak/${fagsakId}"
        return oppgaveTekst
    }

    fun finnOppgaverKnyttetTilSaksbehandlerOgEnhet(behandlingstema: String?,
                                                   oppgavetype: String?,
                                                   enhet: String?,
                                                   saksbehandler: String?)
            : List<OppgaveDto> {

        return integrasjonClient.finnOppgaverKnyttetTilSaksbehandlerOgEnhet(behandlingstema, oppgavetype, enhet, saksbehandler)
    }

    enum class Behandlingstema(val kode: String) {
        ORDINÆR_BARNETRYGD("ab0180"),
        BARNETRYGD_EØS("ab0058"),
        BARNETRYGD("ab0270"), //Kan brukes hvis man ikke vet om det er EØS, Utvidet eller Ordinært
        UTVIDET_BARNETRYGD("ab0096")
    }
}