package no.nav.familie.ba.sak.kjerne.eøs.sats

import no.nav.familie.ba.sak.common.Feil
import java.time.YearMonth

/**
 * Register over satser per EØS-land.
 *
 * Satser for hvert land legges til i [EøsSatser.kt](./EøsSatser.kt) (f.eks. [EøsSatserPolen]).
 *
 */
object EøsSatserRegister {
    /**
     * Register over alle land med EØS-satser.
     */
    internal val satser: List<EøsSats>
        get() =
            EøsSatser::class
                .sealedSubclasses
                .map { it.objectInstance ?: error("${it.simpleName} må være object") }
                .flatMap { it.satser }

    /**
     * @return gjeldende sats for et land i en gitt måned eller null dersom ingen sats er registrert for landet i den aktuelle måneden.
     */
    fun finnSatsForLandIMåned(
        land: String,
        måned: YearMonth,
    ): EøsSats? = satser.find { it.land == land && it.erGyldigForMåned(måned) }

    /**
     * @return gjeldende sats for et land i en gitt måned
     * @throws [Feil] hvis ingen sats er registrert for landet i den aktuelle måneden.
     */
    fun hentSatsForLandIMåned(
        land: String,
        måned: YearMonth,
    ): EøsSats =
        finnSatsForLandIMåned(land, måned)
            ?: throw Feil("Ingen EØS-sats registrert for land $land i måned $måned.")
}
