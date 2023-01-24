package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene

import org.hibernate.Hibernate
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
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

    @Column(name = "fk_fagsak_id", nullable = false, updatable = false, unique = true)
    val fagsakId: Long,

    @Column(name = "start_tid", nullable = false, updatable = false)
    val startTidspunkt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "ferdig_tid")
    var endretTidspunkt: LocalDateTime? = null
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as Satskjøring

        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id , fagsakId = $fagsakId )"
    }
}
