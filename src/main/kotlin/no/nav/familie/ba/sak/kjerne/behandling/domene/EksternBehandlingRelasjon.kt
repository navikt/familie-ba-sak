package no.nav.familie.ba.sak.kjerne.behandling.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity(name = "EksternBehandlingRelasjon")
@Table(name = "EKSTERN_BEHANDLING_RELASJON")
data class EksternBehandlingRelasjon(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ekstern_behandling_relasjon_seq_generator")
    @SequenceGenerator(name = "ekstern_behandling_relasjon_seq_generator", sequenceName = "ekstern_behandling_relasjon_seq", allocationSize = 50)
    val id: Long = 0,
    @Column(name = "fk_behandling_id", nullable = false)
    val internBehandlingId: Long,
    @Column(name = "ekstern_behandling_id", nullable = false)
    val eksternBehandlingId: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "ekstern_behandling_fagsystem", nullable = false)
    val eksternBehandlingFagsystem: Fagsystem,
    @Column(name = "opprettet_tid", nullable = false)
    val opprettetTid: LocalDateTime = LocalDateTime.now(),
) {
    enum class Fagsystem {
        KLAGE,
        TILBAKEKREVING,
    }

    companion object Factory {
        fun opprettFraNyEksternBehandlingRelasjon(
            internBehandlingId: Long,
            nyEksternBehandlingRelasjon: NyEksternBehandlingRelasjon,
        ) = EksternBehandlingRelasjon(
            internBehandlingId = internBehandlingId,
            eksternBehandlingId = nyEksternBehandlingRelasjon.eksternBehandlingId,
            eksternBehandlingFagsystem = nyEksternBehandlingRelasjon.eksternBehandlingFagsystem,
        )
    }
}
