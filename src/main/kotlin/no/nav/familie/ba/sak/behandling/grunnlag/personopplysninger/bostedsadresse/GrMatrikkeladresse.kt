package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.bostedsadresse

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import java.util.*
import javax.persistence.Column
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrMatrikkeladresse")
@DiscriminatorValue("Matrikkeladresse")
data class GrMatrikkeladresse(
        @Column(name = "matrikkel_id")
        val matrikkelId: Long?,

        @Column(name = "bruksenhetsnummer")
        val bruksenhetsnummer: String?,

        @Column(name = "tilleggsnavn")
        val tilleggsnavn: String?,

        @Column(name = "postnummer")
        val postnummer: String?,

        @Column(name = "kommunenummer")
        val kommunenummer: String?,

        @JsonIgnore
        @ManyToOne(optional = false)
        @JoinColumn(name = "fk_po_person_id", nullable = false, updatable = false)
        override val person: Person,

        ) : GrBostedsadresse(person = person) {

    override fun toSecureString(): String {
        return """MatrikkeladresseDao(matrikkelId=$matrikkelId,bruksenhetsnummer=$bruksenhetsnummer,tilleggsnavn=$tilleggsnavn,
|               postnummer=$postnummer,kommunenummer=$kommunenummer""".trimMargin()
    }

    override fun toString(): String {
        return "Matrikkeladresse(detaljer skjult)"
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val otherMatrikkeladresse = other as GrMatrikkeladresse
        return this === other
               || matrikkelId != null
               && matrikkelId == otherMatrikkeladresse.matrikkelId
               && bruksenhetsnummer == otherMatrikkeladresse.bruksenhetsnummer
    }

    override fun hashCode(): Int {
        return Objects.hash(matrikkelId, bruksenhetsnummer)
    }

    companion object {

        fun fraMatrikkeladresse(matrikkeladresse: Matrikkeladresse): GrMatrikkeladresse =
                GrMatrikkeladresse(
                        matrikkelId = matrikkeladresse.matrikkelId,
                        bruksenhetsnummer = matrikkeladresse.bruksenhetsnummer,
                        tilleggsnavn = matrikkeladresse.tilleggsnavn,
                        postnummer = matrikkeladresse.postnummer,
                        kommunenummer = matrikkeladresse.kommunenummer
                )
    }
}
