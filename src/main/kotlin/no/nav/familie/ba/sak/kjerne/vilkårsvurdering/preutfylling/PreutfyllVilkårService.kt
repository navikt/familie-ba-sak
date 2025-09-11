package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class PreutfyllVilkårService(
    private val preutfyllLovligOppholdService: PreutfyllLovligOppholdService,
    private val preutfyllBosattIRiketService: PreutfyllBosattIRiketService,
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
    }

    @Transactional(propagation = Propagation.NESTED)
    fun preutfyllBosattIRiket(
        vilkårsvurdering: Vilkårsvurdering,
        barnSomVilkårSkalPreutfyllesFor: List<String>? = null,
    ) {
        if (featureToggleService.isEnabled(FeatureToggle.PREUTFYLLING_VILKÅR)) {
            preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, barnSomVilkårSkalPreutfyllesFor)
        }
    }
}
