package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.springframework.stereotype.Service

@Service
class PreutfyllVilkårService(
    private val preutfyllLovligOppholdMedPersongrunnlagService: PreutfyllLovligOppholdMedLagringIPersongrunnlagService,
    private val preutfyllBorHosSøkerMedDataFraPersongrunnlagService: PreutfyllBorHosSøkerMedDataFraPersongrunnlagService,
    private val preutfyllBosattIRiketMedLagringIPersonopplyningsgrunnlagService: PreutfyllBosattIRiketMedLagringIPersonopplyningsgrunnlagService,
    private val persongrunnlagService: PersongrunnlagService,
    private val featureToggleService: FeatureToggleService,
) {
    fun preutfyllVilkår(vilkårsvurdering: Vilkårsvurdering) {
        if (vilkårsvurdering.behandling.kategori == BehandlingKategori.EØS) return
        if (vilkårsvurdering.behandling.type != BehandlingType.FØRSTEGANGSBEHANDLING) return

        if (featureToggleService.isEnabled(FeatureToggle.PREUTFYLLING_PERSONOPPLYSNIGSGRUNNLAG)) {
            persongrunnlagService.oppdaterRegisteropplysninger(vilkårsvurdering.behandling.id)
        }

        if (featureToggleService.isEnabled(FeatureToggle.PREUTFYLLING_VILKÅR)) {
            preutfyllBosattIRiketMedLagringIPersonopplyningsgrunnlagService.preutfyllBosattIRiket(vilkårsvurdering)
        }
        if (featureToggleService.isEnabled(FeatureToggle.PREUTFYLLING_VILKÅR_LOVLIG_OPPHOLD)) {
            preutfyllLovligOppholdMedPersongrunnlagService.preutfyllLovligOpphold(vilkårsvurdering)
        }
        if (featureToggleService.isEnabled(FeatureToggle.PREUTFYLLING_BOR_HOS_SØKER)) {
            preutfyllBorHosSøkerMedDataFraPersongrunnlagService.preutfyllBorFastHosSøkerVilkårResultat(vilkårsvurdering)
        }
    }

    fun preutfyllBosattIRiketForFødselshendelseBehandlinger(
        vilkårsvurdering: Vilkårsvurdering,
        identerVilkårSkalPreutfyllesFor: List<String>?,
    ) {
        preutfyllBosattIRiketMedLagringIPersonopplyningsgrunnlagService.preutfyllBosattIRiket(
            vilkårsvurdering = vilkårsvurdering,
            identerVilkårSkalPreutfyllesFor = identerVilkårSkalPreutfyllesFor,
        )
    }

    companion object {
        const val PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT = "Fylt ut automatisk fra registerdata i PDL\n"
    }
}
