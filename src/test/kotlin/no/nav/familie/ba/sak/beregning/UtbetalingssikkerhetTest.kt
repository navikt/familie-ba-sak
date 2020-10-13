package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate.now

class UtbetalingssikkerhetTest {

    @Test
    fun `Skal kaste feil når tilkjent ytelse går mer enn 3 år og 2 mnd tilbake i tid`() {
        val person = tilfeldigPerson()

        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        val andel = lagAndelTilkjentYtelse(now().minusYears(4).toString(),
                                           "2020-01-01",
                                           YtelseType.ORDINÆR_BARNETRYGD,
                                           1054,
                                           person = person)

        tilkjentYtelse.andelerTilkjentYtelse.add(andel)

        assertThrows<UtbetalingsikkerhetFeil> {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarGyldigEtterbetalingsperiode(tilkjentYtelse)
        }
    }

    @Test
    fun `Skal ikke kaste feil når tilkjent ytelse går mindre enn 3 år og 2 mnd tilbake i tid`() {
        val person = tilfeldigPerson()

        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        val andel = lagAndelTilkjentYtelse(now().minusYears(3).toString(),
                                           "2020-01-01",
                                           YtelseType.ORDINÆR_BARNETRYGD,
                                           1054,
                                           person = person)

        tilkjentYtelse.andelerTilkjentYtelse.add(andel)

        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarGyldigEtterbetalingsperiode(tilkjentYtelse)
        }
    }

    @Test
    fun `Skal kaste feil når en periode har flere andeler enn det som er tillatt`() {
        val person = tilfeldigPerson(personType = PersonType.SØKER)
        val personopplysningGrunnlag = PersonopplysningGrunnlag(
                behandlingId = 1,
                personer = mutableSetOf(person)
        )

        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(listOf(
                lagAndelTilkjentYtelse(now().minusYears(1).toString(),
                                       "2020-01-01",
                                       YtelseType.UTVIDET_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse(now().minusYears(1).toString(),
                                       "2020-01-01",
                                       YtelseType.UTVIDET_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse(now().minusYears(1).toString(),
                                       "2020-01-01",
                                       YtelseType.SMÅBARNSTILLEGG,
                                       660,
                                       person = person)
        ))

        val feil = assertThrows<UtbetalingsikkerhetFeil> {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(tilkjentYtelse,
                                                                                         personopplysningGrunnlag)
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

        tilkjentYtelse.andelerTilkjentYtelse.addAll(listOf(
                lagAndelTilkjentYtelse(now().minusYears(1).toString(),
                                       "2020-01-01",
                                       YtelseType.UTVIDET_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse(now().minusYears(1).toString(),
                                       "2020-01-01",
                                       YtelseType.SMÅBARNSTILLEGG,
                                       660,
                                       person = person),
        ))

        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(tilkjentYtelse,
                                                                                         personopplysningGrunnlag)
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

        tilkjentYtelse.andelerTilkjentYtelse.addAll(listOf(
                lagAndelTilkjentYtelse(now().minusYears(1).toString(),
                                       "2020-01-01",
                                       YtelseType.ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse(now().minusYears(1).toString(),
                                       "2020-01-01",
                                       YtelseType.UTVIDET_BARNETRYGD,
                                       1500,
                                       person = person),
        ))

        val feil = assertThrows<UtbetalingsikkerhetFeil> {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(tilkjentYtelse,
                                                                                         personopplysningGrunnlag)
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

        tilkjentYtelse.andelerTilkjentYtelse.addAll(listOf(
                lagAndelTilkjentYtelse(now().minusYears(1).toString(),
                                       "2020-01-01",
                                       YtelseType.UTVIDET_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse(now().minusYears(1).toString(),
                                       "2020-01-01",
                                       YtelseType.SMÅBARNSTILLEGG,
                                       660,
                                       person = person),
        ))

        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(tilkjentYtelse,
                                                                                         personopplysningGrunnlag)
        }
    }

    @Test
    fun `Skal kaste feil når et barn har perioder utover 0-18 år`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn = tilfeldigPerson()
        val personopplysningGrunnlag = PersonopplysningGrunnlag(
                behandlingId = 1,
                personer = mutableSetOf(søker, barn)
        )

        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(listOf(
                lagAndelTilkjentYtelse(barn.fødselsdato.minusYears(1).toString(),
                                       barn.fødselsdato.plusYears(10).toString(),
                                       YtelseType.UTVIDET_BARNETRYGD,
                                       1054,
                                       person = barn)
        ))

        val feil = assertThrows<UtbetalingsikkerhetFeil> {
            TilkjentYtelseValidering.validerAtTilkjentYtelseKunHarGyldigTotalPeriode(tilkjentYtelse,
                                                                                     personopplysningGrunnlag)
        }

        assertTrue(feil.frontendFeilmelding?.contains("${barn.personIdent.ident} har utbetalinger utover 0-18 år")!!)
    }

    @Test
    fun `Skal kaste feil når søker har perioder utover 0-18 år`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn = tilfeldigPerson()
        val personopplysningGrunnlag = PersonopplysningGrunnlag(
                behandlingId = 1,
                personer = mutableSetOf(søker, barn)
        )

        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(listOf(
                lagAndelTilkjentYtelse(barn.fødselsdato.minusYears(1).toString(),
                                       barn.fødselsdato.plusYears(10).toString(),
                                       YtelseType.UTVIDET_BARNETRYGD,
                                       1054,
                                       person = søker)
        ))

        val feil = assertThrows<UtbetalingsikkerhetFeil> {
            TilkjentYtelseValidering.validerAtTilkjentYtelseKunHarGyldigTotalPeriode(tilkjentYtelse,
                                                                                     personopplysningGrunnlag)
        }

        assertTrue(feil.frontendFeilmelding?.contains("Søker har utbetalinger")!!)
    }

    @Test
    fun `Skal ikke kaste feil når barn har riktig 0-18 år periode`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn = tilfeldigPerson()
        val personopplysningGrunnlag = PersonopplysningGrunnlag(
                behandlingId = 1,
                personer = mutableSetOf(søker, barn)
        )

        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(listOf(
                lagAndelTilkjentYtelse(barn.fødselsdato.førsteDagINesteMåned().toString(),
                                       barn.fødselsdato.plusYears(18).sisteDagIForrigeMåned().toString(),
                                       YtelseType.UTVIDET_BARNETRYGD,
                                       1054,
                                       person = barn)
        ))

        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtTilkjentYtelseKunHarGyldigTotalPeriode(tilkjentYtelse,
                                                                                     personopplysningGrunnlag)
        }
    }

    @Test
    fun `Skal kaste feil når barn får har over 100% gradering for ytelsetype`() {
        val barn = tilfeldigPerson()
        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(listOf(
                lagAndelTilkjentYtelse(barn.fødselsdato.førsteDagINesteMåned().toString(),
                                       barn.fødselsdato.plusYears(18).sisteDagIForrigeMåned().toString(),
                                       YtelseType.ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = barn)
        ))

        val far = tilfeldigPerson(personType = PersonType.SØKER)
        val personopplysningGrunnlag2 = PersonopplysningGrunnlag(
                behandlingId = 1,
                personer = mutableSetOf(far, barn)
        )

        val tilkjentYtelse2 = lagInitiellTilkjentYtelse()

        tilkjentYtelse2.andelerTilkjentYtelse.addAll(listOf(
                lagAndelTilkjentYtelse(barn.fødselsdato.førsteDagINesteMåned().toString(),
                                       barn.fødselsdato.plusYears(18).sisteDagIForrigeMåned().toString(),
                                       YtelseType.ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = barn),
                lagAndelTilkjentYtelse(barn.fødselsdato.førsteDagINesteMåned().toString(),
                                       barn.fødselsdato.plusYears(18).sisteDagIForrigeMåned().toString(),
                                       YtelseType.SMÅBARNSTILLEGG,
                                       660,
                                       person = barn)
        ))

        val feil = assertThrows<UtbetalingsikkerhetFeil> {
            TilkjentYtelseValidering.valider100ProsentGraderingForBarna(tilkjentYtelse2,
                                                                        listOf(Pair(barn, listOf(tilkjentYtelse))),
                                                                        personopplysningGrunnlag2)
        }

        assertTrue(feil.frontendFeilmelding?.contains("Det utbetales allerede barnetrygd (${YtelseType.ORDINÆR_BARNETRYGD}) for ${barn.personIdent}")!!)
    }

    @Test
    fun `Skal ikke kaste feil når man ser sender inn ulike barn`() {
        val barn = tilfeldigPerson()
        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(listOf(
                lagAndelTilkjentYtelse(barn.fødselsdato.førsteDagINesteMåned().toString(),
                                       barn.fødselsdato.plusYears(18).sisteDagIForrigeMåned().toString(),
                                       YtelseType.ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = barn)
        ))

        val far = tilfeldigPerson(personType = PersonType.SØKER)
        val barn2 = tilfeldigPerson()
        val personopplysningGrunnlag2 = PersonopplysningGrunnlag(
                behandlingId = 1,
                personer = mutableSetOf(far, barn2)
        )

        val tilkjentYtelse2 = lagInitiellTilkjentYtelse()

        tilkjentYtelse2.andelerTilkjentYtelse.addAll(listOf(
                lagAndelTilkjentYtelse(barn2.fødselsdato.førsteDagINesteMåned().toString(),
                                       barn2.fødselsdato.plusYears(18).sisteDagIForrigeMåned().toString(),
                                       YtelseType.ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = barn2)
        ))

        assertDoesNotThrow {
            TilkjentYtelseValidering.valider100ProsentGraderingForBarna(tilkjentYtelse2,
                                                                        listOf(Pair(barn, listOf(tilkjentYtelse))),
                                                                        personopplysningGrunnlag2)
        }
    }
}