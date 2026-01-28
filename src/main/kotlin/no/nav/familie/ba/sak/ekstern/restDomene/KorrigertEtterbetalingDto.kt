package no.nav.familie.ba.sak.ekstern.restDomene

import com.fasterxml.jackson.annotation.JsonAutoDetect
import no.nav.familie.ba.sak.kjerne.korrigertetterbetaling.KorrigertEtterbetaling
import no.nav.familie.ba.sak.kjerne.korrigertetterbetaling.KorrigertEtterbetalingÅrsak
import java.time.LocalDateTime

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class KorrigertEtterbetalingDto(
    val id: Long,
    val årsak: KorrigertEtterbetalingÅrsak,
    val begrunnelse: String?,
    val opprettetTidspunkt: LocalDateTime,
    val beløp: Int,
    val aktiv: Boolean,
)

fun KorrigertEtterbetaling.tilKorrigertEtterbetalingDto(): KorrigertEtterbetalingDto =
    KorrigertEtterbetalingDto(
        id = id,
        årsak = årsak,
        begrunnelse = begrunnelse,
        opprettetTidspunkt = opprettetTidspunkt,
        beløp = beløp,
        aktiv = aktiv,
    )
