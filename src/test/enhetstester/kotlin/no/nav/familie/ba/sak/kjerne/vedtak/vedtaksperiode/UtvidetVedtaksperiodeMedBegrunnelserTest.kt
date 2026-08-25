package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilUtvidetVedtaksperiodeMedBegrunnelser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UtvidetVedtaksperiodeMedBegrunnelserTest {
    val barn1 = lagAktør()
    val barn2 = lagAktør()
    val barn3 = lagAktør()
    val søker = lagAktør()

    @Test
    fun `Skal kun legge på utbetalingsdetaljer som gjelder riktig andeler tilkjent ytelse for fortsatt innvilget`() {
        // Arrange
        val behandling = lagBehandling()

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                barnasIdenter = listOf(barn1.aktivFødselsnummer(), barn2.aktivFødselsnummer()),
                søkerPersonIdent = søker.aktivFødselsnummer(),
                søkerAktør = søker,
                barnAktør = listOf(barn1, barn2),
            )

        val fom = YearMonth.of(2018, 6)
        val tom = YearMonth.of(2018, 8)

        val endretUtbetalingAndel =
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                fom = fom,
                tom = tom,
                aktører = setOf(barn2),
            )

        val andelerTilkjentYtelse =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    behandling = behandling,
                    endretUtbetalingAndeler = emptyList(),
                    fom = fom.minusMonths(2),
                    tom = tom,
                    aktør = barn1,
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    behandling = behandling,
                    endretUtbetalingAndeler = listOf(endretUtbetalingAndel),
                    fom = fom,
                    tom = tom,
                    aktør = barn2,
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    behandling = behandling,
                    endretUtbetalingAndeler = emptyList(),
                    fom = tom.plusMonths(1),
                    tom = tom.plusMonths(3),
                    aktør = barn1,
                ),
            )

        val vedtaksperiodeMedBegrunnelser =
            lagVedtaksperiodeMedBegrunnelser(
                fom = null,
                tom = null,
                type = Vedtaksperiodetype.FORTSATT_INNVILGET,
            )

        // Act
        val utvidetVedtaksperiodeMedBegrunnelser =
            vedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
                personopplysningGrunnlag = personopplysningGrunnlag,
                andelerTilkjentYtelse = andelerTilkjentYtelse,
            )

        // Assert
        Assertions.assertEquals(1, utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.size)
        Assertions.assertEquals(
            barn1.aktivFødselsnummer(),
            utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer
                .single()
                .person.personIdent,
        )
        Assertions.assertFalse(utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.single().erPåvirketAvEndring)
    }
}
