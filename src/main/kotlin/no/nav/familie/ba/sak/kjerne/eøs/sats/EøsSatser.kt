package no.nav.familie.ba.sak.kjerne.eøs.sats

/**
 * Abstrakt klasse som representerer alle registrerte EØS-satser for ett bestemt land.
 *
 * Hvert land som har EØS-satser i systemet oppretter et eget `object` som arver fra denne klassen,
 * og legger inn sine satser i [satser]-listen.
 *
 * Eksempel på bruk:
 * ```kotlin
 * object EøsSatserPolen : EøsSatser() {
 *     override val land = "PL"
 *     override val satser = listOf(
 *         EøsSats(land = "PL", valuta = "PLN", intervall = Intervall.MÅNEDLIG,
 *                 beløp = BigDecimal("800"), fom = YearMonth.of(2025, 1)),
 *     )
 * }
 * ```
 *
 * Nye land registreres i [EøsSatsService.satser].
 */
abstract class EøsSatser {
    abstract val land: String
    abstract val satser: List<EøsSats>

    operator fun component1(): String = land

    operator fun component2(): List<EøsSats> = satser
}

/**
 * EØS-satser for Polen (PL) i polske zloty (PLN).
 */
object EøsSatserPolen : EøsSatser() {
    override val land = "PL"
    override val satser: List<EøsSats> = listOf()
}
