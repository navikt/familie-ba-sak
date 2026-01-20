package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.kontrakter.felles.Fødselsnummer

data class NyttVilkårDto(
    val personIdent: String,
    val vilkårType: Vilkår,
) {
    // Bruker init til å validere personidenten
    init {
        Fødselsnummer(personIdent)
    }
}
