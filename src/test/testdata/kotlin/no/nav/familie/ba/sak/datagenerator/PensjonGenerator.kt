package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.ekstern.pensjon.FerdigstiltBehandlingForPensjon
import java.time.LocalDate
import java.time.LocalDateTime

fun lagFerdigstiltBehandlingForPensjon(
    sekvensnummer: Long = 1L,
    behandlingId: Long = 123L,
    ident: String = "12345678901",
    vedtakstidspunkt: LocalDateTime = LocalDateTime.of(2026, 10, 8, 12, 0),
    endringstidspunkt: LocalDate = LocalDate.of(2026, 10, 8),
): FerdigstiltBehandlingForPensjon =
    FerdigstiltBehandlingForPensjon(
        sekvensnummer = sekvensnummer,
        behandlingId = behandlingId,
        ident = ident,
        vedtakstidspunkt = vedtakstidspunkt,
        endringstidspunkt = endringstidspunkt,
    )
