package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vilkår.VilkårService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegistrerPersongrunnlag(
        private val persongrunnlagService: PersongrunnlagService,
        private val vilkårService: VilkårService
) : BehandlingSteg<RegistrerPersongrunnlagDTO> {

    @Transactional
    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: RegistrerPersongrunnlagDTO): StegType {
        persongrunnlagService.lagreSøkerOgBarnIPersonopplysningsgrunnlaget(data.ident, data.barnasIdenter, behandling)
        vilkårService.initierVilkårvurderingForBehandling(behandling.id, data.bekreftEndringerViaFrontend)

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.REGISTRERE_PERSONGRUNNLAG
    }
}

data class RegistrerPersongrunnlagDTO(
        val ident: String,
        val bekreftEndringerViaFrontend: Boolean = false,
        val barnasIdenter: List<String>)