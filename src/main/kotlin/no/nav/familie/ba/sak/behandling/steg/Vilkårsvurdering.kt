package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vilkår.VilkårService
import no.nav.familie.ba.sak.behandling.vedtak.RestVilkårsvurdering
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class Vilkårsvurdering(
        private val behandlingService: BehandlingService,
        private val vilkårService: VilkårService,
        private val vedtakService: VedtakService,
        private val persongrunnlagService: PersongrunnlagService
) : BehandlingSteg<RestVilkårsvurdering> {

    @Transactional
    override fun utførSteg(behandling: Behandling, data: RestVilkårsvurdering): Behandling {
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandling.id)
                                       ?: error("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        val testBehandling = behandlingService.hent(behandlingId = behandling.id)
        val vilkårsvurdertBehandling = behandlingService.settVilkårsvurdering(testBehandling,
                                                                              data.brevType,
                                                                              data.begrunnelse)

        if (data.behandlingResultat.isNotEmpty()) {
            vilkårService.kontrollerVurderteVilkårOgLagResultat(personopplysningGrunnlag,
                                                                data.behandlingResultat,
                                                                vilkårsvurdertBehandling.id)
        } else {
            vilkårService.vurderVilkårForFødselshendelse(personopplysningGrunnlag, vilkårsvurdertBehandling.id)
        }
        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(vilkårsvurdertBehandling,
                                                                 personopplysningGrunnlag,
                                                                 ansvarligSaksbehandler = SikkerhetContext.hentSaksbehandler())

        return vilkårsvurdertBehandling
    }

    override fun stegType(): StegType {
        return StegType.VILKÅRSVURDERING
    }
}