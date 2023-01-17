package no.nav.familie.ba.sak.kjerne.brev.mottaker

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import org.hibernate.Hibernate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Brevmottaker")
@Table(name = "BREVMOTTAKER")
data class Brevmottaker(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "brevmottaker_seq_generator")
    @SequenceGenerator(name = "brevmottaker_seq_generator", sequenceName = "brevmottaker_seq", allocationSize = 50)
    val id: Long = 0,

    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    val behandlingId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: MottakerType,

    @Column(name = "navn", nullable = false, length = 70)
    var navn: String,

    @Column(name = "adresselinje_1", nullable = false, length = 40)
    var adresselinje1: String,

    @Column(name = "adresselinje_2", length = 40)
    var adresselinje2: String? = null,

    @Column(name = "postnummer", nullable = false, length = 10)
    var postnummer: String,

    @Column(name = "poststed", nullable = false, length = 30)
    var poststed: String,

    @Column(name = "landkode", nullable = false, length = 2)
    var landkode: String
) : BaseEntitet() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as Brevmottaker

        return id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(" +
            "id = $id, " +
            "behandlingId = $behandlingId, " +
            "type = $type, " +
            "navn = $navn, " +
            "adresselinje1 = $adresselinje1, " +
            "adresselinje2 = $adresselinje2, " +
            "postnummer = $postnummer, " +
            "poststed = $poststed, " +
            "landkode = $landkode)"
    }
}

enum class MottakerType {
    BRUKER_MED_UTENLANDSK_ADRESSE,
    FULLMEKTIG,
    VERGE,
    DØDSBO
}
