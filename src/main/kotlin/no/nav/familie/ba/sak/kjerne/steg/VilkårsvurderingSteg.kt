package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
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

        if (behandling.opprettetÅrsak == BehandlingÅrsak.DØDSFALL_BRUKER) {
            val vilkårsvurdering = vilkårService.hentVilkårsvurderingThrows(behandling.id)
            validerIngenVilkårSattEtterSøkersDød(
                personopplysningGrunnlag = personopplysningGrunnlag,
                vilkårsvurdering = vilkårsvurdering
            )
        }

        if (featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS)) {
            vilkårService.hentVilkårsvurdering(behandling.id)?.apply {
                validerIkkeBlandetRegelverk(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    vilkårsvurdering = this
                )
            }
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

        tilbakestillBehandlingService.tilbakestillDataTilVilkårsvurderingssteg(behandling)
        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        if (featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS)) {
            kompetanseService.tilpassKompetanserTilRegelverk(BehandlingId(behandling.id))
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
