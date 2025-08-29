package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.springframework.stereotype.Service

@Service
class PreutfyllVilkårService(
    private val preutfyllLovligOppholdService: PreutfyllLovligOppholdService,
    private val preutfyllBosattIRiketService: PreutfyllBosattIRiketService,
    private val preutfyllBorHosSøkerService: PreutfyllBorHosSøkerService,
    private val featureToggleService: FeatureToggleService,
) {
    fun preutfyllVilkår(vilkårsvurdering: Vilkårsvurdering) {
        if (vilkårsvurdering.behandling.kategori == BehandlingKategori.EØS) return

        if (featureToggleService.isEnabled(FeatureToggle.PREUTFYLLING_VILKÅR)) {
            preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering)
        }
        if (featureToggleService.isEnabled(FeatureToggle.PREUTFYLLING_VILKÅR_LOVLIG_OPPHOLD)) {
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering)
        }
        if (featureToggleService.isEnabled(FeatureToggle.PREUTFYLLING_BOR_HOS_SØKER)) {
            preutfyllBorHosSøkerService.preutfyllBorFastHosSøkerVilkårResultat(vilkårsvurdering)
        }
    }

    fun preutfyllBosattIRiket(vilkårsvurdering: Vilkårsvurdering) {
        if (featureToggleService.isEnabled(FeatureToggle.PREUTFYLLING_VILKÅR)) {
            preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering)
        }
    }
}
