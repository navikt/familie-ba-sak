package no.nav.familie.ba.sak.ekstern.restDomene

import com.fasterxml.jackson.annotation.JsonAutoDetect
import java.time.LocalDate

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class TilbakekrevingsvedtakMotregningDto(
    val id: Long,
    val behandlingId: Long,
    val årsakTilFeilutbetaling: String?,
    val vurderingAvSkyld: String?,
    val varselDato: LocalDate,
    val samtykke: Boolean,
    val heleBeløpetSkalKrevesTilbake: Boolean,
)

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class OppdaterTilbakekrevingsvedtakMotregningDto(
    val årsakTilFeilutbetaling: String?,
    val vurderingAvSkyld: String?,
    val varselDato: LocalDate?,
    val samtykke: Boolean?,
    val heleBeløpetSkalKrevesTilbake: Boolean?,
)
