package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.Fagsak
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet

import java.lang.RuntimeException

class ArbeidsfordelingService(val behandlingRepository: BehandlingRepository,
                              val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                              val integrasjonTjeneste: IntegrasjonTjeneste) {
    fun hentBehandlendeEnhet(fagsak: Fagsak): List<Arbeidsfordelingsenhet> {
        val søker = integrasjonTjeneste.hentPersoninfoFor(fagsak.personIdent.ident)

        val aktivBehandling = behandlingRepository.findByFagsakAndAktiv(fagsak.id)
                ?: throw RuntimeException("Kunne ikke finne en aktiv behandling på fagsak med ID: ${fagsak.id}")

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(aktivBehandling.id)
                ?: throw RuntimeException("Kunne ikke finne et aktivt personopplysningsgrunnlag på behandling med ID: ${aktivBehandling.id}")

        val personinfoliste = personopplysningGrunnlag.barna.map {
            barn -> integrasjonTjeneste.hentPersoninfoFor(barn.personIdent.ident)
        }.plus(søker)

        val strengesteDiskresjonskode = SikkerhetService.finnStrengesteDiskresjonskode(personinfoliste)

        return integrasjonTjeneste.hentBehandlendeEnhet(søker.geografiskTilknytning, strengesteDiskresjonskode)
    }
}