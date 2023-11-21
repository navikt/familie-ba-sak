package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilMinimertPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import java.math.BigDecimal

data class MinimertUtbetalingsperiodeDetalj(
    val person: MinimertRestPerson,
    val ytelseType: YtelseType,
    val utbetaltPerMnd: Int,
    val erPåvirketAvEndring: Boolean,
    val endringsårsak: Årsak?,
    val prosent: BigDecimal,
)

fun UtbetalingsperiodeDetalj.tilMinimertUtbetalingsperiodeDetalj() =
    MinimertUtbetalingsperiodeDetalj(
        person = this.person.tilMinimertPerson(),
        ytelseType = this.ytelseType,
        utbetaltPerMnd = this.utbetaltPerMnd,
        erPåvirketAvEndring = this.erPåvirketAvEndring,
        prosent = this.prosent,
        endringsårsak = this.endringsårsak,
    )
