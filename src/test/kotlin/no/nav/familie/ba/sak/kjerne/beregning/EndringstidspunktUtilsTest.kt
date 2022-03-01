package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EndringstidspunktUtilsTest {

    @Test
    fun `Skal finne endringer i beløp`() {
        val person1 = lagPerson()
        val person2 = lagPerson()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 2108,
                person = person2
            ),
        )

        val førsteEndringstidspunkt = førsteEndringstidspunkt(
            forrigeAndelerTilkjentYtelse = forrigeAndeler,
            andelerTilkjentYtelse = andeler
        )
        assertEquals(inneværendeMåned().minusYears(1), førsteEndringstidspunkt)
    }

    @Test
    fun `Skal finne reduksjon i fomdato`() {
        val person1 = lagPerson()
        val person2 = lagPerson()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1).minusMonths(2),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            ),
        )

        val førsteEndringstidspunkt = førsteEndringstidspunkt(
            forrigeAndelerTilkjentYtelse = forrigeAndeler,
            andelerTilkjentYtelse = andeler
        )
        assertEquals(inneværendeMåned().minusYears(1).minusMonths(2), førsteEndringstidspunkt)
    }
    
    @Test
    fun `Skal oppdage perioder som forsvinner`() {
        val person1 = lagPerson()
        val person2 = lagPerson()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusMonths(15),
                tom = inneværendeMåned().minusMonths(13),
                beløp = 1054,
                person = person2
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            ),
        )

        val førsteEndringstidspunkt = førsteEndringstidspunkt(
            forrigeAndelerTilkjentYtelse = forrigeAndeler,
            andelerTilkjentYtelse = andeler
        )
        assertEquals(inneværendeMåned().minusMonths(15), førsteEndringstidspunkt)
    }

    @Test
    fun `Skal oppdage nye perioder`() {
        val person1 = lagPerson()
        val person2 = lagPerson()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusMonths(15),
                tom = inneværendeMåned().minusMonths(13),
                beløp = 1054,
                person = person2
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            ),
        )

        val førsteEndringstidspunkt = førsteEndringstidspunkt(
            forrigeAndelerTilkjentYtelse = forrigeAndeler,
            andelerTilkjentYtelse = andeler
        )
        assertEquals(inneværendeMåned().minusMonths(15), førsteEndringstidspunkt)
    }

    @Test
    fun `Skal oppdage dersom vi har samme beløp, men på forskjellige personer`() {
        val person1 = lagPerson()
        val person2 = lagPerson()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person1
            ),
        )

        val førsteEndringstidspunkt = førsteEndringstidspunkt(
            forrigeAndelerTilkjentYtelse = forrigeAndeler,
            andelerTilkjentYtelse = andeler
        )
        assertEquals(inneværendeMåned().minusYears(1), førsteEndringstidspunkt)
    }
}
