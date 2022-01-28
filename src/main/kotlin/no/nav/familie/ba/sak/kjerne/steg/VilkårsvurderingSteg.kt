package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.validerSøkerBosattIRiketTomdato
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VilkårsvurderingSteg(
    private val vilkårService: VilkårService,
    private val beregningService: BeregningService,
    private val persongrunnlagService: PersongrunnlagService,
    private val behandlingService: BehandlingService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService
) : BehandlingSteg<String> {

    @Transactional
    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: String
    ): StegType {
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandling.id)

        if (behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
            vilkårService.initierVilkårsvurderingForBehandling(
                behandling = behandling,
                bekreftEndringerViaFrontend = true,
                forrigeBehandlingSomErVedtatt = behandlingService.hentForrigeBehandlingSomErVedtatt(
                    behandling
                )
            )
        }

        if (behandling.opprettetÅrsak == BehandlingÅrsak.DØDSFALL_BRUKER) {
            val vilkårsvurdering = vilkårService.hentVilkårsvurderingThrows(behandling.id)
            validerSøkerBosattIRiketTomdato(
                personopplysningGrunnlag = personopplysningGrunnlag,
                vilkårsvurdering = vilkårsvurdering
            )
        }

        // midlertidig validering for helmanuell migrering
        if (behandling.erHelmanuellMigrering()) {
            val vilkårsvurdering = vilkårService.hentVilkårsvurderingThrows(behandling.id)
            val finnesDeltBosted = vilkårsvurdering.personResultater.any {
                it.vilkårResultater.filter { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.BOR_MED_SØKER }
                    .any { borMedSøker ->
                        borMedSøker.utdypendeVilkårsvurderinger
                            .contains(UtdypendeVilkårsvurdering.DELT_BOSTED)
                    }
            }
            if (!finnesDeltBosted) {
                throw FunksjonellFeil(
                    melding = "Behandling ${behandling.id} kan ikke fortsettes uten delt bosted i vilkårsvurdering " +
                        "for minst ett av barna",
                    frontendFeilmelding = "Det må legges inn delt bosted i vilkårsvurderingen for minst ett av barna " +
                        "før du kan fortsette behandlingen"
                )
            }
        }

        tilbakestillBehandlingService.tilbakestillDataTilVilkårsvurderingssteg(behandling)
        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.VILKÅRSVURDERING
    }
}
