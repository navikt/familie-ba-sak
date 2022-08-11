package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.etterbetalingkorrigering.EtterbetalingKorrigering
import no.nav.familie.ba.sak.kjerne.etterbetalingkorrigering.EtterbetalingKorrigeringÅrsak
import java.time.LocalDateTime

data class RestEtterbetalingKorrigering(
    val id: Long,
    val årsak: EtterbetalingKorrigeringÅrsak,
    val begrunnelse: String?,
    val opprettetTidspunkt: LocalDateTime,
    val beløp: Int,
    val aktiv: Boolean
)

fun EtterbetalingKorrigering.tilRestEtterbetalingKorrigering(): RestEtterbetalingKorrigering =
    RestEtterbetalingKorrigering(
        id = id,
        årsak = årsak,
        begrunnelse = begrunnelse,
        opprettetTidspunkt = opprettetTidspunkt,
        beløp = beløp,
        aktiv = aktiv
    )
