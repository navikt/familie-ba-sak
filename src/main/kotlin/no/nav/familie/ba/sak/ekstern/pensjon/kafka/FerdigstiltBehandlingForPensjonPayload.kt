package no.nav.familie.ba.sak.ekstern.pensjon.kafka

import no.nav.familie.ba.sak.ekstern.pensjon.FerdigstiltBehandlingForPensjon
import java.time.LocalDate
import java.time.LocalDateTime

data class FerdigstiltBehandlingForPensjonPayload(
    val ident: String,
    val vedtaktidspunkt: LocalDateTime,
    val endringstidspunkt: LocalDate,
    val sekvensnummer: Long,
) {
    companion object {
        fun fra(ferdigstiltBehandlingForPensjon: FerdigstiltBehandlingForPensjon): FerdigstiltBehandlingForPensjonPayload =
            FerdigstiltBehandlingForPensjonPayload(
                ident = ferdigstiltBehandlingForPensjon.ident,
                vedtaktidspunkt = ferdigstiltBehandlingForPensjon.vedtaktidspunkt,
                endringstidspunkt = ferdigstiltBehandlingForPensjon.endringstidspunkt,
                sekvensnummer = ferdigstiltBehandlingForPensjon.sekvensnummer,
            )
    }
}
