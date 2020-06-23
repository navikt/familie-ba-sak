package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import no.nav.familie.kontrakter.felles.personinfo.Vegadresse
import java.util.*
import javax.persistence.Column
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity(name = "GrVegadresse")
@DiscriminatorValue("Vegadresse")
data class GrVegadresse(
        @Column(name = "matrikkel_id")
        val matrikkelId: Long?,

        @Column(name = "husnummer")
        val husnummer: String?,

        @Column(name = "husbokstav")
        val husbokstav: String?,

        @Column(name = "bruksenhetsnummer")
        val bruksenhetsnummer: String?,

        @Column(name = "adressenavn")
        val adressenavn: String?,

        @Column(name = "kommunenummer")
        val kommunenummer: String?,

        @Column(name = "tilleggsnavn")
        val tilleggsnavn: String?,

        @Column(name = "postnummer")
        val postnummer: String?

) : GrBostedsadresse() {

    override fun toSecureString(): String {
        return """VegadresseDao(husnummer=$husnummer,husbokstav=$husbokstav,matrikkelId=$matrikkelId,bruksenhetsnummer=$bruksenhetsnummer,
|           adressenavn=$adressenavn,kommunenummer=$kommunenummer,tilleggsnavn=$tilleggsnavn,postnummer=$postnummer""".trimMargin()
    }

    override fun toString(): String{
        return ""
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val otherVegadresse = other as GrVegadresse
        return this === other
               || matrikkelId != null
               && matrikkelId == otherVegadresse.matrikkelId
               && bruksenhetsnummer == otherVegadresse.bruksenhetsnummer
    }

    override fun hashCode(): Int {
        return Objects.hash(matrikkelId, bruksenhetsnummer)
    }

    companion object {
        fun fraVegadresse(vegadresse: Vegadresse): GrVegadresse =
                GrVegadresse(
                        matrikkelId = vegadresse.matrikkelId,
                        husnummer = vegadresse.husnummer,
                        husbokstav = vegadresse.husbokstav,
                        bruksenhetsnummer = vegadresse.bruksenhetsnummer,
                        adressenavn = vegadresse.adressenavn,
                        kommunenummer = vegadresse.kommunenummer,
                        tilleggsnavn = vegadresse.tilleggsnavn,
                        postnummer = vegadresse.postnummer
                )
    }
}
