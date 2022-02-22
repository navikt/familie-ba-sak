package no.nav.familie.ba.sak.integrasjoner.skyggesak

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity(name = "Skyggesak")
@Table(name = "SKYGGESAK")
data class Skyggesak(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "skyggesak_seq_generator")
    @SequenceGenerator(
        name = "skyggesak_seq_generator",
        sequenceName = "SKYGGESAK_SEQ",
        allocationSize = 50
    )
    val id: Long = 0,

    @Column(name = "fk_fagsak_id", nullable = false, updatable = false)
    val fagsakId: Long,

    @Column(name = "sendt_tid")
    var sendtTidspunkt: LocalDateTime? = null,
)
