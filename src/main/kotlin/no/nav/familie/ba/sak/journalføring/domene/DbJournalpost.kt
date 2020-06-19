package no.nav.familie.ba.sak.journalføring.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.behandling.domene.Behandling
import javax.persistence.*

@Entity(name = "Journalpost")
@Table(name = "JOURNALPOST")
data class DbJournalpost(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "journalpost_seq_generator")
        @SequenceGenerator(name = "journalpost_seq_generator", sequenceName = "journalpost_seq", allocationSize = 50)
        val id: Long = 0,

        @JsonIgnore
        @ManyToOne @JoinColumn(name = "fk_behandling_id", nullable = false)
        val behandling: Behandling,

        @Column(name = "journalpost_id")
        val journalpostId: String
)