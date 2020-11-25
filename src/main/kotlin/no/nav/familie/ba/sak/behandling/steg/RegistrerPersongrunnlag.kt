package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegistrerPersongrunnlag(
        private val behandlingService: BehandlingService,
        private val persongrunnlagService: PersongrunnlagService
) : BehandlingSteg<RegistrerPersongrunnlagDTO> {

    @Transactional
    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: RegistrerPersongrunnlagDTO): StegType {
        val forrigeBehandlingSomErIverksatt =
                behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)
        if (behandling.type == BehandlingType.REVURDERING && forrigeBehandlingSomErIverksatt != null) {
            val forrigePersongrunnlag = persongrunnlagService.hentAktiv(behandlingId = forrigeBehandlingSomErIverksatt.id)
            val forrigePersongrunnlagBarna = forrigePersongrunnlag?.barna?.map { it.personIdent.ident }!!

            persongrunnlagService.lagreSøkerOgBarnIPersonopplysningsgrunnlaget(data.ident,
                                                                               data.barnasIdenter.union(forrigePersongrunnlagBarna)
                                                                                       .toList(),
                                                                               behandling,
                                                                               Målform.NB)
        } else {
            persongrunnlagService.lagreSøkerOgBarnIPersonopplysningsgrunnlaget(data.ident,
                                                                               data.barnasIdenter,
                                                                               behandling,
                                                                               Målform.NB)
        }
        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.REGISTRERE_PERSONGRUNNLAG
    }
}

data class RegistrerPersongrunnlagDTO(
        val ident: String,
        val barnasIdenter: List<String>)