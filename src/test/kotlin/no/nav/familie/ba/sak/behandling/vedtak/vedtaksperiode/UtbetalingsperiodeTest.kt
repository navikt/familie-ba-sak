package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.behandling.restDomene.tilRestPerson
import no.nav.familie.ba.sak.common.lagUtbetalingsperiode
import no.nav.familie.ba.sak.common.lagUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.common.tilfeldigSøker
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UtbetalingsperiodeTest {

    @Test
    fun `Skal gi riktig utbetalingsperiode for vedtaksperiode`() {
        val søker = tilfeldigSøker()
        val gjeldendeFomDato = LocalDate.now().minusMonths(1).withDayOfMonth(1)
        val gjeldendeUtbetalingsperiode = lagUtbetalingsperiode(
                periodeFom = gjeldendeFomDato,
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj(person = søker.tilRestPerson())),
        )
        val utbetalingsperioder = listOf(
                lagUtbetalingsperiode(
                        periodeFom = LocalDate.now().minusMonths(2).withDayOfMonth(1),
                        utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj(person = søker.tilRestPerson())),
                ),
                gjeldendeUtbetalingsperiode,
                lagUtbetalingsperiode(
                        periodeFom = LocalDate.now().withDayOfMonth(1),
                        utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj(person = søker.tilRestPerson())),
                )
        )

        val utbetalingsperiodeForVedtaksperiode =
                hentUtbetalingsperiodeForVedtaksperiode(
                        utbetalingsperioder,
                        gjeldendeFomDato,
                )

        Assertions.assertEquals(gjeldendeUtbetalingsperiode, utbetalingsperiodeForVedtaksperiode)
    }
}