package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.tilPersonEnkel
import no.nav.familie.ba.sak.datagenerator.tilPersonEnkelSøkerOgBarn
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate

class UtbetalingssikkerhetTest {
    @Test
    fun `Skal kaste feil når en periode har flere andeler enn det som er tillatt`() {
        val person = tilfeldigPerson(personType = PersonType.SØKER)

        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            listOf(
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.UTVIDET_BARNETRYGD,
                    1054,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.UTVIDET_BARNETRYGD,
                    1054,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.SMÅBARNSTILLEGG,
                    660,
                    person = person,
                ),
            ),
        )

        val feil =
            assertThrows<IllegalStateException> {
                TilkjentYtelseValidering.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
                    tilkjentYtelse,
                    listOf(person.tilPersonEnkel()),
                )
            }

        assertEquals("Feil med tidslinje. Overlapp på periode", feil.message)
    }

    @Test
    fun `Skal ikke kaste feil når et barn har både finnmarkstillegg andel og ordinær for en periode`() {
        // Arrange
        val person = tilfeldigPerson(personType = PersonType.BARN)

        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            listOf(
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.FINNMARKSTILLEGG,
                    1054,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    660,
                    person = person,
                ),
            ),
        )

        // Act && Assert
        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
                tilkjentYtelse,
                listOf(person.tilPersonEnkel()),
            )
        }
    }

    @Test
    fun `Skal ikke kaste feil når barn har både finnmarkstillegg, utvidet og ordinær andel for en periode i fagsak type barn enslig mindreårig`() {
        // Arrange
        val person = tilfeldigPerson(personType = PersonType.BARN)
        val behandling = lagBehandling(fagsak = lagFagsak(type = FagsakType.BARN_ENSLIG_MINDREÅRIG))

        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling = behandling)

        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            listOf(
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.FINNMARKSTILLEGG,
                    1054,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.UTVIDET_BARNETRYGD,
                    1054,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    660,
                    person = person,
                ),
            ),
        )

        // Act && Assert
        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
                tilkjentYtelse,
                listOf(person.tilPersonEnkel()),
            )
        }
    }

    @Test
    fun `Skal ikke kaste feil når en periode har like mange andeler som er tillatt`() {
        val person = tilfeldigPerson(personType = PersonType.SØKER)

        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            listOf(
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.UTVIDET_BARNETRYGD,
                    1054,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.SMÅBARNSTILLEGG,
                    660,
                    person = person,
                ),
            ),
        )

        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
                tilkjentYtelse,
                listOf(person.tilPersonEnkel()),
            )
        }
    }

    @Test
    fun `Skal kaste feil når en periode har større totalbeløp enn det som er tillatt`() {
        val person = tilfeldigPerson(personType = PersonType.SØKER)

        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            listOf(
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.UTVIDET_BARNETRYGD,
                    SatsService.finnSisteSatsFor(SatsType.UTVIDET_BARNETRYGD).beløp + 1,
                    person = person,
                ),
            ),
        )

        val feil =
            assertThrows<UtbetalingsikkerhetFeil> {
                TilkjentYtelseValidering.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
                    tilkjentYtelse,
                    listOf(person.tilPersonEnkel()),
                )
            }

        assertTrue(feil.message?.contains("Tillatt totalbeløp")!!)
    }

    @Test
    fun `Skal ikke kaste feil når en periode har gyldig totalbeløp`() {
        val person = tilfeldigPerson(personType = PersonType.SØKER)

        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            listOf(
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.UTVIDET_BARNETRYGD,
                    1054,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    inneværendeMåned().minusYears(1),
                    inneværendeMåned().minusMonths(6),
                    YtelseType.SMÅBARNSTILLEGG,
                    660,
                    person = person,
                ),
            ),
        )

        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
                tilkjentYtelse,
                listOf(person.tilPersonEnkel()),
            )
        }
    }

    @Test
    fun `Skal kaste feil når barn får har over 100 prosent gradering for ytelsetype`() {
        val barn1 = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(2).minusMonths(1))
        val barn2 = tilfeldigPerson()
        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            listOf(
                lagAndelTilkjentYtelse(
                    barn1.fødselsdato.nesteMåned(),
                    barn1.fødselsdato.plusYears(18).forrigeMåned(),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    1054,
                    person = barn1,
                    prosent = BigDecimal(100),
                ),
                lagAndelTilkjentYtelse(
                    barn2.fødselsdato.nesteMåned(),
                    barn2.fødselsdato.plusYears(18).forrigeMåned(),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    1054,
                    person = barn2,
                    prosent = BigDecimal(100),
                ),
            ),
        )

        val far = tilfeldigPerson(personType = PersonType.SØKER)
        val personopplysningGrunnlag2 =
            PersonopplysningGrunnlag(
                behandlingId = 1,
                personer = mutableSetOf(far, barn1, barn2),
            )

        val tilkjentYtelse2 = lagInitiellTilkjentYtelse()

        tilkjentYtelse2.andelerTilkjentYtelse.addAll(
            listOf(
                lagAndelTilkjentYtelse(
                    barn1.fødselsdato.nesteMåned(),
                    barn1.fødselsdato.plusYears(18).forrigeMåned(),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    1054,
                    person = barn1,
                    prosent = BigDecimal(100),
                ),
                lagAndelTilkjentYtelse(
                    barn1.fødselsdato.nesteMåned(),
                    barn1.fødselsdato.plusYears(18).forrigeMåned(),
                    YtelseType.SMÅBARNSTILLEGG,
                    660,
                    person = barn1,
                ),
                lagAndelTilkjentYtelse(
                    barn2.fødselsdato.nesteMåned(),
                    barn2.fødselsdato.plusYears(18).forrigeMåned(),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    1054,
                    person = barn2,
                    prosent = BigDecimal(100),
                ),
                lagAndelTilkjentYtelse(
                    barn2.fødselsdato.nesteMåned(),
                    barn2.fødselsdato.plusYears(18).forrigeMåned(),
                    YtelseType.SMÅBARNSTILLEGG,
                    660,
                    person = barn2,
                ),
            ),
        )

        val feil =
            assertThrows<UtbetalingsikkerhetFeil> {
                TilkjentYtelseValidering.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
                    behandlendeBehandlingTilkjentYtelse = tilkjentYtelse2,
                    barnMedAndreRelevanteTilkjentYtelser =
                        listOf(
                            Pair(barn1.tilPersonEnkel(), listOf(tilkjentYtelse)),
                            Pair(barn2.tilPersonEnkel(), listOf(tilkjentYtelse)),
                        ),
                    søkerOgBarn = personopplysningGrunnlag2.tilPersonEnkelSøkerOgBarn(),
                )
            }

        assertTrue(
            feil.frontendFeilmelding?.contains(
                "Du kan ikke godkjenne dette vedtaket fordi det vil betales ut mer enn 100% for barn født ${
                    barn1.fødselsdato.tilKortString()
                } i perioden ${barn1.fødselsdato.nesteMåned()} til ${
                    barn1.fødselsdato.plusYears(18).forrigeMåned()
                } og ${
                    barn2.fødselsdato.tilKortString()
                } i perioden ${barn2.fødselsdato.nesteMåned()} til ${
                    barn2.fødselsdato.plusYears(18).forrigeMåned()
                }",
            )!!,
        )
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
                    prosent = BigDecimal(50),
                ),
            ),
        )

        val far = tilfeldigPerson(personType = PersonType.SØKER)
        val personopplysningGrunnlag2 =
            PersonopplysningGrunnlag(
                behandlingId = 1,
                personer = mutableSetOf(far, barn),
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
                    prosent = BigDecimal(50),
                ),
                lagAndelTilkjentYtelse(
                    barn.fødselsdato.nesteMåned(),
                    barn.fødselsdato.plusYears(18).forrigeMåned(),
                    YtelseType.SMÅBARNSTILLEGG,
                    660,
                    person = barn,
                ),
            ),
        )

        TilkjentYtelseValidering.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
            behandlendeBehandlingTilkjentYtelse = tilkjentYtelse2,
            barnMedAndreRelevanteTilkjentYtelser = listOf(Pair(barn.tilPersonEnkel(), listOf(tilkjentYtelse))),
            søkerOgBarn = personopplysningGrunnlag2.tilPersonEnkelSøkerOgBarn(),
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
                    person = barn,
                ),
            ),
        )

        val far = tilfeldigPerson(personType = PersonType.SØKER)
        val barn2 = tilfeldigPerson()
        val personopplysningGrunnlag2 =
            PersonopplysningGrunnlag(
                behandlingId = 1,
                personer = mutableSetOf(far, barn2),
            )

        val tilkjentYtelse2 = lagInitiellTilkjentYtelse()

        tilkjentYtelse2.andelerTilkjentYtelse.addAll(
            listOf(
                lagAndelTilkjentYtelse(
                    barn2.fødselsdato.nesteMåned(),
                    barn2.fødselsdato.plusYears(18).forrigeMåned(),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    1054,
                    person = barn2,
                ),
            ),
        )

        assertDoesNotThrow {
            TilkjentYtelseValidering.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
                behandlendeBehandlingTilkjentYtelse = tilkjentYtelse2,
                barnMedAndreRelevanteTilkjentYtelser = listOf(Pair(barn.tilPersonEnkel(), listOf(tilkjentYtelse))),
                søkerOgBarn = personopplysningGrunnlag2.tilPersonEnkelSøkerOgBarn(),
            )
        }
    }

    @Test
    fun `Korrekt maksbeløp gis for persontype`() {
        val utvidetBarnetrygd = SatsService.finnSisteSatsFor(SatsType.UTVIDET_BARNETRYGD).beløp
        val småbarnstillegg = SatsService.finnSisteSatsFor(SatsType.SMA).beløp
        val ordinærBarnetrygd = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp
        val finnmarkstillegg = SatsService.finnSisteSatsFor(SatsType.FINNMARKSTILLEGG).beløp

        assertEquals(
            utvidetBarnetrygd + småbarnstillegg,
            TilkjentYtelseValidering.maksBeløp(personType = PersonType.SØKER, fagsakType = FagsakType.NORMAL),
        )
        assertEquals(
            ordinærBarnetrygd + finnmarkstillegg,
            TilkjentYtelseValidering.maksBeløp(personType = PersonType.BARN, fagsakType = FagsakType.NORMAL),
        )
        assertEquals(
            ordinærBarnetrygd + finnmarkstillegg,
            TilkjentYtelseValidering.maksBeløp(personType = PersonType.BARN, fagsakType = FagsakType.INSTITUSJON),
        )
        assertEquals(
            ordinærBarnetrygd + utvidetBarnetrygd + finnmarkstillegg,
            TilkjentYtelseValidering.maksBeløp(
                personType = PersonType.BARN,
                fagsakType = FagsakType.BARN_ENSLIG_MINDREÅRIG,
            ),
        )
        assertThrows<Feil> { TilkjentYtelseValidering.maksBeløp(personType = PersonType.ANNENPART, FagsakType.NORMAL) }
    }

    /**
     * Kontroller og eventuelt oppdater TilkjentYtelseValidering.maksBeløp() dersom nye satstyper legges til
     */
    @Test
    fun `Alle satstyper er tatt hensyn til`() {
        val støttedeSatstyper =
            setOf(
                SatsType.SMA,
                SatsType.TILLEGG_ORBA,
                SatsType.FINN_SVAL,
                SatsType.ORBA,
                SatsType.UTVIDET_BARNETRYGD,
                SatsType.FINNMARKSTILLEGG,
                SatsType.SVALBARDTILLEGG,
            )
        assertTrue(støttedeSatstyper.containsAll(SatsType.entries.toSet()))
        assertEquals(støttedeSatstyper.size, SatsType.entries.size)
    }
}
