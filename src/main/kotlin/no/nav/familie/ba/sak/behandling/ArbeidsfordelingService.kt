package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.Fagsak
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo

import java.lang.RuntimeException

class ArbeidsfordelingService(val behandlingRepository: BehandlingRepository,
                              val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                              val integrasjonTjeneste: IntegrasjonTjeneste) {
    fun hentBehandlendeEnhet(fagsak: Fagsak): List<Arbeidsfordelingsenhet> {
        val søker = integrasjonTjeneste.hentPersoninfoFor(fagsak.personIdent.ident)
        val aktivBehandling = behandlingRepository.findByFagsakAndAktiv(fagsak.id) ?: throw RuntimeException()
        val strengesteDiskresjonskode = finnStrengesteDiskresjonskode(søker, aktivBehandling)
        return integrasjonTjeneste.hentBehandlendeEnhet(søker.geografiskTilknytning, strengesteDiskresjonskode)
    }

    private fun finnStrengesteDiskresjonskode(søker: Personinfo, aktivBehandling: Behandling): String? {
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(aktivBehandling.id) ?: throw RuntimeException()

        return personopplysningGrunnlag.barna.fold(søker.diskresjonskode, { diskresjonskode, barn ->
            if (diskresjonskode != Diskresjonskode.KODE6.kode) {
                integrasjonTjeneste.hentPersoninfoFor(barn.personIdent.ident).diskresjonskode.run {
                    when (this) {
                        Diskresjonskode.KODE6.kode, Diskresjonskode.KODE7.kode -> this
                        else -> diskresjonskode
                    }
                }
            } else {
                diskresjonskode
            }
        })
    }
}

enum class Diskresjonskode(val kode: String) {
    KODE6("SPSF"),
    KODE7("SPFO")
}