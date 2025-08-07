package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.LocalDateTime

@Entity(name = "FinnmarkstilleggKjøring")
@Table(name = "finnmarkstillegg_kjoering")
data class FinnmarkstilleggKjøring(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "finnmarkstillegg_kjoering_seq_generator")
    @SequenceGenerator(
        name = "finnmarkstillegg_kjoering_seq_generator",
        sequenceName = "finnmarkstillegg_kjoering_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @Column(name = "fk_fagsak_id", nullable = false, updatable = false, unique = true)
    val fagsakId: Long,
    @Column(name = "start_tid", nullable = false, updatable = false)
    val startTidspunkt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "ferdig_tid")
    var ferdigTidspunkt: LocalDateTime? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as FinnmarkstilleggKjøring

        return id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String = this::class.simpleName + "(id = $id, fagsakId = $fagsakId)"
}
