package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vilkår.VilkårService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegistrerPersongrunnlag(
        private val behandlingService: BehandlingService,
        private val persongrunnlagService: PersongrunnlagService,
        private val vilkårService: VilkårService
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
                                                                               data.målform)
        } else {
            persongrunnlagService.lagreSøkerOgBarnIPersonopplysningsgrunnlaget(data.ident,
                                                                               data.barnasIdenter,
                                                                               behandling,
                                                                               data.målform)
        }

        if (!behandling.skalBehandlesAutomatisk) {
            vilkårService.initierVilkårvurderingForBehandling(behandling = behandling,
                                                              bekreftEndringerViaFrontend = data.bekreftEndringerViaFrontend,
                                                              forrigeBehandling = forrigeBehandlingSomErIverksatt)
        }
        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.REGISTRERE_PERSONGRUNNLAG
    }
}

data class RegistrerPersongrunnlagDTO(
        val ident: String,
        val bekreftEndringerViaFrontend: Boolean = false,
        val barnasIdenter: List<String>,
        val målform: Målform = Målform.NB)