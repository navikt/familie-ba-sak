package no.nav.familie.ba.sak.ekstern.restDomene

import java.time.LocalDate

data class RestTilbakekrevingsvedtakMotregning(
    val id: Long,
    val behandlingId: Long,
    val årsakTilFeilutbetaling: String?,
    val vurderingAvSkyld: String?,
    val varselDato: LocalDate?,
    val samtykke: Boolean,
)

data class RestOppdaterTilbakekrevingsvedtakMotregning(
    val årsakTilFeilutbetaling: String?,
    val vurderingAvSkyld: String?,
    val varselDato: LocalDate?,
    val samtykke: Boolean?,
)
