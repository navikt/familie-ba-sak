package no.nav.familie.ba.sak.behandling.resultat

import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import java.util.*

/**
 * TODO finn et mer passende navn enn krav.
 * Krav i denne sammenheng er både krav fra søker, men også "krav" fra forrige behandling som kan ha endret seg.
 * På en måte er alt krav fra søker, men "kravene" fra forrige behandling kan stamme fra en annen søknad.
 */
data class Krav(
        val personIdent: String,
        val ytelseType: YtelseType,
        val erSøknadskrav: Boolean,
        val resultatTyper: List<BehandlingResultatType> = emptyList()
) {

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val entitet: Krav = other as Krav
        return Objects.equals(hashCode(), entitet.hashCode())
    }

    /**
     * Vi sjekker likhet på person og ytelsetype.
     * Søknadskrav trumfer, men håndteres ikke av equals/hashcode.
     */
    override fun hashCode(): Int {
        return Objects.hash(personIdent, ytelseType)
    }
}
