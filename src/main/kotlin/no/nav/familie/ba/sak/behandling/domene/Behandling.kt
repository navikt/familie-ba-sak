package no.nav.familie.ba.sak.behandling.domene

import no.nav.familie.ba.sak.common.BaseEntitet
import javax.persistence.*

@Entity(name = "Behandling")
@Table(name = "BEHANDLING")
data class Behandling(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_seq")
        @SequenceGenerator(name = "behandling_seq")
        val id: Long? = null,

        @ManyToOne(optional = false) @JoinColumn(name = "fk_fagsak_id", nullable = false, updatable = false)
        var fagsak: Fagsak,

        @Column(name = "journalpost_id")
        var journalpostID: String?,

        @Enumerated(EnumType.STRING)
        @Column(name = "behandling_type", nullable = false)
        var type: BehandlingType,

        /**
         * saksnummer fra GSAK.
         */
        @Column(name = "saksnummer")
        var saksnummer: String? = null,

        // TODO legg til status (ta det fra vedtaket).

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true
) : BaseEntitet()

enum class BehandlingType {
    FØRSTEGANGSBEHANDLING,
    REVURDERING,
    KLAGE,
}