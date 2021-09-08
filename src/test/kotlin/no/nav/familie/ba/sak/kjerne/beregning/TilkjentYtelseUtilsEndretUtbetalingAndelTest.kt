package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.overstyring.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.overstyring.domene.Årsak
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
    val beløp = 100

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
                MånedPeriode(YearMonth.of(2018, 10), YearMonth.of(2021, 8)))
                .map {
                    lagAndelTilkjentYtelse(barn1, it.fom, it.tom)
                }

        val andelTilkjentytelseForBarn2 = listOf(
                MånedPeriode(YearMonth.of(2018, 2), YearMonth.of(2018, 8)),
                MånedPeriode(YearMonth.of(2018, 11), YearMonth.of(2021, 9)))
                .map {
                    lagAndelTilkjentYtelse(barn2, it.fom, it.tom)
                }

        val endretUtbetalingerForBarn1 = listOf(
                MånedPeriode(YearMonth.of(2015, 1), YearMonth.of(2018, 3)))
                .map {
                    lagEndretUtbetalingAndel(barn1, it.fom, it.tom, 50)
                }

        val endretUtbetalingerForBarn2 = listOf(
                MånedPeriode(YearMonth.of(2018, 4), YearMonth.of(2018, 5)),
                MånedPeriode(YearMonth.of(2018, 7), YearMonth.of(2021, 9)))
                .map {
                    lagEndretUtbetalingAndel(barn2, it.fom, it.tom, 50)
                }

        val andelerTilkjentYtelserEtterEUA =
                TilkjentYtelseUtils.oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
                        andelTilkjentytelseForBarn1 + andelTilkjentytelseForBarn2,
                        endretUtbetalingerForBarn1 + endretUtbetalingerForBarn2)

        val andelerTilkjentYtelserEtterEUA2 =
                TilkjentYtelseUtils.oppdaterTilkjentYtelseMedEndretUtbetalingAndeler2(
                        andelTilkjentytelseForBarn1 + andelTilkjentytelseForBarn2,
                        endretUtbetalingerForBarn1 + endretUtbetalingerForBarn2)

        assertEquals(andelerTilkjentYtelserEtterEUA, andelerTilkjentYtelserEtterEUA2)
        assertEquals(8, andelerTilkjentYtelserEtterEUA.size)
    }

    @Test
    fun `Ba`() {

        val andelerTilkjentYtelser = listOf(
                AndelTilkjentYtelse(
                        behandlingId = behandling.id,
                        tilkjentYtelse = tilkjentYtelse,
                        personIdent = barn1.personIdent.ident,
                        beløp = beløp,
                        stønadFom = YearMonth.of(2021, 6),
                        stønadTom = YearMonth.of(2021, 8),
                        type = YtelseType.ORDINÆR_BARNETRYGD
                ),
                AndelTilkjentYtelse(
                        behandlingId = behandling.id,
                        tilkjentYtelse = tilkjentYtelse,
                        personIdent = barn1.personIdent.ident,
                        beløp = beløp,
                        stønadFom = YearMonth.of(2021, 9),
                        stønadTom = YearMonth.of(2021, 11),
                        type = YtelseType.ORDINÆR_BARNETRYGD
                )
        )

        val endretUtbetalinger = listOf(
                EndretUtbetalingAndel(
                        behandlingId = behandling.id,
                        person = barn1,
                        prosent = BigDecimal(50),
                        fom = YearMonth.of(2021, 6),
                        tom = YearMonth.of(2021, 7),
                        årsak = Årsak.DELT_BOSTED,
                        begrunnelse = "Halv utbetaling"
                ),
                EndretUtbetalingAndel(
                        behandlingId = behandling.id,
                        person = barn1,
                        prosent = BigDecimal(0),
                        fom = YearMonth.of(2021, 9),
                        tom = YearMonth.of(2021, 10),
                        årsak = Årsak.DELT_BOSTED,
                        begrunnelse = "Halv utbetaling"
                ),
        )

        val andelerTilkjentYtelserEtterEUA =
                TilkjentYtelseUtils.oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(andelerTilkjentYtelser, endretUtbetalinger)

        val andelerTilkjentYtelserEtterEUA2 =
                TilkjentYtelseUtils.oppdaterTilkjentYtelseMedEndretUtbetalingAndeler2(andelerTilkjentYtelser, endretUtbetalinger)

        assertEquals(andelerTilkjentYtelserEtterEUA, andelerTilkjentYtelserEtterEUA2)
        assertEquals(4, andelerTilkjentYtelserEtterEUA.size)
    }

    private fun lagAndelTilkjentYtelse(barn: Person, fom: YearMonth, tom: YearMonth) = AndelTilkjentYtelse(
            behandlingId = behandling.id,
            tilkjentYtelse = tilkjentYtelse,
            personIdent = barn.personIdent.ident,
            beløp = beløp,
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