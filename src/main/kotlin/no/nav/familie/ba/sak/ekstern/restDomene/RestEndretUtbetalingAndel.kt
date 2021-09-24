package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import java.math.BigDecimal
import java.time.YearMonth

class RestEndretUtbetalingAndel(
    val id: Long?,
    val personIdent: String?,
    val prosent: BigDecimal?,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val årsak: Årsak?,
    val begrunnelse: String?,
)

fun EndretUtbetalingAndel.tilRestEndretUtbetalingAndel() = RestEndretUtbetalingAndel(
        id = this.id,
        personIdent = this.person?.personIdent?.ident,
        prosent = this.prosent,
        fom = this.fom,
        tom = this.tom,
        årsak = this.årsak,
        begrunnelse = this.begrunnelse
)