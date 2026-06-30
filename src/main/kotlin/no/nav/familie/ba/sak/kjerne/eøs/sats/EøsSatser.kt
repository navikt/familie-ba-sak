package no.nav.familie.ba.sak.kjerne.eøs.sats

/**
 * Sealed klasse som representerer alle registrerte satser for ett bestemt EØS-land.
 *
 * Hvert land som har satser i systemet oppretter et eget `object` som arver fra denne klassen,
 * og legger inn sine satser i [satser]-listen.
 *
 * @sample EøsSatserPolen
 */
sealed class EøsSatser {
    abstract val land: String
    abstract val satser: List<EøsSats>

    operator fun component1(): String = land

    operator fun component2(): List<EøsSats> = satser
}

/**
 * Satser for Polen (PL).
 */
object EøsSatserPolen : EøsSatser() {
    override val land = "PL"
    override val satser: List<EøsSats> = listOf()
}
