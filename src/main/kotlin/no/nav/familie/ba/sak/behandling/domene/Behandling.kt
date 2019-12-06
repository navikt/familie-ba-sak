package no.nav.familie.ba.sak.behandling.domene

import no.nav.familie.ba.sak.common.BaseEntitet
import java.util.*
import javax.persistence.*

@Entity(name = "Behandling") @Table(name = "BEHANDLING")
data class Behandling(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_seq")
        @SequenceGenerator(name = "behandling_seq")
        val id: Long? = null,

        @ManyToOne(optional = false) @JoinColumn(name = "fk_fagsak_id", nullable = false, updatable = false)
        var fagsak: Fagsak,

        @Column(name = "journalpost_id", nullable = false)
        var journalpostID: String,

        /**
        * saksnummer fra GSAK.
        */
        @Column(name = "saksnummer")
        var saksnummer: String? = null) : BaseEntitet() {
}