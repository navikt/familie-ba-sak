package no.nav.familie.ba.sak.arbeidsfordeling

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.Fagsak
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet
import org.springframework.stereotype.Service

@Service
class ArbeidsfordelingService(private val behandlingRepository: BehandlingRepository,
                              private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                              private val integrasjonTjeneste: IntegrasjonTjeneste) {

    fun hentBehandlendeEnhet(fagsak: Fagsak): List<Arbeidsfordelingsenhet> {
        val søker = integrasjonTjeneste.hentPersoninfoFor(fagsak.personIdent.ident)

        val aktivBehandling = behandlingRepository.findByFagsakAndAktiv(fagsak.id)
                              ?: error("Kunne ikke finne en aktiv behandling på fagsak med ID: ${fagsak.id}")

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(aktivBehandling.id)
                                       ?: error("Kunne ikke finne et aktivt personopplysningsgrunnlag på behandling med ID: ${aktivBehandling.id}")

        val personinfoliste = personopplysningGrunnlag.barna.map { barn ->
            integrasjonTjeneste.hentPersoninfoFor(barn.personIdent.ident)
        }.plus(søker)

        val strengesteDiskresjonskode =
                finnStrengesteDiskresjonskode(personinfoliste)

        return integrasjonTjeneste.hentBehandlendeEnhet(søker.geografiskTilknytning, strengesteDiskresjonskode)
    }
}