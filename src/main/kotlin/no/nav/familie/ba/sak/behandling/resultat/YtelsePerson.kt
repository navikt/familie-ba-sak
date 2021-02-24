package no.nav.familie.ba.sak.behandling.resultat

import no.nav.familie.ba.sak.beregning.domene.YtelseType
import java.time.YearMonth
import java.util.*

/**
 * periodeStartForRentOpphør: settes dersom det kun er reduksjon på person som fører til opphør på personen.
 */
data class YtelsePerson(
        val personIdent: String,
        val ytelseType: YtelseType,
        val erFramstiltKravForINåværendeBehandling: Boolean,
        val erFramstiltKravForITidligereBehandling: Boolean,
        val resultater: Set<YtelsePersonResultat> = emptySet(),
        val periodeStartForRentOpphør: YearMonth? = null
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

enum class YtelsePersonResultat(val displayName: String) {
    INNVILGET(displayName = "Innvilget"),
    AVSLÅTT(displayName = "Avslått"),
    OPPHØRT(displayName = "Reduksjon som har ført til opphør"),
    IKKE_VURDERT(displayName = "Ikke vurdert"),
    ENDRET(displayName = "Endret"),
}
