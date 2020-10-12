package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.tilfeldigPerson
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate.now

class Utbetalingssikkerhet {

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

        assertThrows<Feil> {
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

        val feil = assertThrows<Feil> {
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
                                       YtelseType.UTVIDET_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse(now().minusYears(1).toString(),
                                       "2020-01-01",
                                       YtelseType.UTVIDET_BARNETRYGD,
                                       1054,
                                       person = person)
        ))

        val feil = assertThrows<Feil> {
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
                lagAndelTilkjentYtelse(now().minusYears(20).toString(),
                                       "2020-01-01",
                                       YtelseType.UTVIDET_BARNETRYGD,
                                       1054,
                                       person = barn)
        ))

        val feil = assertThrows<Feil> {
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
                lagAndelTilkjentYtelse(now().minusYears(20).toString(),
                                       "2020-01-01",
                                       YtelseType.UTVIDET_BARNETRYGD,
                                       1054,
                                       person = søker)
        ))

        val feil = assertThrows<Feil> {
            TilkjentYtelseValidering.validerAtTilkjentYtelseKunHarGyldigTotalPeriode(tilkjentYtelse,
                                                                                     personopplysningGrunnlag)
        }

        assertTrue(feil.frontendFeilmelding?.contains("Søker har utbetalinger")!!)
    }
}