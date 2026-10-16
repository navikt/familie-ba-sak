package no.nav.familie.ba.sak.statistikk.automatiskbehandlingavSoknadStatistikk

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity(name = "AutomatiskBehandlingAvSøknadStatistikk")
@Table(name = "automatisk_behandling_av_soknad_statistikk")
class AutomatiskBehandlingAvSøknadStatistikk(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "automatisk_behandling_av_soknad_statistikk_seq_generator")
    @SequenceGenerator(
        name = "automatisk_behandling_av_soknad_statistikk_seq_generator",
        sequenceName = "automatisk_behandling_av_soknad_statistikk_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @Column(name = "behandling_id")
    val behandlingId: Long? = null,
    @Column(name = "journalpost_id")
    val journalpostId: Long? = null,
    @Column(name = "aarsak_til_manuell_behandling", length = 255)
    val årsakTilManuellBehandling: String? = null,
    @Column(name = "opprettet_tid", updatable = false)
    val opprettetTidspunkt: LocalDateTime? = LocalDateTime.now(),
)
