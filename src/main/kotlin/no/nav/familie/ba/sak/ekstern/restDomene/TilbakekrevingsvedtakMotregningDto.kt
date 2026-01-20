package no.nav.familie.ba.sak.ekstern.restDomene

import java.time.LocalDate

data class TilbakekrevingsvedtakMotregningDto(
    val id: Long,
    val behandlingId: Long,
    val årsakTilFeilutbetaling: String?,
    val vurderingAvSkyld: String?,
    val varselDato: LocalDate,
    val samtykke: Boolean,
    val heleBeløpetSkalKrevesTilbake: Boolean,
)

data class OppdaterTilbakekrevingsvedtakMotregningDto(
    val årsakTilFeilutbetaling: String?,
    val vurderingAvSkyld: String?,
    val varselDato: LocalDate?,
    val samtykke: Boolean?,
    val heleBeløpetSkalKrevesTilbake: Boolean?,
)
