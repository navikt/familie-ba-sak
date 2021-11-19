package no.nav.familie.ba.sak.kjerne.personident

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.aktørid.AktørId
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDateTime
import java.util.Objects
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Personident")
@Table(name = "PERSONIDENT")
data class Personident(
    @Id
    @Column(name = "foedselsnummer", nullable = false)
    val fødselsnummer: String,

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_aktoer_id", nullable = false, updatable = false)
    val aktørId: AktørId,

    @Column(name = "aktiv", nullable = false)
    var aktiv: Boolean = true,

    @Column(name = "gjelder_til", columnDefinition = "DATE")
    var gjelderTil: LocalDateTime? = null,

) : BaseEntitet() {

    override fun toString(): String {
        return """Personident(aktørId=${aktørId.aktørId},
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
        return Objects.hash(fødselsnummer)
    }
}
