package no.nav.familie.ba.sak.arbeidsfordeling

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.domene.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet
import org.springframework.stereotype.Service

@Service
class ArbeidsfordelingService(private val behandlingRepository: BehandlingRepository,
                              private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                              private val integrasjonClient: IntegrasjonClient) {

    fun hentBehandlendeEnhet(fagsak: Fagsak): List<Arbeidsfordelingsenhet> {
        val søker = identMedAdressebeskyttelse(fagsak.hentAktivIdent().ident)

        val aktivBehandling = behandlingRepository.findByFagsakAndAktiv(fagsak.id)
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