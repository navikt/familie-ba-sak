package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import java.time.YearMonth
import java.util.Objects

/**
 * Representer en person det er framstilt krav for nå eller tidligere
 * @property personIdent Personens ident
 * @property ytelseType Typen ytelse
 * @property kravOpprinnelse Om krav for person er framstilt nå, ligger på behandling fra tidligere, eller begge deler
 * @property resultater Hvilke konsekvenser _denne_ behandlingen har for personen
 * @property ytelseSlutt Tom-dato på personens siste andel etter denne behandlingen (utbetalingsslutt)
 */
data class YtelsePerson(
    val personIdent: String,
    val ytelseType: YtelseType,
    val kravOpprinnelse: List<KravOpprinnelse>,
    val resultater: Set<YtelsePersonResultat> = emptySet(),
    val ytelseSlutt: YearMonth? = null
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

    fun erFramstiltKravForIInneværendeBehandling() = this.kravOpprinnelse.contains(KravOpprinnelse.INNEVÆRENDE)

    fun erFramstiltKravForITidligereBehandling() = this.kravOpprinnelse.contains(KravOpprinnelse.TIDLIGERE)
}

enum class YtelsePersonResultat(val displayName: String) {
    INNVILGET(displayName = "Innvilget"),
    AVSLÅTT(displayName = "Avslått"),
    OPPHØRT(displayName = "Reduksjon som har ført til opphør"),
    IKKE_VURDERT(displayName = "Ikke vurdert"),
    ENDRET(displayName = "Endret"),
}

enum class KravOpprinnelse(val displayName: String) {
    INNEVÆRENDE(displayName = "Krav framstilt i inneværende behandling"),
    TIDLIGERE(displayName = "Krav framstilt tidligere behandling"),
}
