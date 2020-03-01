package no.nav.familie.ba.sak.arbeidsfordeling

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.Fagsak
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonOnBehalfClient
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet
import org.springframework.stereotype.Service

@Service
class ArbeidsfordelingService(private val behandlingRepository: BehandlingRepository,
                              private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                              private val integrasjonOnBehalfClient: IntegrasjonOnBehalfClient) {

    fun hentBehandlendeEnhet(fagsak: Fagsak): List<Arbeidsfordelingsenhet> {
        val søker = integrasjonOnBehalfClient.hentPersoninfoFor(fagsak.personIdent.ident)

        val aktivBehandling = behandlingRepository.findByFagsakAndAktiv(fagsak.id)
                              ?: throw RuntimeException("Kunne ikke finne en aktiv behandling på fagsak med ID: ${fagsak.id}")

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(aktivBehandling.id)
                                       ?: throw RuntimeException("Kunne ikke finne et aktivt personopplysningsgrunnlag på behandling med ID: ${aktivBehandling.id}")

        val personinfoliste = personopplysningGrunnlag.barna.map { barn ->
            integrasjonOnBehalfClient.hentPersoninfoFor(barn.personIdent.ident)
        }.plus(søker)

        val strengesteDiskresjonskode =
                finnStrengesteDiskresjonskode(personinfoliste)

        return integrasjonOnBehalfClient.hentBehandlendeEnhet(søker.geografiskTilknytning, strengesteDiskresjonskode)
    }
}