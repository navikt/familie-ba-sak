package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVedtaksbegrunnelse
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPerson
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth

class UtvidetVedtaksperiodeMedBegrunnelserTest {

    val barn1 = tilfeldigPerson(personType = PersonType.BARN)
    val barn2 = tilfeldigPerson(personType = PersonType.BARN)
    val søker = tilfeldigSøker()

    @Test
    fun `Skal kun legge på utbetalingsdetaljer som gjelder riktig andeler tilkjent ytelse for utbetaling`() {
        val behandling = lagBehandling()

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            barnasIdenter = listOf(barn1.personIdent.ident, barn2.personIdent.ident),
            søkerPersonIdent = søker.personIdent.ident
        )

        val fom = YearMonth.of(2018, 6)
        val tom = YearMonth.of(2018, 8)

        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            behandlingId = behandling.id,
            fom = fom,
            tom = tom,
            person = barn2
        )

        val andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                behandling = behandling,
                endretUtbetalingAndeler = emptyList(),
                fom = fom.toString(),
                tom = tom.toString(),
                person = barn1
            ),
            lagAndelTilkjentYtelse(
                behandling = behandling,
                endretUtbetalingAndeler = listOf(endretUtbetalingAndel),
                fom = fom.toString(),
                tom = tom.toString(),
                person = barn2
            ),
            lagAndelTilkjentYtelse(
                behandling = behandling,
                endretUtbetalingAndeler = emptyList(),
                fom = tom.plusMonths(1).toString(),
                tom = tom.plusMonths(3).toString(),
                person = barn1
            )
        )

        val vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(
            fom = fom.førsteDagIInneværendeMåned(),
            tom = tom.sisteDagIInneværendeMåned(),
            type = Vedtaksperiodetype.UTBETALING
        )

        val utvidetVedtaksperiodeMedBegrunnelser =
            vedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
                personopplysningGrunnlag,
                andelerTilkjentYtelse
            )

        Assertions.assertEquals(1, utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.size)
        Assertions.assertEquals(
            barn1.tilRestPerson().personIdent,
            utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.single().person.personIdent
        )
        Assertions.assertFalse(utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.single().erPåvirketAvEndring)
    }

    @Test
    fun `Skal kun legge på utbetalingsdetaljer som gjelder riktig andeler tilkjent ytelse for endret utbetaling`() {
        val behandling = lagBehandling()

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            barnasIdenter = listOf(barn1.personIdent.ident, barn2.personIdent.ident),
            søkerPersonIdent = søker.personIdent.ident
        )

        val fom = YearMonth.of(2018, 6)
        val tom = YearMonth.of(2018, 8)

        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            behandlingId = behandling.id,
            fom = fom,
            tom = tom,
            person = barn2
        )

        val andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                behandling = behandling,
                endretUtbetalingAndeler = emptyList(),
                fom = fom.toString(),
                tom = tom.toString(),
                person = barn1
            ),
            lagAndelTilkjentYtelse(
                behandling = behandling,
                endretUtbetalingAndeler = listOf(endretUtbetalingAndel),
                fom = fom.toString(),
                tom = tom.toString(),
                person = barn2
            ),
            lagAndelTilkjentYtelse(
                behandling = behandling,
                endretUtbetalingAndeler = emptyList(),
                fom = tom.plusMonths(1).toString(),
                tom = tom.plusMonths(3).toString(),
                person = barn1
            )
        )

        val vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(
            fom = fom.førsteDagIInneværendeMåned(),
            tom = tom.sisteDagIInneværendeMåned(),
            type = Vedtaksperiodetype.ENDRET_UTBETALING,
            begrunnelser = mutableSetOf(lagVedtaksbegrunnelse(personIdenter = listOf(barn2.personIdent.ident)))
        )

        val utvidetVedtaksperiodeMedBegrunnelser =
            vedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
                personopplysningGrunnlag,
                andelerTilkjentYtelse
            )

        Assertions.assertEquals(1, utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.size)
        Assertions.assertEquals(
            barn2.tilRestPerson().personIdent,
            utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.single().person.personIdent
        )
        Assertions.assertTrue(utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.single().erPåvirketAvEndring)
    }
}
