package no.nav.familie.ba.sak.arbeidsfordeling

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.domene.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.oppgave.domene.OppgaveRepository
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.stereotype.Service

@Service
class ArbeidsfordelingService(private val behandlingService: BehandlingService,
                              private val oppgaveRepository: OppgaveRepository,
                              private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                              private val integrasjonClient: IntegrasjonClient) {

    /**
     * Bruker oppgaveRepository og integrasjonClient for å unngå dependency cycle.
     */
    fun bestemBehandlendeEnhet(behandling: Behandling): String {
        val behandleSakDbOppgave =
                oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(Oppgavetype.BehandleSak, behandling)

        val enhetFraBehandleSakOppgave = when (behandleSakDbOppgave) {
            null -> null
            else -> integrasjonClient.finnOppgaveMedId(oppgaveId = behandleSakDbOppgave.gsakId.toLong()).tildeltEnhetsnr
        }

        val enhetFraArbeidsfordeling =
                hentBehandlendeEnhet(fagsak = behandling.fagsak).firstOrNull()

        return enhetFraBehandleSakOppgave ?: enhetFraArbeidsfordeling?.enhetId
               ?: throw Feil(message = "Finner ikke behandlende enhet på behandling. Både enhet fra oppgave og arbeidsfordeling er null")
    }

    fun hentBehandlendeEnhet(fagsak: Fagsak): List<Arbeidsfordelingsenhet> {
        val søker = identMedAdressebeskyttelse(fagsak.hentAktivIdent().ident)

        val aktivBehandling = behandlingService.hentAktivForFagsak(fagsakId = fagsak.id)
                              ?: error("Kunne ikke finne en aktiv behandling på fagsak med ID: ${fagsak.id}")

        val personinfoliste = when (val personopplysningGrunnlag =
                personopplysningGrunnlagRepository.findByBehandlingAndAktiv(aktivBehandling.id)) {
            null -> listOf(søker)
            else -> personopplysningGrunnlag.barna.map { barn ->
                identMedAdressebeskyttelse(barn.personIdent.ident)
            }.plus(søker)
        }

        val identMedStrengeste = finnPersonMedStrengesteAdressebeskyttelse(personinfoliste)

        return integrasjonClient.hentBehandlendeEnhet(identMedStrengeste ?: søker.ident)
    }

    private fun identMedAdressebeskyttelse(ident: String) = IdentMedAdressebeskyttelse(
            ident = ident,
            adressebeskyttelsegradering = integrasjonClient.hentPersoninfoFor(ident).adressebeskyttelseGradering)

    data class IdentMedAdressebeskyttelse(
            val ident: String,
            val adressebeskyttelsegradering: ADRESSEBESKYTTELSEGRADERING?
    )
}