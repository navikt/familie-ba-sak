package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.vilkår.Vilkår

data class RestNyttVilkår(
        val personIdent: String,
        val vilkårType: Vilkår
)
