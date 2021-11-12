package no.nav.familie.ba.sak.kjerne.personident

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDateTime
import java.util.Objects
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Personident")
@Table(name = "PERSONIDENT")
data class Personident(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "personident_seq_generator")
    @SequenceGenerator(name = "personident_seq_generator", sequenceName = "personident_seq", allocationSize = 50)
    val id: Long = 0,

    @Column(name = "aktoer_id", nullable = false)
    val aktørId: String,

    @Column(name = "foedselsnummer", nullable = false)
    val fødselsnummer: String,

    @Column(name = "aktiv", nullable = false)
    var aktiv: Boolean = false,

    @Column(name = "gjelder_til", columnDefinition = "DATE")
    var gjelderTil: LocalDateTime? = null,

) : BaseEntitet() {

    override fun toString(): String {
        return """Personident(aktørId=$aktørId,
                        |aktiv=$aktiv
                        |gjelderTil=$gjelderTil)""".trimMargin()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val entitet: Personident = other as Personident
        return Objects.equals(hashCode(), entitet.hashCode())
    }

    override fun hashCode(): Int {
        return Objects.hash(aktørId, fødselsnummer)
    }
}
