package no.nav.familie.ba.sak.behandling.vilkårsvurdering

import no.nav.familie.ba.sak.behandling.domene.vilkår.VilkårType
import no.nav.familie.ba.sak.fakta.vilkårsvurdering.Vilkår
import no.nav.nare.core.evaluations.Evaluering


internal val rettenTilBarnetrygd = Vilkår(
        vilkårType = VilkårType.UNDER_18_ÅR_OG_BOR_MED_SØKER,
        implementasjon = {
            when {
                this.barn.isNotEmpty() -> Evaluering.ja(
                        "Barn er under 18 år og bor med søker"
                )
                else -> Evaluering.nei("Barn er ikke under 18 år eller bor ikke med søker")
            }
        })