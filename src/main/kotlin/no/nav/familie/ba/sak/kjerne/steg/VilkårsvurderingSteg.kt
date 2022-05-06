package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.KAN_MANUELT_MIGRERE_ANNET_ENN_DELT_BOSTED
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.validerIkkeBlandetRegelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.validerIngenVilkårSattEtterSøkersDød
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VilkårsvurderingSteg(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingstemaService: BehandlingstemaService,
    private val vilkårService: VilkårService,
    private val beregningService: BeregningService,
    private val persongrunnlagService: PersongrunnlagService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
    private val kompetanseService: KompetanseService,
    private val featureToggleService: FeatureToggleService
) : BehandlingSteg<String> {

    override fun preValiderSteg(behandling: Behandling, stegService: StegService?) {
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandling.id)
        val vilkårsvurdering = vilkårService.hentVilkårsvurderingThrows(behandling.id)

        if (behandling.opprettetÅrsak == BehandlingÅrsak.DØDSFALL_BRUKER) {
            validerIngenVilkårSattEtterSøkersDød(
                personopplysningGrunnlag = personopplysningGrunnlag,
                vilkårsvurdering = vilkårsvurdering
            )
        }

        if (featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS)) {
            validerIkkeBlandetRegelverk(
                personopplysningGrunnlag = personopplysningGrunnlag,
                vilkårsvurdering = vilkårsvurdering
            )
        }
    }

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
                forrigeBehandlingSomErVedtatt = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(
                    behandling
                )
            )
        }

        // midlertidig validering for helmanuell migrering
        if (behandling.erHelmanuellMigrering() && !featureToggleService.isEnabled(
                KAN_MANUELT_MIGRERE_ANNET_ENN_DELT_BOSTED
            )
        ) {
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

        if (featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS)) {
            kompetanseService.tilpassKompetanserTilRegelverk(behandling.id)
        }

        behandlingstemaService.oppdaterBehandlingstema(
            behandling = behandlingHentOgPersisterService.hent(behandlingId = behandling.id),
        )

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.VILKÅRSVURDERING
    }
}
