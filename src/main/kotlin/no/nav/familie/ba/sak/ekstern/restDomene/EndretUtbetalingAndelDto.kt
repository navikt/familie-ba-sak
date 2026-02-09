package no.nav.familie.ba.sak.ekstern.restDomene

import com.fasterxml.jackson.annotation.JsonAutoDetect
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class EndretUtbetalingAndelDto(
    val id: Long?,
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
