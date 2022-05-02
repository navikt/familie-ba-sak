package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilBrevTekst
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class UtbetalingssikkerhetTest {

    @Test
    fun `Skal kaste feil når en periode har flere andeler enn det som er tillatt`() {
        val person = tilfeldigPerson(personType = PersonType.SØKER)
        val personopplysningGrunnlag = PersonopplysningGrunnlag(
            behandlingId = 1,
            personer = mutableSetOf(person)
        )

        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            listOf(
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.UTVIDET_BARNETRYGD,
                    1054,
                    person = person
                ),
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.UTVIDET_BARNETRYGD,
                    1054,
                    person = person
                ),
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.SMÅBARNSTILLEGG,
                    660,
                    person = person
                )
            )
        )

        val feil = assertThrows<UtbetalingsikkerhetFeil> {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
                tilkjentYtelse,
                personopplysningGrunnlag
            )
        }

        assertTrue(feil.message?.contains("Tillatte andeler")!!)
    }

    @Test
    fun `Skal ikke kaste feil når en periode har like mange andeler som er tillatt`() {
        val person = tilfeldigPerson(personType = PersonType.SØKER)
        val personopplysningGrunnlag = PersonopplysningGrunnlag(
            behandlingId = 1,
            personer = mutableSetOf(person)
        )

        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            listOf(
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.UTVIDET_BARNETRYGD,
                    1054,
                    person = person
                ),
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.SMÅBARNSTILLEGG,
                    660,
                    person = person
                ),
            )
        )

        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
                tilkjentYtelse,
                personopplysningGrunnlag
            )
        }
    }

    @Test
    fun `Skal kaste feil når en periode har større totalbeløp enn det som er tillatt`() {
        val person = tilfeldigPerson(personType = PersonType.SØKER)
        val personopplysningGrunnlag = PersonopplysningGrunnlag(
            behandlingId = 1,
            personer = mutableSetOf(person)
        )

        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            listOf(
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    1054,
                    person = person
                ),
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.UTVIDET_BARNETRYGD,
                    1500,
                    person = person
                ),
            )
        )

        val feil = assertThrows<UtbetalingsikkerhetFeil> {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
                tilkjentYtelse,
                personopplysningGrunnlag
            )
        }

        assertTrue(feil.message?.contains("Tillatt totalbeløp")!!)
    }

    @Test
    fun `Skal ikke kaste feil når en periode har gyldig totalbeløp`() {
        val person = tilfeldigPerson(personType = PersonType.SØKER)
        val personopplysningGrunnlag = PersonopplysningGrunnlag(
            behandlingId = 1,
            personer = mutableSetOf(person)
        )

        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            listOf(
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.UTVIDET_BARNETRYGD,
                    1054,
                    person = person
                ),
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.SMÅBARNSTILLEGG,
                    660,
                    person = person
                ),
            )
        )

        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
                tilkjentYtelse,
                personopplysningGrunnlag
            )
        }
    }

    @Test
    fun `Skal kaste feil når barn får har over 100 prosent gradering for ytelsetype`() {
        val barn = tilfeldigPerson()
        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            listOf(
                lagAndelTilkjentYtelse(
                    barn.fødselsdato.nesteMåned(),
                    barn.fødselsdato.plusYears(18).forrigeMåned(),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    1054,
                    person = barn,
                    prosent = BigDecimal(100)
                )
            )
        )

        val far = tilfeldigPerson(personType = PersonType.SØKER)
        val personopplysningGrunnlag2 = PersonopplysningGrunnlag(
            behandlingId = 1,
            personer = mutableSetOf(far, barn)
        )

        val tilkjentYtelse2 = lagInitiellTilkjentYtelse()

        tilkjentYtelse2.andelerTilkjentYtelse.addAll(
            listOf(
                lagAndelTilkjentYtelse(
                    barn.fødselsdato.nesteMåned(),
                    barn.fødselsdato.plusYears(18).forrigeMåned(),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    1054,
                    person = barn,
                    prosent = BigDecimal(100)
                ),
                lagAndelTilkjentYtelse(
                    barn.fødselsdato.nesteMåned(),
                    barn.fødselsdato.plusYears(18).forrigeMåned(),
                    YtelseType.SMÅBARNSTILLEGG,
                    660,
                    person = barn
                )
            )
        )

        val feil = assertThrows<UtbetalingsikkerhetFeil> {
            TilkjentYtelseValidering.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
                behandlendeBehandlingTilkjentYtelse = tilkjentYtelse2,
                barnMedAndreRelevanteTilkjentYtelser = listOf(Pair(barn, listOf(tilkjentYtelse))),
                personopplysningGrunnlag = personopplysningGrunnlag2,
            )
        }

        assertTrue(feil.frontendFeilmelding?.contains("Du kan ikke godkjenne dette vedtaket fordi det vil betales ut mer enn 100% for barn født ${listOf(barn.fødselsdato).tilBrevTekst()}")!!)
    }

    @Test
    fun `Skal ikke kaste feil når utbetalingsandeler for barn ikke overskrider 100 prosent for ytelsetype`() {
        val barn = tilfeldigPerson()
        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            listOf(
                lagAndelTilkjentYtelse(
                    barn.fødselsdato.nesteMåned(),
                    barn.fødselsdato.plusYears(18).forrigeMåned(),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    1054,
                    person = barn,
                    prosent = BigDecimal(50)
                )
            )
        )

        val far = tilfeldigPerson(personType = PersonType.SØKER)
        val personopplysningGrunnlag2 = PersonopplysningGrunnlag(
            behandlingId = 1,
            personer = mutableSetOf(far, barn)
        )

        val tilkjentYtelse2 = lagInitiellTilkjentYtelse()

        tilkjentYtelse2.andelerTilkjentYtelse.addAll(
            listOf(
                lagAndelTilkjentYtelse(
                    barn.fødselsdato.nesteMåned(),
                    barn.fødselsdato.plusYears(18).forrigeMåned(),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    1054,
                    person = barn,
                    prosent = BigDecimal(50)
                ),
                lagAndelTilkjentYtelse(
                    barn.fødselsdato.nesteMåned(),
                    barn.fødselsdato.plusYears(18).forrigeMåned(),
                    YtelseType.SMÅBARNSTILLEGG,
                    660,
                    person = barn
                )
            )
        )

        TilkjentYtelseValidering.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
            behandlendeBehandlingTilkjentYtelse = tilkjentYtelse2,
            barnMedAndreRelevanteTilkjentYtelser = listOf(Pair(barn, listOf(tilkjentYtelse))),
            personopplysningGrunnlag = personopplysningGrunnlag2,
        )
    }

    @Test
    fun `Skal ikke kaste feil når man ser sender inn ulike barn`() {
        val barn = tilfeldigPerson()
        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            listOf(
                lagAndelTilkjentYtelse(
                    barn.fødselsdato.nesteMåned(),
                    barn.fødselsdato.plusYears(18).forrigeMåned(),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    1054,
                    person = barn
                )
            )
        )

        val far = tilfeldigPerson(personType = PersonType.SØKER)
        val barn2 = tilfeldigPerson()
        val personopplysningGrunnlag2 = PersonopplysningGrunnlag(
            behandlingId = 1,
            personer = mutableSetOf(far, barn2)
        )

        val tilkjentYtelse2 = lagInitiellTilkjentYtelse()

        tilkjentYtelse2.andelerTilkjentYtelse.addAll(
            listOf(
                lagAndelTilkjentYtelse(
                    barn2.fødselsdato.nesteMåned(),
                    barn2.fødselsdato.plusYears(18).forrigeMåned(),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    1054,
                    person = barn2
                )
            )
        )

        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
                behandlendeBehandlingTilkjentYtelse = tilkjentYtelse2,
                barnMedAndreRelevanteTilkjentYtelser = listOf(Pair(barn, listOf(tilkjentYtelse))),
                personopplysningGrunnlag = personopplysningGrunnlag2,
            )
        }
    }

    @Test
    fun `Korrekt maksbeløp gis for persontype`() {
        assertEquals(1054 + 660, TilkjentYtelseValidering.maksBeløp(personType = PersonType.SØKER))
        assertEquals(1676, TilkjentYtelseValidering.maksBeløp(personType = PersonType.BARN))
        assertThrows<Feil> { TilkjentYtelseValidering.maksBeløp(personType = PersonType.ANNENPART) }
    }

    /**
     * Kontroller og eventuelt oppdater TilkjentYtelseValidering.maksBeløp() dersom nye satstyper legges til
     */
    @Test
    fun `Alle satstyper er tatt hensyn til`() {
        val støttedeSatstyper = setOf(
            SatsType.SMA,
            SatsType.TILLEGG_ORBA,
            SatsType.FINN_SVAL,
            SatsType.ORBA
        )
        assertTrue(støttedeSatstyper.containsAll(SatsType.values().toSet()))
        assertEquals(støttedeSatstyper.size, SatsType.values().size)
    }
}
