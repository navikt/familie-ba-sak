package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.etterbetalingkorrigering.EtterbetalingKorrigering
import no.nav.familie.ba.sak.kjerne.etterbetalingkorrigering.EtterbetalingKorrigeringÅrsak

data class RestEtterbetalingKorrigering(
    val id: Long,
    val årsak: EtterbetalingKorrigeringÅrsak,
    val begrunnelse: String?,
    val beløp: Int,
    val behandlingId: Long,
)

fun EtterbetalingKorrigering.tilRestEtterbetalingKorrigering() =
    RestEtterbetalingKorrigering(
        id = id,
        årsak = årsak,
        begrunnelse = begrunnelse,
        beløp = beløp,
        behandlingId = behandling.id
    )
