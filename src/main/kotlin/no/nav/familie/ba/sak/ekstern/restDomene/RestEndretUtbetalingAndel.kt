package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import java.math.BigDecimal
import java.time.YearMonth

class RestEndretUtbetalingAndel(
    val personIdent: String?,
    val prosent: BigDecimal?,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val årsak: Årsak?,
    val begrunnelse: String?,
)

fun EndretUtbetalingAndel.tilRestEndretUtbetalingAndel() = RestEndretUtbetalingAndel(
        personIdent = this.person?.personIdent?.ident,
        prosent = this.prosent,
        fom = this.fom,
        tom = this.tom,
        årsak = this.årsak,
        begrunnelse = this.begrunnelse
)