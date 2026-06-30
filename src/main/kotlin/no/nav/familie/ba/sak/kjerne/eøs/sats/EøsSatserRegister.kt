package no.nav.familie.ba.sak.kjerne.eøs.sats

import no.nav.familie.ba.sak.common.Feil
import java.time.YearMonth

/**
 * Hardkodet register over satser per EØS-land.
 *
 * Satser for hvert land legges til i [hei](./EøsSatser.kt) vedlikeholdes i den tilhørende filen (f.eks. [EøsSatserPolen]).
 *
 */
object EøsSatserRegister {
    /**
     * Register over alle land med EØS-satser.
     */
    internal val satser: List<EøsSatser> =
        EøsSatser::class
            .sealedSubclasses
            .map { it.objectInstance ?: error("${it.simpleName} må være object") }

    /**
     * @return gjeldende sats for et land i en gitt måned eller null dersom ingen sats er registrert for landet i den aktuelle måneden.
     */
    fun finnSatsForLandIMåned(
        land: String,
        måned: YearMonth,
    ): EøsSats? =
        satser
            .find { it.land == land }
            ?.satser
            ?.find { it.erGyldigForMåned(måned) }

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
