package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.springframework.stereotype.Service

@Service
class PreutfyllVilkårService(
    private val preutfyllLovligOppholdService: PreutfyllLovligOppholdService,
    private val preutfyllBosattIRiketService: PreutfyllBosattIRiketService,
    private val unleashService: UnleashNextMedContextService,
) {
    fun preutfyllVilkår(vilkårsvurdering: Vilkårsvurdering) {
        if (vilkårsvurdering.behandling.kategori == BehandlingKategori.EØS) return

        if (unleashService.isEnabled(FeatureToggle.PREUTFYLLING_VILKÅR)) {
            preutfyllBosattIRiketService.prefutfyllBosattIRiket(vilkårsvurdering)
        }
        if (unleashService.isEnabled(FeatureToggle.PREUTFYLLING_VILKÅR_LOVLIG_OPPHOLD)) {
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering)
        }
    }
}
