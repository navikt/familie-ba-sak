package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
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
            val forrigeMålform = persongrunnlagService.hentSøkersMålform(behandlingId = forrigeBehandlingSomErIverksatt.id)

            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(data.ident,
                                                                      data.barnasIdenter.union(
                                                                                               forrigePersongrunnlagBarna)
                                                                                               .toList(),
                                                                      behandling,
                                                                      forrigeMålform)
        } else {
            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(data.ident,
                                                                      data.barnasIdenter,
                                                                      behandling,
                                                                      Målform.NB)
        }
        if (!(behandling.opprettetÅrsak == BehandlingÅrsak.SØKNAD ||
              behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE)) {
            vilkårService.initierVilkårsvurderingForBehandling(behandling = behandling,
                                                               bekreftEndringerViaFrontend = true,
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
        val barnasIdenter: List<String>)