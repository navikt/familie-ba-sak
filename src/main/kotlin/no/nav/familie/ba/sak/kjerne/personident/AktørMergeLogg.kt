package no.nav.familie.ba.sak.kjerne.personident

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity(name = "AktørMergeLogg")
@Table(name = "AKTOER_MERGE_LOGG")
data class AktørMergeLogg(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "merge_logg_seq_generator")
    @SequenceGenerator(
        name = "merge_logg_seq_generator",
        sequenceName = "AKTOER_MERGE_LOGG_SEQ",
        allocationSize = 50,
    ) val id: Long = 0,
    @Column(name = "fk_fagsak_id", nullable = false, updatable = false)
    val fagsakId: Long,
    @Column(name = "historisk_aktoer_id", updatable = false, length = 50) val historiskAktørId: String,
    @Column(name = "ny_aktoer_id", updatable = false, length = 50) val nyAktørId: String,
    @Column(name = "merge_tid") var mergeTidspunkt: LocalDateTime,
)
