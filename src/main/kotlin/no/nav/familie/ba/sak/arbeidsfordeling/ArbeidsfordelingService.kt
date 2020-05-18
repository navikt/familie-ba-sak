package no.nav.familie.ba.sak.arbeidsfordeling

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet
import org.springframework.stereotype.Service

@Service
class ArbeidsfordelingService(private val behandlingRepository: BehandlingRepository,
                              private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                              private val integrasjonClient: IntegrasjonClient) {

    fun hentBehandlendeEnhet(fagsak: Fagsak): List<Arbeidsfordelingsenhet> {
        val søker = integrasjonClient.hentPersoninfoFor(fagsak.hentAktivIdent().ident)

        val aktivBehandling = behandlingRepository.findByFagsakAndAktiv(fagsak.id)
                              ?: error("Kunne ikke finne en aktiv behandling på fagsak med ID: ${fagsak.id}")

        val personinfoliste = when (val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(aktivBehandling.id)) {
              null -> listOf(søker)
              else -> personopplysningGrunnlag.barna.map { barn ->
                integrasjonClient.hentPersoninfoFor(barn.personIdent.ident) }.plus(søker)
        }
                val strengesteDiskresjonskode =
                finnStrengesteDiskresjonskode(personinfoliste)

        return integrasjonClient.hentBehandlendeEnhet(søker.geografiskTilknytning, strengesteDiskresjonskode)
    }
}