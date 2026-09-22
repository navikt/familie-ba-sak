package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak

enum class ExceptionStrategi {
    SWALLOW,
    THROW,
    ;

    companion object {
        fun fraBehandlingÅrsak(årsak: BehandlingÅrsak): ExceptionStrategi =
            when (årsak) {
                BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD -> THROW
                else -> SWALLOW
            }
    }
}
