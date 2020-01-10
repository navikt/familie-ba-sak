package no.nav.familie.ba.sak.behandling.domene.vedtak

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.common.BaseEntitet
import java.time.LocalDate
import javax.persistence.*

@Entity(name = "BehandlingVedtak")
@Table(name = "BEHANDLING_VEDTAK")
data class BehandlingVedtak(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_vedtak_seq")
        @SequenceGenerator(name = "behandling_vedtak_seq")
        val id: Long? = null,

        @ManyToOne(optional = false) @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
        val behandling: Behandling,

        @Column(name = "ansvarlig_saksbehandler", nullable = false)
        val ansvarligSaksbehandler: String,

        @Column(name = "vedtaksdato", nullable = false)
        val vedtaksdato: LocalDate,

        @Column(name = "stonad_fom", nullable = false)
        var stønadFom: LocalDate,

        @Column(name = "stonad_tom", nullable = false)
        var stønadTom: LocalDate,

        @Column(name = "stonad_brev_markdown", columnDefinition = "TEXT")
        var stønadBrevMarkdown: String = "",

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false)
        var status: BehandlingVedtakStatus = BehandlingVedtakStatus.OPPRETTET,

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true
) : BaseEntitet()

enum class BehandlingVedtakStatus {
        OPPRETTET, SENDT_TIL_IVERKSETTING, IVERKSATT
}