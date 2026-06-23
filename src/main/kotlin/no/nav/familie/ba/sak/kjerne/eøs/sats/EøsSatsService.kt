package no.nav.familie.ba.sak.kjerne.eøs.sats

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.eøs.sats.EøsSatsService.satser
import java.time.YearMonth

/**
 * Hardkodet register over EØS-satser per land, analogt med [no.nav.familie.ba.sak.kjerne.beregning.SatsService].
 *
 * Nye land legges til i [satser] som egne [EøsSatser]-objekter.
 * Satser innen hvert land vedlikeholdes i den tilhørende filen (f.eks. [EøsSatserPolen]).
 */
object EøsSatsService {
    /**
     * Register over alle land med EØS-satser.
     * Nye land registreres her når de legges til i systemet.
     */
    internal val satser: List<EøsSatser> =
        listOf(
            EøsSatserPolen,
        )

    /**
     * @return gjeldende sats for et land i en gitt måned eller null dersom ingen sats er registrert for landet i den aktuelle måneden.
     */
    fun finnGjeldendeSatsForLand(
        land: String,
        måned: YearMonth = YearMonth.now(),
    ): EøsSats? =
        satser
            .find { it.land == land }
            ?.satser
            ?.find { it.erGyldigForMåned(måned) }

    /**
     * @return den nyeste satsen for et land.
     * @throws [Feil] dersom ingen sats er registrert for landet.
     */
    fun hentSisteSatsForLand(land: String): EøsSats =
        satser.find { it.land == land }?.satser?.maxByOrNull { it.fom }
            ?: throw Feil("Ingen EØS-sats registrert for land: $land")
}
