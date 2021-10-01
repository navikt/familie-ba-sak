package no.nav.familie.ba.sak.ekstern.restDomene

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
    val avtaletidspunkt: YearMonth?,
    val søknadstidspunkt: YearMonth?,
    val begrunnelse: String?,
)