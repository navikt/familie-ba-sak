package no.nav.familie.ba.sak.kjerne.fagsak

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import java.time.LocalDateTime

@Entity(name = "FagsakLaasing")
@Table(name = "fagsak_laasing")
data class FagsakLåsing(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fagsak_laasing_seq_generator")
    @SequenceGenerator(
        name = "fagsak_laasing_seq_generator",
        sequenceName = "fagsak_laasing_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_fagsak_id", nullable = false, updatable = false)
    val fagsak: Fagsak,
    @Column(name = "tidspunkt", nullable = false, updatable = false)
    val tidspunkt: LocalDateTime,
    @Enumerated(EnumType.STRING)
    @Column(name = "hendelse", nullable = false, updatable = false)
    val hendelse: FagsakLåsHendelse,
    @Column(name = "begrunnelse", nullable = false, updatable = false)
    val begrunnelse: String,
    @Column(name = "aktiv", nullable = false)
    var aktiv: Boolean,
) : BaseEntitet()

enum class FagsakLåsHendelse {
    LÅST,
    LÅST_OPP,
}
