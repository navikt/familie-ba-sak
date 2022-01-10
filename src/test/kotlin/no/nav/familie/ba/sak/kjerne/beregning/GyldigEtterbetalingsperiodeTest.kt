package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class GyldigEtterbetalingsperiodeTest {

    @Test
    fun `Skal kaste feil ved etterbetaling som er mer enn 3 år tilbake i tid`() {
        val person1 = tilfeldigPerson()
        val person2 = tilfeldigPerson()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 2108,
                person = person2
            ),
        )

        assertThrows<UtbetalingsikkerhetFeil> {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarGyldigEtterbetalingsperiode(
                forrigeAndelerTilkjentYtelse = forrigeAndeler,
                andelerTilkjentYtelse = andeler,
                kravDato = LocalDateTime.now()
            )
        }

        assertThrows<UtbetalingsikkerhetFeil> {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarGyldigEtterbetalingsperiode(
                forrigeAndelerTilkjentYtelse = emptyList(),
                andelerTilkjentYtelse = andeler,
                kravDato = LocalDateTime.now()
            )
        }
    }

    @Test
    fun `Skal ikke kaste feil ved uendret tilkjent ytelse andel mer enn 3 år tilbake`() {
        val person1 = tilfeldigPerson()

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 2108,
                person = person1
            ),
        )

        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarGyldigEtterbetalingsperiode(
                forrigeAndelerTilkjentYtelse = andeler,
                andelerTilkjentYtelse = andeler,
                kravDato = LocalDateTime.now()
            )
        }
    }

    @Test
    fun `Skal ikke kaste feil ved reduksjon av beløp mer enn 3 år tilbake`() {
        val person1 = tilfeldigPerson()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 2108,
                person = person1
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person1
            ),
        )

        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarGyldigEtterbetalingsperiode(
                forrigeAndelerTilkjentYtelse = forrigeAndeler,
                andelerTilkjentYtelse = andeler,
                kravDato = LocalDateTime.now()
            )
        }

        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarGyldigEtterbetalingsperiode(
                forrigeAndelerTilkjentYtelse = forrigeAndeler,
                andelerTilkjentYtelse = emptyList(),
                kravDato = LocalDateTime.now()
            )
        }
    }

    @Test
    fun `Skal ikke kaste feil ved endring av tilkjent ytelse andel som er mindre enn 3 år tilbake i tid`() {
        val person1 = tilfeldigPerson()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
        )

        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarGyldigEtterbetalingsperiode(
                forrigeAndelerTilkjentYtelse = forrigeAndeler,
                andelerTilkjentYtelse = andeler,
                kravDato = LocalDateTime.now().minusYears(2)
            )
        }

        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarGyldigEtterbetalingsperiode(
                forrigeAndelerTilkjentYtelse = emptyList(),
                andelerTilkjentYtelse = andeler,
                kravDato = LocalDateTime.now().minusYears(2)
            )
        }

        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarGyldigEtterbetalingsperiode(
                forrigeAndelerTilkjentYtelse = forrigeAndeler,
                andelerTilkjentYtelse = emptyList(),
                kravDato = LocalDateTime.now().minusYears(2)
            )
        }
    }

    @Test
    fun `Skal ikke kaste feil forskjellige typer ytelse`() {
        val person1 = tilfeldigPerson()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 660,
                person = person1,
                ytelseType = YtelseType.SMÅBARNSTILLEGG
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 660,
                person = person1,
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
            ),
        )

        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarGyldigEtterbetalingsperiode(
                forrigeAndelerTilkjentYtelse = forrigeAndeler,
                andelerTilkjentYtelse = andeler,
                kravDato = LocalDateTime.now().minusYears(2)
            )
        }

        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarGyldigEtterbetalingsperiode(
                forrigeAndelerTilkjentYtelse = emptyList(),
                andelerTilkjentYtelse = andeler,
                kravDato = LocalDateTime.now().minusYears(2)
            )
        }

        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarGyldigEtterbetalingsperiode(
                forrigeAndelerTilkjentYtelse = forrigeAndeler,
                andelerTilkjentYtelse = emptyList(),
                kravDato = LocalDateTime.now().minusYears(2)
            )
        }
    }
}
