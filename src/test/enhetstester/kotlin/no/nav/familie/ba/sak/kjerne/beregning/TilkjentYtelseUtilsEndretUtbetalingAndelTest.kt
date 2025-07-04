package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomBarnFnr
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
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

    val barnAktør1 = randomAktør(randomBarnFnr(10))
    val barnAktør2 = randomAktør(randomBarnFnr(14))

    val barn1 = tilfeldigPerson(personType = PersonType.BARN, aktør = barnAktør1)
    val barn2 = tilfeldigPerson(personType = PersonType.BARN, aktør = barnAktør2)
    val søker = tilfeldigPerson(personType = PersonType.SØKER)

    @BeforeEach
    fun setUp() {
    }

    @Test
    fun `teste nye andeler tilkjent ytelse for to barn med endrete utbetalingsandeler`() {
        /**
         * Tidslinjer barn 1:
         * -------------[############]-----------[#########]---------- AndelTilkjentYtelse
         *            0118        0418         1018      0821
         * ---[################]-------------------------------------- EndretUtbetalingYtelse
         *  0115             0318
         *
         * -------------[######][##]-------------[#########]---------- Nye AndelTilkjentYtelse
         *
         * Periodene for nye AndelTilkjentYtelse: 0118-0318, 0418-0418, 1018-0821
         *
         *
         * Tidslinjer barn 2:
         * --------------[###################]--------[###########]------------ AndelTilkjentYtelse
         *              0218               0818     1118        0921
         * ---------------------[####]----[#######################]---[####]--- EndretUtbetalingYtelse
         *                    0418 0518  0718                   0921 1121-1221
         *
         * --------------[#####][####][##][##]--------[###########]------------ Nye AndelTilkjentYtelse
         *
         * Periodene for nye AndelTilkjentYtelse: 0218-0318, 0418-0518, 0618-0618, 0718-0818, 1118-0921
         */

        val andelTilkjentytelseForBarn1 =
            listOf(
                MånedPeriode(YearMonth.of(2018, 1), YearMonth.of(2018, 4)),
                MånedPeriode(YearMonth.of(2018, 10), YearMonth.of(2021, 8)),
            ).map {
                lagAndelTilkjentYtelse(barn1, it.fom, it.tom)
            }

        val andelTilkjentytelseForBarn2 =
            listOf(
                MånedPeriode(YearMonth.of(2018, 2), YearMonth.of(2018, 8)),
                MånedPeriode(YearMonth.of(2018, 11), YearMonth.of(2021, 9)),
            ).map {
                lagAndelTilkjentYtelse(barn2, it.fom, it.tom)
            }

        val endretUtbetalingerForBarn1 =
            listOf(
                MånedPeriode(YearMonth.of(2015, 1), YearMonth.of(2018, 3)),
                MånedPeriode(YearMonth.of(2018, 4), YearMonth.of(2018, 4)),
            ).map {
                lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                    behandlingId = behandling.id,
                    personer = setOf(barn1),
                    fom = it.fom,
                    tom = it.tom,
                    prosent = 50,
                )
            }

        val endretUtbetalingerForBarn2 =
            listOf(
                MånedPeriode(YearMonth.of(2018, 4), YearMonth.of(2018, 5)),
                MånedPeriode(YearMonth.of(2018, 7), YearMonth.of(2021, 9)),
                MånedPeriode(YearMonth.of(2021, 11), YearMonth.of(2021, 12)),
            ).map {
                lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                    behandlingId = behandling.id,
                    personer = setOf(barn2),
                    fom = it.fom,
                    tom = it.tom,
                    prosent = 50,
                )
            }

        val andelerTilkjentYtelserEtterEUA =
            AndelTilkjentYtelseMedEndretUtbetalingGenerator.lagAndelerMedEndretUtbetalingAndeler(
                andelTilkjentYtelserUtenEndringer = (andelTilkjentytelseForBarn1 + andelTilkjentytelseForBarn2),
                endretUtbetalingAndeler = endretUtbetalingerForBarn1 + endretUtbetalingerForBarn2,
                tilkjentYtelse = tilkjentYtelse,
            )

        val andelerTilkjentYtelserEtterEUAList = andelerTilkjentYtelserEtterEUA.map { it.andel }.toList()

        assertEquals(8, andelerTilkjentYtelserEtterEUAList.size)

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList.filter { it.aktør == barnAktør1 }[0],
            barn1.aktør.aktivFødselsnummer(),
            beløp / BigDecimal(2),
            YearMonth.of(2018, 1),
            YearMonth.of(2018, 3),
        )

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList.filter { it.aktør == barnAktør1 }[1],
            barn1.aktør.aktivFødselsnummer(),
            beløp / BigDecimal(2),
            YearMonth.of(2018, 4),
            YearMonth.of(2018, 4),
        )

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList.filter { it.aktør == barnAktør1 }[2],
            barn1.aktør.aktivFødselsnummer(),
            beløp,
            YearMonth.of(2018, 10),
            YearMonth.of(2021, 8),
        )

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList.filter { it.aktør == barnAktør2 }[0],
            barn2.aktør.aktivFødselsnummer(),
            beløp,
            YearMonth.of(2018, 2),
            YearMonth.of(2018, 3),
        )

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList.filter { it.aktør == barnAktør2 }[1],
            barn2.aktør.aktivFødselsnummer(),
            beløp / BigDecimal(2),
            YearMonth.of(2018, 4),
            YearMonth.of(2018, 5),
        )

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList.filter { it.aktør == barnAktør2 }[2],
            barn2.aktør.aktivFødselsnummer(),
            beløp,
            YearMonth.of(2018, 6),
            YearMonth.of(2018, 6),
        )

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList.filter { it.aktør == barnAktør2 }[3],
            barn2.aktør.aktivFødselsnummer(),
            beløp / BigDecimal(2),
            YearMonth.of(2018, 7),
            YearMonth.of(2018, 8),
        )

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList.filter { it.aktør == barnAktør2 }[4],
            barn2.aktør.aktivFødselsnummer(),
            beløp / BigDecimal(2),
            YearMonth.of(2018, 11),
            YearMonth.of(2021, 9),
        )
    }

    private fun verifiserAndelTilkjentYtelse(
        andelTilkjentYtelse: AndelTilkjentYtelse,
        forventetBarnIdent: String,
        forventetBeløp: BigDecimal,
        forventetStønadFom: YearMonth,
        forventetStønadTom: YearMonth,
    ) {
        assertEquals(forventetBarnIdent, andelTilkjentYtelse.aktør.aktivFødselsnummer())
        assertEquals(forventetBeløp, BigDecimal(andelTilkjentYtelse.kalkulertUtbetalingsbeløp))
        assertEquals(forventetStønadFom, andelTilkjentYtelse.stønadFom)
        assertEquals(forventetStønadTom, andelTilkjentYtelse.stønadTom)
    }

    private fun lagAndelTilkjentYtelse(
        barn: Person,
        fom: YearMonth,
        tom: YearMonth,
    ) = AndelTilkjentYtelse(
        behandlingId = behandling.id,
        tilkjentYtelse = tilkjentYtelse,
        aktør = barn.aktør,
        kalkulertUtbetalingsbeløp = beløp.toInt(),
        nasjonaltPeriodebeløp = beløp.toInt(),
        stønadFom = fom,
        stønadTom = tom,
        type = YtelseType.ORDINÆR_BARNETRYGD,
        sats = beløp.toInt(),
        prosent = BigDecimal(100),
    )
}
