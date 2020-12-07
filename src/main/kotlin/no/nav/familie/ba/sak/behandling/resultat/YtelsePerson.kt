package no.nav.familie.ba.sak.behandling.resultat

import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import java.util.*

data class YtelsePerson(
        val personIdent: String,
        val ytelseType: YtelseType,
        val erSøktOmINåværendeBehandling: Boolean,
        val resultatTyper: List<BehandlingResultatType> = emptyList()
) {

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val entitet: YtelsePerson = other as YtelsePerson
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
