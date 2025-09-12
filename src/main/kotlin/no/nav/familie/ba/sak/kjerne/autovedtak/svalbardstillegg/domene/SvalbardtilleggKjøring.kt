package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardstillegg.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate

@Entity(name = "SvalbardtilleggKjøring")
@Table(name = "svalbardtillegg_kjoering")
data class SvalbardtilleggKjøring(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "svalbardtillegg_kjoering_seq_generator")
    @SequenceGenerator(
        name = "svalbardtillegg_kjoering_seq_generator",
        sequenceName = "svalbardtillegg_kjoering_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @Column(name = "fk_fagsak_id", nullable = false, updatable = false, unique = true)
    val fagsakId: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as SvalbardtilleggKjøring

        return id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String = this::class.simpleName + "(id = $id, fagsakId = $fagsakId)"
}
