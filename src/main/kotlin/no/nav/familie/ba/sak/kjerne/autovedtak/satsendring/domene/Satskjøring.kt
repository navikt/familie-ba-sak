package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene

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

@Entity(name = "Satskjøring")
@Table(name = "satskjoering")
data class Satskjøring(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "satskjoering_seq_generator")
    @SequenceGenerator(
        name = "satskjoering_seq_generator",
        sequenceName = "satskjoering_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @OneToOne(optional = false)
    @JoinColumn(name = "fk_fagsak_id", nullable = false, updatable = false, unique = true)
    val fagsak: Fagsak,

    @Column(name = "start_tid", nullable = false, updatable = false)
    val startTidspunkt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "ferdig_tid")
    var endretTidspunkt: LocalDateTime? = null
)
