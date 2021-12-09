package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene

import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertPerson
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilMinimertPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import java.math.BigDecimal

data class MinimertUtbetalingsperiodeDetalj(
    val person: MinimertPerson,
    val ytelseType: YtelseType,
    val utbetaltPerMnd: Int,
    val erPåvirketAvEndring: Boolean,
    val prosent: BigDecimal,
)

fun UtbetalingsperiodeDetalj.tilMinimertUtbetalingsperiodeDetalj() = MinimertUtbetalingsperiodeDetalj(
    person = this.person.tilMinimertPerson(),
    ytelseType = this.ytelseType,
    utbetaltPerMnd = this.utbetaltPerMnd,
    erPåvirketAvEndring = this.erPåvirketAvEndring,
    prosent = this.prosent
)
