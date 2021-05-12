package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FortsattInnvilgetPeriodeTest {

    @Test
    fun `Skal teste at inneværende periode blir valgt i listen over utbetalingsperioder`() {
        val gyldigFom = inneværendeMåned().minusMonths(1).førsteDagIInneværendeMåned()
        val gyldigTom = inneværendeMåned().plusMonths(2).sisteDagIInneværendeMåned()
        val utbetalingsperioder = listOf(
                Utbetalingsperiode(
                        periodeFom = inneværendeMåned().minusMonths(20).førsteDagIInneværendeMåned(),
                        periodeTom = inneværendeMåned().minusMonths(10).sisteDagIInneværendeMåned(),
                        utbetalingsperiodeDetaljer = emptyList(),
                        utbetaltPerMnd = 1054,
                        ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD),
                        antallBarn = 1
                ),
                Utbetalingsperiode(
                        periodeFom = inneværendeMåned().minusMonths(9).førsteDagIInneværendeMåned(),
                        periodeTom = inneværendeMåned().minusMonths(2).sisteDagIInneværendeMåned(),
                        utbetalingsperiodeDetaljer = emptyList(),
                        utbetaltPerMnd = 1054,
                        ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD),
                        antallBarn = 1
                ),
                Utbetalingsperiode(
                        periodeFom = gyldigFom,
                        periodeTom = gyldigTom,
                        utbetalingsperiodeDetaljer = emptyList(),
                        utbetaltPerMnd = 1054,
                        ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD),
                        antallBarn = 1
                ),
                Utbetalingsperiode(
                        periodeFom = inneværendeMåned().plusMonths(3).førsteDagIInneværendeMåned(),
                        periodeTom = inneværendeMåned().plusMonths(20).sisteDagIInneværendeMåned(),
                        utbetalingsperiodeDetaljer = emptyList(),
                        utbetaltPerMnd = 1054,
                        ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD),
                        antallBarn = 1
                )
        )

        assertEquals(gyldigFom, hentInneværendeEllerNesteUtbetalingsperiodeForFortsattInnvilget(utbetalingsperioder).periodeFom)
        assertEquals(gyldigTom, hentInneværendeEllerNesteUtbetalingsperiodeForFortsattInnvilget(utbetalingsperioder).periodeTom)
    }

    @Test
    fun `Skal teste at neste periode blir valgt i listen over utbetalingsperioder`() {
        val gyldigFom = inneværendeMåned().plusMonths(1).førsteDagIInneværendeMåned()
        val gyldigTom = inneværendeMåned().plusMonths(20).sisteDagIInneværendeMåned()
        val utbetalingsperioder = listOf(
                Utbetalingsperiode(
                        periodeFom = gyldigFom,
                        periodeTom = gyldigTom,
                        utbetalingsperiodeDetaljer = emptyList(),
                        utbetaltPerMnd = 1054,
                        ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD),
                        antallBarn = 1
                ),
        )

        assertEquals(gyldigFom, hentInneværendeEllerNesteUtbetalingsperiodeForFortsattInnvilget(utbetalingsperioder).periodeFom)
        assertEquals(gyldigTom, hentInneværendeEllerNesteUtbetalingsperiodeForFortsattInnvilget(utbetalingsperioder).periodeTom)
    }
}