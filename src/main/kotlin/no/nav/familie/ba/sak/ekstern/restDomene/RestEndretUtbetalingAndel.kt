package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class RestEndretUtbetalingAndel(
    val id: Long?,
    val personIdent: String?,
    val personIdenter: List<String>?,
    val prosent: BigDecimal?,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val årsak: Årsak?,
    val avtaletidspunktDeltBosted: LocalDate?,
    val søknadstidspunkt: LocalDate?,
    val begrunnelse: String?,
    val erTilknyttetAndeler: Boolean?,
)
