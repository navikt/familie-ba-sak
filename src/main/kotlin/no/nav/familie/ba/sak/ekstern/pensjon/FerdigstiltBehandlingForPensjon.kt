package no.nav.familie.ba.sak.ekstern.pensjon

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "ferdigstilt_behandling_for_pensjon")
class FerdigstiltBehandlingForPensjon(
    @Id
    @Column(name = "sekvensnummer", nullable = false, updatable = false)
    val sekvensnummer: Long,
    @Column(name = "behandling_id", nullable = false, unique = true, updatable = false)
    val behandlingId: Long,
    @Column(name = "ident", nullable = false, updatable = false, length = 11)
    val ident: String,
    @Column(name = "vedtaktidspunkt", nullable = false, updatable = false)
    val vedtaktidspunkt: LocalDateTime,
    @Column(name = "endringstidspunkt", nullable = false, updatable = false)
    val endringstidspunkt: LocalDate,
)
