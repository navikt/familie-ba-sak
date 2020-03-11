package no.nav.familie.ba.sak.behandling.vilkårsvurdering

import no.nav.familie.ba.sak.integrasjoner.domene.FAMILIERELASJONSROLLE
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo

data class Fakta(
        val personinfo: Personinfo
) {
    val søkerAlder = personinfo.fødselsdato
    val barn = personinfo.familierelasjoner.filter { relasjon -> relasjon.relasjonsrolle == FAMILIERELASJONSROLLE.BARN }
}