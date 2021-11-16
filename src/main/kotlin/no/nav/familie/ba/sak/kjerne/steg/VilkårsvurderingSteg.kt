package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VilkårsvurderingSteg(
    private val vilkårService: VilkårService,
    private val beregningService: BeregningService,
    private val persongrunnlagService: PersongrunnlagService,
    private val behandlingService: BehandlingService,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
) : BehandlingSteg<String> {

    @Transactional
    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: String
    ): StegType {
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandling.id)
            ?: throw Feil("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        if (behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
            vilkårService.initierVilkårsvurderingForBehandling(
                behandling = behandling,
                bekreftEndringerViaFrontend = true,
                forrigeBehandlingSomErVedtatt = behandlingService.hentForrigeBehandlingSomErVedtatt(
                    behandling
                )
            )
        }
        endretUtbetalingAndelService.fjernKnytningTilAndelTilkjentYtelse(behandling.id)
        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.VILKÅRSVURDERING
    }
}
