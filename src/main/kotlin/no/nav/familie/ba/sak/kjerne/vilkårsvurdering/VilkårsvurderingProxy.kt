package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class VilkårsvurderingProxy(val featureToggleService: FeatureToggleService) {

    fun flyttResultaterTilInitielt(
        initiellVilkårsvurdering: Vilkårsvurdering,
        aktivVilkårsvurdering: Vilkårsvurdering,
        forrigeBehandlingVilkårsvurdering: Vilkårsvurdering? = null,
        løpendeUnderkategori: BehandlingUnderkategori? = null
    ): Pair<Vilkårsvurdering, Vilkårsvurdering> {
        val eksisterende = VilkårsvurderingUtils.flyttResultaterTilInitielt(
            initiellVilkårsvurdering = initiellVilkårsvurdering,
            aktivVilkårsvurdering = aktivVilkårsvurdering,
            forrigeBehandlingVilkårsvurdering = forrigeBehandlingVilkårsvurdering,
            løpendeUnderkategori = løpendeUnderkategori
        )
        if (featureToggleService.isEnabled(FeatureToggleConfig.NY_VILKÅRSVURDERINGFLYTTING)) {
            val ny = VilkårsvurderingResultatFlytter.flyttResultaterTilInitielt(
                initiellVilkårsvurdering = initiellVilkårsvurdering,
                aktivVilkårsvurdering = aktivVilkårsvurdering,
                løpendeUnderkategori = løpendeUnderkategori,
                personResultaterFraForrigeBehandling = forrigeBehandlingVilkårsvurdering?.personResultater
            )
            val oppdatertLik = eksisterende.first == ny.first
            val aktivLik = eksisterende.second.personResultater == ny.second
            if (!oppdatertLik) {
                logger.warn("Forskjell mellom lik og gammel vilkårsvurderings-flytter for oppdaterte vilkår. Gammel: ${eksisterende.first}. Ny: ${ny.first}")
            }
            if (!aktivLik) {
                logger.warn("Forskjell mellom lik og gammel vilkårsvurderings-flytter for aktive vilkår. Gammel: ${eksisterende.second.personResultater}. Ny: ${ny.second}")
            }
        }
        return eksisterende
    }

    companion object {
        val logger = LoggerFactory.getLogger(VilkårsvurderingProxy::class.java)
    }
}
