package no.nav.familie.ba.sak.ekstern.pensjon.kafka

import no.nav.familie.ba.sak.ekstern.pensjon.FerdigstiltBehandlingForPensjon
import java.time.LocalDate
import java.time.LocalDateTime

data class FerdigstiltBehandlingForPensjonPayload(
    val ident: String,
    val vedtakstidspunkt: LocalDateTime,
    val endringstidspunkt: LocalDate,
    val sekvensnummer: Long,
) {
    override fun toString(): String = "FerdigstiltBehandlingForPensjonPayload(vedtakstidspunkt=$vedtakstidspunkt, endringstidspunkt=$endringstidspunkt, sekvensnummer=$sekvensnummer)"

    companion object {
        fun fra(ferdigstiltBehandlingForPensjon: FerdigstiltBehandlingForPensjon): FerdigstiltBehandlingForPensjonPayload =
            FerdigstiltBehandlingForPensjonPayload(
                ident = ferdigstiltBehandlingForPensjon.ident,
                vedtakstidspunkt = ferdigstiltBehandlingForPensjon.vedtakstidspunkt,
                endringstidspunkt = ferdigstiltBehandlingForPensjon.endringstidspunkt,
                sekvensnummer = ferdigstiltBehandlingForPensjon.sekvensnummer,
            )
    }
}
