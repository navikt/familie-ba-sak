package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal class TilkjentYtelseUtilsEndretUtbetalingAndelTest {

    val behandling = lagBehandling()
    val tilkjentYtelse =
        TilkjentYtelse(behandling = behandling, endretDato = LocalDate.now(), opprettetDato = LocalDate.now())
    val beløp = BigDecimal(100)

    val barn1 = tilfeldigPerson(personType = PersonType.BARN)
    val barn2 = tilfeldigPerson(personType = PersonType.BARN)
    val søker = tilfeldigPerson(personType = PersonType.SØKER)

    @BeforeEach
    fun setUp() {
    }

    @Test
    fun `teste nye andeler tilkjent ytelse for to barn med endrete utbetalingsandeler`() {

        val andelTilkjentytelseForBarn1 = listOf(
            MånedPeriode(YearMonth.of(2018, 1), YearMonth.of(2018, 4)),
            MånedPeriode(YearMonth.of(2018, 10), YearMonth.of(2021, 8))
        )
            .map {
                lagAndelTilkjentYtelse(barn1, it.fom, it.tom)
            }

        val andelTilkjentytelseForBarn2 = listOf(
            MånedPeriode(YearMonth.of(2018, 2), YearMonth.of(2018, 8)),
            MånedPeriode(YearMonth.of(2018, 11), YearMonth.of(2021, 9))
        )
            .map {
                lagAndelTilkjentYtelse(barn2, it.fom, it.tom)
            }

        val endretUtbetalingerForBarn1 = listOf(
            MånedPeriode(YearMonth.of(2015, 1), YearMonth.of(2018, 3))
        )
            .map {
                lagEndretUtbetalingAndel(barn1, it.fom, it.tom, 50)
            }

        val endretUtbetalingerForBarn2 = listOf(
            MånedPeriode(YearMonth.of(2018, 4), YearMonth.of(2018, 5)),
            MånedPeriode(YearMonth.of(2018, 7), YearMonth.of(2021, 9)),
            MånedPeriode(YearMonth.of(2021, 11), YearMonth.of(2021, 12))
        )
            .map {
                lagEndretUtbetalingAndel(barn2, it.fom, it.tom, 50)
            }

        val andelerTilkjentYtelserEtterEUA =
            TilkjentYtelseUtils.oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
                (andelTilkjentytelseForBarn1.toMutableSet() + andelTilkjentytelseForBarn2.toMutableSet()).toMutableSet(),
                endretUtbetalingerForBarn1 + endretUtbetalingerForBarn2
            )

        val andelerTilkjentYtelserEtterEUAList = andelerTilkjentYtelserEtterEUA.toList()

        assertEquals(8, andelerTilkjentYtelserEtterEUAList.size)

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList[0],
            barn1.personIdent.ident,
            beløp / BigDecimal(2),
            YearMonth.of(2018, 1),
            YearMonth.of(2018, 3)
        )

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList[1],
            barn1.personIdent.ident,
            beløp,
            YearMonth.of(2018, 4),
            YearMonth.of(2018, 4)
        )

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList[2],
            barn1.personIdent.ident,
            beløp,
            YearMonth.of(2018, 10),
            YearMonth.of(2021, 8)
        )

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList[3],
            barn2.personIdent.ident,
            beløp,
            YearMonth.of(2018, 2),
            YearMonth.of(2018, 3)
        )

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList[4],
            barn2.personIdent.ident,
            beløp / BigDecimal(2),
            YearMonth.of(2018, 4),
            YearMonth.of(2018, 5)
        )

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList[5],
            barn2.personIdent.ident,
            beløp,
            YearMonth.of(2018, 6),
            YearMonth.of(2018, 6)
        )

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList[6],
            barn2.personIdent.ident,
            beløp / BigDecimal(2),
            YearMonth.of(2018, 7),
            YearMonth.of(2018, 8)
        )

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList[7],
            barn2.personIdent.ident,
            beløp / BigDecimal(2),
            YearMonth.of(2018, 11),
            YearMonth.of(2021, 9)
        )
    }

    private fun verifiserAndelTilkjentYtelse(
        andelTilkjentYtelse: AndelTilkjentYtelse,
        forventetBarnIdent: String,
        forventetBeløp: BigDecimal,
        forventetStønadFom: YearMonth,
        forventetStønadTom: YearMonth
    ) {
        assertEquals(forventetBarnIdent, andelTilkjentYtelse.personIdent)
        assertEquals(forventetBeløp, BigDecimal(andelTilkjentYtelse.beløp))
        assertEquals(forventetStønadFom, andelTilkjentYtelse.stønadFom)
        assertEquals(forventetStønadTom, andelTilkjentYtelse.stønadTom)
    }

    private fun lagAndelTilkjentYtelse(barn: Person, fom: YearMonth, tom: YearMonth) = AndelTilkjentYtelse(
        behandlingId = behandling.id,
        tilkjentYtelse = tilkjentYtelse,
        personIdent = barn.personIdent.ident,
        beløp = beløp.toInt(),
        stønadFom = fom,
        stønadTom = tom,
        type = YtelseType.ORDINÆR_BARNETRYGD
    )

    private fun lagEndretUtbetalingAndel(barn: Person, fom: YearMonth, tom: YearMonth, prosent: Int) = EndretUtbetalingAndel(
        behandlingId = behandling.id,
        person = barn,
        prosent = BigDecimal(prosent),
        fom = fom,
        tom = tom,
        årsak = Årsak.DELT_BOSTED,
        begrunnelse = "Begrunnelse for endring"
    )
}