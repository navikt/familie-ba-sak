package no.nav.familie.ba.sak.kjerne.endretutbetaling

import io.mockk.mockk
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerAtAlleOpprettedeEndringerErUtfylt
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerAtEndringerErTilknyttetAndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerDeltBosted
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerIngenOverlappendeEndring
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerPeriodeInnenforTilkjentytelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import kotlin.random.Random

class EndretUtbetalingAndelValideringTest {

    val søker = lagPerson(type = PersonType.SØKER)
    val barn = lagPerson(type = PersonType.BARN)
    val endretUtbetalingAndelUtvidetNullutbetaling =
        endretUtbetalingAndel(søker, YtelseType.UTVIDET_BARNETRYGD, BigDecimal.ZERO)
    val endretUtbetalingAndelDeltBostedNullutbetaling =
        endretUtbetalingAndel(barn, YtelseType.ORDINÆR_BARNETRYGD, BigDecimal.ZERO)

    val endretUtbetalingAndelUtvidetFullUtbetaling =
        endretUtbetalingAndel(søker, YtelseType.UTVIDET_BARNETRYGD, BigDecimal.valueOf(100))
    val endretUtbetalingAndelDeltBostedFullUtbetaling =
        endretUtbetalingAndel(barn, YtelseType.ORDINÆR_BARNETRYGD, BigDecimal.valueOf(100))

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
                fom = YearMonth.of(2020, 2),
                tom = YearMonth.of(2020, 4),
                person = barn1
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2020, 7),
                tom = YearMonth.of(2020, 10),
                person = barn1
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2018, 10),
                tom = YearMonth.of(2021, 10),
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
            fom = YearMonth.of(2020, 2),
            tom = YearMonth.of(2020, 4),
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

        val feil = assertThrows<FunksjonellFeil> {
            validerAtAlleOpprettedeEndringerErUtfylt(
                listOf(
                    endretUtbetalingAndel1,
                    endretUtbetalingAndel2.copy(fom = null)
                )
            )
        }
        assertEquals(
            "Det er opprettet instanser av EndretUtbetalingandel som ikke er fylt ut før navigering til neste steg.",
            feil.melding
        )
    }

    @Test
    fun `sjekk at alle endrede utbetalingsandeler er tilknyttet andeltilkjentytelser`() {
        val endretUtbetalingAndel1 = lagEndretUtbetalingAndel(person = tilfeldigPerson())
        val feil = assertThrows<FunksjonellFeil> {
            validerAtEndringerErTilknyttetAndelTilkjentYtelse(listOf(endretUtbetalingAndel1))
        }
        assertEquals(
            "Det er opprettet instanser av EndretUtbetalingandel som ikke er tilknyttet noen andeler. De må enten lagres eller slettes av SB.",
            feil.melding
        )

        val andelTilkjentYtelse: AndelTilkjentYtelse = mockk()
        validerAtEndringerErTilknyttetAndelTilkjentYtelse(
            listOf(
                endretUtbetalingAndel1.copy(andelTilkjentYtelser = mutableListOf(andelTilkjentYtelse))
            )
        )
    }

    @Test
    fun `skal ikke feile dersom de er en utvidet endring og delt bosded endring med samme periode og prosent`() {
        validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer(
            listOf(endretUtbetalingAndelUtvidetNullutbetaling, endretUtbetalingAndelDeltBostedNullutbetaling)
        )
        Assertions.assertDoesNotThrow {
            validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer(
                listOf(endretUtbetalingAndelUtvidetNullutbetaling, endretUtbetalingAndelDeltBostedNullutbetaling)
            )
        }
    }

    @Test
    fun `skal ikke feile dersom endring strekker seg over utvidet og ordinær ytelse`() {
        val fom1 = inneværendeMåned().minusMonths(2)
        val tom1 = inneværendeMåned().minusMonths(2)
        val fom2 = inneværendeMåned().minusMonths(1)
        val tom2 = inneværendeMåned().minusMonths(1)

        val utvidetEndring = lagEndretUtbetalingAndel(
            fom = fom1,
            tom = tom2,
            person = søker,
            årsak = Årsak.DELT_BOSTED,
            prosent = BigDecimal.ZERO,
            andelTilkjentYtelser = mutableListOf(
                lagAndelTilkjentYtelse(
                    fom = fom1,
                    tom = tom1,
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD
                ),
                lagAndelTilkjentYtelse(
                    fom = fom2,
                    tom = tom2,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD
                )
            )
        )

        val deltBostedEndring =
            endretUtbetalingAndel(
                barn, YtelseType.ORDINÆR_BARNETRYGD, BigDecimal.ZERO,
                fomUtvidet = fom1,
                tomUtvidet = tom2,
            )

        Assertions.assertThrows(FunksjonellFeil::class.java) {
            validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer(
                listOf(utvidetEndring, deltBostedEndring)
            )
        }
    }

    @Test
    fun `skal kaste feil dersom det er en endring på utvidet ytelse uten en endring på delt bosted i samme periode`() {

        Assertions.assertThrows(FunksjonellFeil::class.java) {
            validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer(
                listOf(endretUtbetalingAndelUtvidetNullutbetaling)
            )
        }
    }

    @Test
    fun `skal kaste feil dersom det er en endring på utvidet ytelse og delt bosted med forskjellig prosent`() {

        Assertions.assertThrows(FunksjonellFeil::class.java) {
            validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer(
                listOf(
                    endretUtbetalingAndelUtvidetNullutbetaling,
                    endretUtbetalingAndelDeltBostedFullUtbetaling
                )
            )
        }

        Assertions.assertThrows(FunksjonellFeil::class.java) {
            validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer(
                listOf(
                    endretUtbetalingAndelUtvidetFullUtbetaling,
                    endretUtbetalingAndelDeltBostedNullutbetaling
                )
            )
        }
    }

    private fun endretUtbetalingAndel(
        person: Person,
        ytelsestype: YtelseType,
        prosent: BigDecimal,
        fomUtvidet: YearMonth = inneværendeMåned().minusMonths(1),
        tomUtvidet: YearMonth = inneværendeMåned().minusMonths(1),
    ): EndretUtbetalingAndel {
        return lagEndretUtbetalingAndel(
            id = Random.nextLong(),
            fom = fomUtvidet,
            tom = tomUtvidet,
            person = person,
            årsak = Årsak.DELT_BOSTED,
            prosent = prosent,
            andelTilkjentYtelser = mutableListOf(
                lagAndelTilkjentYtelse(
                    fom = fomUtvidet,
                    tom = tomUtvidet,
                    ytelseType = ytelsestype
                )
            )
        )
    }
}
