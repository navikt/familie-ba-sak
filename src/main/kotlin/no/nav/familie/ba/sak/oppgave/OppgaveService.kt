package no.nav.familie.ba.sak.oppgave

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.kontrakter.felles.oppgave.IdentType
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdent
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgave
import no.nav.familie.kontrakter.felles.oppgave.Tema
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class OppgaveService(private val integrasjonTjeneste: IntegrasjonTjeneste,
                     private val behandlingRepository: BehandlingRepository) {

    fun opprettOppgaveForNyBehandling(behandlingsId: Long): String {
        val behandling = behandlingRepository.finnBehandling(behandlingsId) ?: error("Kan ikke finne behandling med id $behandlingsId")
        val fagsakId = behandling.fagsak.id ?: error("Kan ikke finne fagsakId for behandling $behandlingsId")

        val aktørId = Result.run { integrasjonTjeneste.hentAktørId(behandling.fagsak.personIdent.ident) }.id
        val enhetsnummer = integrasjonTjeneste.hentBehandlendeEnhetForPersonident(behandling.fagsak.personIdent.ident).firstOrNull()

        val opprettOppgave = OpprettOppgave(ident = OppgaveIdent(ident = aktørId, type = IdentType.Aktør),
                                            saksId = fagsakId.toString(),
                                            tema = Tema.BAR,
                                            fristFerdigstillelse = LocalDate.now().plusDays(1), //TODO få denne til å funke på helg og eventuellle andre helligdager
                                            beskrivelse = lagOppgaveTekst(fagsakId),
                                            enhetsnummer = enhetsnummer?.enhetId,
                                            behandlingstema = Behandlingstema.ORDINÆR_BARNETRYGD.kode)

        val opprettetOppgaveId = integrasjonTjeneste.opprettOppgave(opprettOppgave)
        behandlingRepository.save(behandling.copy(oppgaveId = opprettetOppgaveId))
        return opprettetOppgaveId
    }


    private fun lagOppgaveTekst(fagsakId: Long): String {
        //TODO Tekst skal oppdateres når man får et forslag
        var oppgaveTekst =
                "----- Opprettet av familie-ba-sak ${LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)} --- \n"
        oppgaveTekst += "Ny sak om behandling av ordinær barnetrygd \n"
        oppgaveTekst += "https://barnetrygd.nais.adeo.no/fagsak/${fagsakId}/behandle"
        return oppgaveTekst
    }


    enum class Behandlingstema(val kode: String) {
        ORDINÆR_BARNETRYGD("ab0180"),
        BARNETRYGD_EØS("ab0058"),
        BARNETRYGD("ab0270"), //Kan brukes hvis man ikke vet om det er EØS, Utvidet eller Ordinært
        UTVIDET_BARNETRYGD("ab0096")
    }
}