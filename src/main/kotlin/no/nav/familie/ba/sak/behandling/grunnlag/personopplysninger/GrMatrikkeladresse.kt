package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import no.nav.familie.kontrakter.felles.personinfo.Matrikkeladresse
import java.util.*
import javax.persistence.Column
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

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
        val kommunenummer: String?

) : GrBostedsadresse() {

    override fun toSecureString(): String {
        return """MatrikkeladresseDao(matrikkelId=$matrikkelId,bruksenhetsnummer=$bruksenhetsnummer,tilleggsnavn=$tilleggsnavn,
|               postnummer=$postnummer,kommunenummer=$kommunenummer""".trimMargin()
    }

    override fun toString(): String {
        return ""
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
