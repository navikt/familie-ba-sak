package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import java.time.LocalDate

data class FortsattInnvilgetPeriode(
        override val periodeFom: LocalDate = inneværendeMåned().førsteDagIInneværendeMåned(),
        override val periodeTom: LocalDate? = null,
        override val vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.FORTSATT_INNVILGET,
        val utbetalingsperiode: Utbetalingsperiode
) : Vedtaksperiode
