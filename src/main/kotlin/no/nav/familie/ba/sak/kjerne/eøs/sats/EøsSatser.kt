package no.nav.familie.ba.sak.kjerne.eøs.sats

import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import java.time.YearMonth

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
    override val satser: List<EøsSats> =
        listOf(
            EøsSats(
                land = land,
                valuta = "PLN",
                beløp = 500.0.toBigDecimal(),
                fom = YearMonth.of(2016, 4),
                tom = YearMonth.of(2023, 12),
                intervall = Intervall.MÅNEDLIG,
            ),
            EøsSats(
                land = land,
                valuta = "PLN",
                beløp = 800.0.toBigDecimal(),
                fom = YearMonth.of(2024, 1),
                tom = null,
                intervall = Intervall.MÅNEDLIG,
            ),
        )
}
