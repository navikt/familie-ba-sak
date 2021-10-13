package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerAtAlleOpprettedeEndringerErUtfylt
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerDeltBosted
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerIngenOverlappendeEndring
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerPeriodeInnenforTilkjentytelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class EndretUtbetalingAndelValideringTest {

    @Test
    fun `skal sjekke at en endret periode ikke overlapper med eksisternede endrete perioder`() {
        val barn1 = tilfeldigPerson()
        val barn2 = tilfeldigPerson()
        val endretUtbetalingAndel = EndretUtbetalingAndel(
            behandlingId = 1,
            person = barn1,
            fom = YearMonth.of(2020, 2),
            tom = YearMonth.of(2020, 6),
            årsak = Årsak.DELT_BOSTED,
            begrunnelse = "begrunnelse",
            prosent = BigDecimal(100),
            søknadstidspunkt = LocalDate.now(),
            avtaletidspunktDeltBosted = LocalDate.now()
        )

        val feil = assertThrows<UtbetalingsikkerhetFeil> {
            validerIngenOverlappendeEndring(
                endretUtbetalingAndel,
                listOf(
                    endretUtbetalingAndel.copy(
                        fom = YearMonth.of(2018, 4),
                        tom = YearMonth.of(2019, 2)
                    ),
                    endretUtbetalingAndel.copy(
                        fom = YearMonth.of(2020, 4),
                        tom = YearMonth.of(2021, 2)
                    )
                )
            )
        }
        assertEquals(
            "Perioden som blir forsøkt lagt til overlapper med eksisterende periode på person.",
            feil.melding
        )

        // Resterende kall skal validere ok.
        validerIngenOverlappendeEndring(
            endretUtbetalingAndel,
            listOf(
                endretUtbetalingAndel.copy(
                    fom = endretUtbetalingAndel.tom!!.plusMonths(1),
                    tom = endretUtbetalingAndel.tom!!.plusMonths(10)
                )
            )
        )
        validerIngenOverlappendeEndring(
            endretUtbetalingAndel,
            listOf(endretUtbetalingAndel.copy(person = barn2))
        )
        validerIngenOverlappendeEndring(
            endretUtbetalingAndel,
            listOf(endretUtbetalingAndel.copy(årsak = Årsak.EØS_SEKUNDÆRLAND))
        )
    }

    @Test
    fun `skal sjekke at en endret periode ikke strekker seg utover ytterpunktene for tilkjent ytelse`() {
        val barn1 = tilfeldigPerson()
        val barn2 = tilfeldigPerson()

        val andelTilkjentYtelser = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2020, 2).toString(),
                tom = YearMonth.of(2020, 4).toString(),
                person = barn1
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2020, 7).toString(),
                tom = YearMonth.of(2020, 10).toString(),
                person = barn1
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2018, 10).toString(),
                tom = YearMonth.of(2021, 10).toString(),
                person = barn2
            ),
        )

        val endretUtbetalingAndel = EndretUtbetalingAndel(
            behandlingId = 1,
            person = barn1,
            fom = YearMonth.of(2020, 2),
            tom = YearMonth.of(2020, 6),
            årsak = Årsak.DELT_BOSTED,
            begrunnelse = "begrunnelse",
            prosent = BigDecimal(100),
            søknadstidspunkt = LocalDate.now(),
            avtaletidspunktDeltBosted = LocalDate.now()
        )

        var feil = assertThrows<UtbetalingsikkerhetFeil> {
            validerPeriodeInnenforTilkjentytelse(endretUtbetalingAndel, emptyList())
        }
        assertEquals(
            "Det er ingen tilkjent ytelse for personen det blir forsøkt lagt til en endret periode for.",
            feil.melding
        )

        val endretUtbetalingAndelerSomIkkeValiderer = listOf(
            endretUtbetalingAndel.copy(fom = YearMonth.of(2020, 1), tom = YearMonth.of(2020, 11)),
            endretUtbetalingAndel.copy(fom = YearMonth.of(2020, 1), tom = YearMonth.of(2020, 4)),
            endretUtbetalingAndel.copy(fom = YearMonth.of(2020, 2), tom = YearMonth.of(2020, 11))
        )

        endretUtbetalingAndelerSomIkkeValiderer.forEach {
            feil = assertThrows<UtbetalingsikkerhetFeil> {
                validerPeriodeInnenforTilkjentytelse(it, andelTilkjentYtelser)
            }
            assertEquals(
                "Det er ingen tilkjent ytelse for personen det blir forsøkt lagt til en endret periode for.",
                feil.melding
            )
        }

        val endretUtbetalingAndelerSomValiderer = listOf(
            endretUtbetalingAndel,
            endretUtbetalingAndel.copy(fom = YearMonth.of(2020, 2), tom = YearMonth.of(2020, 10)),
            endretUtbetalingAndel.copy(fom = YearMonth.of(2018, 10), tom = YearMonth.of(2021, 10), person = barn2)
        )

        endretUtbetalingAndelerSomValiderer.forEach { validerPeriodeInnenforTilkjentytelse(it, andelTilkjentYtelser) }
    }

    @Test
    fun `skal sjekke at det eksisterer delt bostedsats ved opprettelse av endring med årsak delt bosted`() {
        val barn1 = tilfeldigPerson()
        val andelTilkjentYtelse = lagAndelTilkjentYtelse(
            fom = YearMonth.of(2020, 2).toString(),
            tom = YearMonth.of(2020, 4).toString(),
            person = barn1
        )
        val endretUtbetalingAndel = EndretUtbetalingAndel(
            behandlingId = 1,
            person = barn1,
            fom = YearMonth.of(2020, 2),
            tom = YearMonth.of(2020, 6),
            årsak = Årsak.DELT_BOSTED,
            begrunnelse = "begrunnelse",
            prosent = BigDecimal(100),
            søknadstidspunkt = LocalDate.now(),
            avtaletidspunktDeltBosted = LocalDate.now()
        )
        val feil = assertThrows<UtbetalingsikkerhetFeil> {
            validerDeltBosted(endretUtbetalingAndel, listOf(andelTilkjentYtelse))
        }
        assertEquals(
            "Det er ingen sats for delt bosted i perioden det opprettes en endring med årsak delt bosted for.",
            feil.melding
        )

        validerDeltBosted(endretUtbetalingAndel, listOf(andelTilkjentYtelse.copy(prosent = BigDecimal(50))))
    }

    @Test
    fun `sjekk at alle endrede utbetalingsandeler validerer`() {
        val endretUtbetalingAndel1 = lagEndretUtbetalingAndel(person = tilfeldigPerson())
        val endretUtbetalingAndel2 = lagEndretUtbetalingAndel(person = tilfeldigPerson())
        validerAtAlleOpprettedeEndringerErUtfylt(listOf(endretUtbetalingAndel1, endretUtbetalingAndel2))

        val feil = assertThrows<UtbetalingsikkerhetFeil> {
            validerAtAlleOpprettedeEndringerErUtfylt(
                listOf(
                    endretUtbetalingAndel1,
                    endretUtbetalingAndel2.copy(fom = null)
                )
            )
        }
        assertEquals(
            "Det er opprettet instanser av EndretUtbetalingandel som ikke er fylt ut før navigering til neste sted.",
            feil.melding
        )
    }
}
