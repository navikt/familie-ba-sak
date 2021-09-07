package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
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

    val barn = tilfeldigPerson(personType = PersonType.BARN)
    val søker = tilfeldigPerson(personType = PersonType.SØKER)

    @BeforeEach
    fun setUp() {
    }

    @Test
    fun `Ba`() {

        val andelerTilkjentYtelser = listOf(
            AndelTilkjentYtelse(
                behandlingId = behandling.id,
                tilkjentYtelse = tilkjentYtelse,
                personIdent = barn.personIdent.ident,
                beløp = beløp,
                stønadFom = YearMonth.of(2021, 6),
                stønadTom = YearMonth.of(2021, 8),
                type = YtelseType.ORDINÆR_BARNETRYGD
            ),
            AndelTilkjentYtelse(
                behandlingId = behandling.id,
                tilkjentYtelse = tilkjentYtelse,
                personIdent = barn.personIdent.ident,
                beløp = beløp,
                stønadFom = YearMonth.of(2021, 9),
                stønadTom = YearMonth.of(2021, 11),
                type = YtelseType.ORDINÆR_BARNETRYGD
            )
        )

        val endretUtbetalinger = listOf(
            EndretUtbetalingAndel(
                behandlingId = behandling.id,
                person = barn,
                prosent = BigDecimal(50),
                fom = YearMonth.of(2021, 6),
                tom = YearMonth.of(2021, 7),
                årsak = Årsak.DELT_BOSTED,
                begrunnelse = "Halv utbetaling"
            ),
            EndretUtbetalingAndel(
                behandlingId = behandling.id,
                person = barn,
                prosent = BigDecimal(0),
                fom = YearMonth.of(2021, 9),
                tom = YearMonth.of(2021, 10),
                årsak = Årsak.DELT_BOSTED,
                begrunnelse = "Halv utbetaling"
            ),
        )

        val andelerTilkjentYtelserEtterEUA =
            TilkjentYtelseUtils.oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(andelerTilkjentYtelser, endretUtbetalinger)

        assertEquals(4, andelerTilkjentYtelserEtterEUA.size)
    }
}