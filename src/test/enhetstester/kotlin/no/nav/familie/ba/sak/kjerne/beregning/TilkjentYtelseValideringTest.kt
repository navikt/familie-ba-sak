package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.time.YearMonth

class TilkjentYtelseValideringTest {

    val gyldigEtterbetalingFom = hentGyldigEtterbetalingFom(LocalDateTime.now())

    @Test
    fun `Skal returnere true når person har etterbetaling som er mer enn 3 år tilbake i tid`() {
        val person1 = tilfeldigPerson()
        val person2 = tilfeldigPerson()

        val andeler1 = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 2108,
                person = person1,
            ),
        )
        val forrigeAndeler1 = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 2108,
                person = person1,
            ),
        )

        Assertions.assertFalse(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = forrigeAndeler1,
                andelerForPerson = andeler1,
                gyldigEtterbetalingFom = gyldigEtterbetalingFom,
            ),
        )
        Assertions.assertTrue(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = emptyList(),
                andelerForPerson = andeler1,
                gyldigEtterbetalingFom = gyldigEtterbetalingFom,
            ),
        )
        Assertions.assertTrue(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = emptyList(),
                andelerForPerson = andeler1,
                gyldigEtterbetalingFom = gyldigEtterbetalingFom,
            ),
        )

        val forrigeAndeler2 = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2,
            ),
        )
        val andeler2 = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 2108,
                person = person2,
            ),
        )
        Assertions.assertTrue(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = forrigeAndeler2,
                andelerForPerson = andeler2,
                gyldigEtterbetalingFom = gyldigEtterbetalingFom,
            ),
        )
        Assertions.assertTrue(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = emptyList(),
                andelerForPerson = andeler2,
                gyldigEtterbetalingFom = gyldigEtterbetalingFom,
            ),
        )
        Assertions.assertTrue(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = emptyList(),
                andelerForPerson = andeler2,
                gyldigEtterbetalingFom = gyldigEtterbetalingFom,
            ),
        )
    }

    @Test
    fun `Skal returnere false ved uendret tilkjent ytelse andel mer enn 3 år tilbake`() {
        val person1 = tilfeldigPerson()

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 2108,
                person = person1,
            ),
        )

        Assertions.assertFalse(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = andeler,
                andelerForPerson = andeler,
                gyldigEtterbetalingFom = gyldigEtterbetalingFom,
            ),
        )
    }

    @Test
    fun `Skal returnere false ved reduksjon av beløp mer enn 3 år tilbake`() {
        val person1 = tilfeldigPerson()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 2108,
                person = person1,
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person1,
            ),
        )

        Assertions.assertFalse(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = forrigeAndeler,
                andelerForPerson = andeler,
                gyldigEtterbetalingFom = gyldigEtterbetalingFom,
            ),
        )
        Assertions.assertFalse(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = forrigeAndeler,
                andelerForPerson = emptyList(),
                gyldigEtterbetalingFom = gyldigEtterbetalingFom,
            ),
        )
    }

    @Test
    fun `Skal returnere false ved endring av tilkjent ytelse andel som er mindre enn 3 år tilbake i tid`() {
        val person1 = tilfeldigPerson()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1,
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1,
            ),
        )

        Assertions.assertFalse(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = forrigeAndeler,
                andelerForPerson = andeler,
                gyldigEtterbetalingFom = hentGyldigEtterbetalingFom(LocalDateTime.now().minusYears(2)),
            ),
        )

        Assertions.assertFalse(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = emptyList(),
                andelerForPerson = andeler,
                gyldigEtterbetalingFom = hentGyldigEtterbetalingFom(LocalDateTime.now().minusYears(2)),
            ),
        )

        Assertions.assertFalse(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = emptyList(),
                andelerForPerson = andeler,
                gyldigEtterbetalingFom = hentGyldigEtterbetalingFom(LocalDateTime.now().minusYears(2)),
            ),
        )

        Assertions.assertFalse(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = forrigeAndeler,
                andelerForPerson = emptyList(),
                gyldigEtterbetalingFom = hentGyldigEtterbetalingFom(LocalDateTime.now().minusYears(2)),
            ),
        )
    }

    @Test
    fun `Skal kaste feil for satsendring hvis person har fått fjernet andel som hadde utbetaling i forrige behandling`() {
        val person = lagPerson(type = PersonType.BARN)
        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2023, 5),
                beløp = 1054,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = person.aktør,
            ),
        )

        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2020, 1),
                tom = YearMonth.of(2023, 2),
                beløp = 1054,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = person.aktør,
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2023, 3),
                tom = YearMonth.of(2023, 5),
                beløp = 1083,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = person.aktør,
            ),
        )

        assertThrows<Feil> {
            TilkjentYtelseValidering.validerAtSatsendringKunOppdatererSatsPåEksisterendePerioder(
                forrigeAndelerTilkjentYtelse = forrigeAndeler,
                andelerTilkjentYtelse = nåværendeAndeler,
            )
        }
    }

    @Test
    fun `Skal kaste feil for satsendring hvis person har fått lagt til ny andel i periode som ikke hadde utbetaling før`() {
        val person = lagPerson(type = PersonType.BARN)
        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2020, 1),
                tom = YearMonth.of(2023, 2),
                beløp = 1054,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = person.aktør,
            ),
        )

        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2023, 2),
                beløp = 1054,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = person.aktør,
            ),
        )

        assertThrows<Feil> {
            TilkjentYtelseValidering.validerAtSatsendringKunOppdatererSatsPåEksisterendePerioder(
                forrigeAndelerTilkjentYtelse = forrigeAndeler,
                andelerTilkjentYtelse = nåværendeAndeler,
            )
        }
    }

    @Test
    fun `Skal kaste ikke kaste feil hvis det eneste som er gjort er å oppdatere sats`() {
        val person = lagPerson(type = PersonType.BARN)
        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2023, 5),
                beløp = 1054,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = person.aktør,
            ),
        )

        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2023, 2),
                beløp = 1054,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = person.aktør,
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2023, 3),
                tom = YearMonth.of(2023, 5),
                beløp = 1083,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = person.aktør,
            ),
        )

        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtSatsendringKunOppdatererSatsPåEksisterendePerioder(
                forrigeAndelerTilkjentYtelse = forrigeAndeler,
                andelerTilkjentYtelse = nåværendeAndeler,
            )
        }
    }
}
