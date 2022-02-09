package no.nav.familie.ba.sak.integrasjoner.skyggesak

import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
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

    @OneToOne(optional = false) @JoinColumn(name = "fk_fagsak_id", nullable = false, updatable = false)
    val fagsak: Fagsak,

    @Column(name = "sendt_tid")
    var sendtTidspunkt: LocalDateTime? = null,
)
