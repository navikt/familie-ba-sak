package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.RestVilkårsvurdering
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.VilkårService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class Vilkårsvurdering(
        private val behandlingService: BehandlingService,
        private val vilkårService: VilkårService,
        private val vedtakService: VedtakService,
        private val beregningService: BeregningService,
        private val persongrunnlagService: PersongrunnlagService
) : BehandlingSteg<RestVilkårsvurdering> {

    @Transactional
    override fun utførStegOgAngiNeste(behandling: Behandling, data: RestVilkårsvurdering): StegType {
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandling.id)
                                       ?: error("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        val vilkårsvurdertBehandling = behandlingService.hent(behandlingId = behandling.id)

        if (data.personResultater.isNotEmpty()) {
            vilkårService.kontrollerVurderteVilkårOgLagResultat(data.personResultater,
                                                                vilkårsvurdertBehandling.id)
        } else {
            vilkårService.vurderVilkårForFødselshendelse(vilkårsvurdertBehandling.id)
        }
        val vedtak = vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                vilkårsvurdertBehandling,
                personopplysningGrunnlag,
                ansvarligSaksbehandler = SikkerhetContext.hentSaksbehandlerNavn())

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
        vedtakService.oppdaterVedtakMedStønadsbrev(vedtak)

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.VILKÅRSVURDERING
    }
}