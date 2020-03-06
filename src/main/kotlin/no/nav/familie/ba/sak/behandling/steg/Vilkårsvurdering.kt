package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.domene.vilkår.VilkårService
import no.nav.familie.ba.sak.behandling.vedtak.RestVilkårsvurdering
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service

@Service
class Vilkårsvurdering(
        private val behandlingService: BehandlingService,
        private val vilkårService: VilkårService,
        private val vedtakService: VedtakService,
        private val persongrunnlagService: PersongrunnlagService
) : BehandlingSteg<RestVilkårsvurdering> {

    override fun utførSteg(behandling: Behandling, data: RestVilkårsvurdering): Behandling {
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandling.id)
                                       ?: error("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        val testBehandling = behandlingService.hent(behandlingId = behandling.id)
        val vilkårsvurdertBehandling = behandlingService.settVilkårsvurdering(testBehandling,
                                                                              data.resultat,
                                                                              data.begrunnelse)
        vilkårService.vurderVilkårOgLagResultat(personopplysningGrunnlag,
                                                data.samletVilkårResultat,
                                                vilkårsvurdertBehandling.id)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(vilkårsvurdertBehandling,
                                                                 personopplysningGrunnlag,
                                                                 data.samletVilkårResultat,
                                                                 ansvarligSaksbehandler = SikkerhetContext.hentSaksbehandler())

        return vilkårsvurdertBehandling
    }

    override fun stegType(): StegType {
        return StegType.VILKÅRSVURDERING
    }

    override fun nesteSteg(behandling: Behandling): StegType {
        return StegType.FORESLÅ_VEDTAK
    }
}