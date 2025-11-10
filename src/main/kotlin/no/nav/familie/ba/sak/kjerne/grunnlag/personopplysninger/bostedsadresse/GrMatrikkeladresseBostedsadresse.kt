package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import java.util.Objects

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrMatrikkeladresseBostedsadresse")
@DiscriminatorValue("Matrikkeladresse")
data class GrMatrikkeladresseBostedsadresse(
    @Column(name = "matrikkel_id")
    val matrikkelId: Long?,
    @Column(name = "bruksenhetsnummer")
    val bruksenhetsnummer: String?,
    @Column(name = "tilleggsnavn")
    val tilleggsnavn: String?,
    @Column(name = "postnummer")
    val postnummer: String?,
    @Column(name = "poststed")
    val poststed: String?,
    @Column(name = "kommunenummer")
    val kommunenummer: String?,
) : GrBostedsadresse() {
    override fun tilKopiForNyPerson(): GrBostedsadresse =
        GrMatrikkeladresseBostedsadresse(
            matrikkelId,
            bruksenhetsnummer,
            tilleggsnavn,
            postnummer,
            poststed,
            kommunenummer,
        )

    override fun toSecureString(): String =
        """GrMatrikkeladresseBostedsadresse(matrikkelId=$matrikkelId,bruksenhetsnummer=$bruksenhetsnummer,tilleggsnavn=$tilleggsnavn,
|               postnummer=$postnummer,poststed=$poststed,kommunenummer=$kommunenummer
        """.trimMargin()

    override fun toString(): String = "GrMatrikkeladresseBostedsadresse(detaljer skjult)"

    override fun tilFrontendString() = """Matrikkel $matrikkelId, bruksenhet $bruksenhetsnummer, postnummer $postnummer${poststed?.let { ", $it" } ?: ""}""".trimMargin()

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val otherMatrikkeladresse = other as GrMatrikkeladresseBostedsadresse
        return this === other ||
            matrikkelId != null &&
            matrikkelId == otherMatrikkeladresse.matrikkelId &&
            bruksenhetsnummer == otherMatrikkeladresse.bruksenhetsnummer
    }

    override fun hashCode(): Int = Objects.hash(matrikkelId)

    override fun tilAdresse(): Adresse =
        Adresse(
            gyldigFraOgMed = periode?.fom,
            gyldigTilOgMed = periode?.tom,
            matrikkeladresse =
                Matrikkeladresse(
                    matrikkelId = matrikkelId,
                    bruksenhetsnummer = bruksenhetsnummer,
                    tilleggsnavn = tilleggsnavn,
                    postnummer = postnummer,
                    kommunenummer = kommunenummer,
                ),
        )

    companion object {
        fun fraMatrikkeladresse(
            matrikkeladresse: Matrikkeladresse,
            poststed: String?,
        ): GrMatrikkeladresseBostedsadresse =
            GrMatrikkeladresseBostedsadresse(
                matrikkelId = matrikkeladresse.matrikkelId,
                bruksenhetsnummer = matrikkeladresse.bruksenhetsnummer,
                tilleggsnavn = matrikkeladresse.tilleggsnavn,
                postnummer = matrikkeladresse.postnummer,
                poststed = poststed,
                kommunenummer = matrikkeladresse.kommunenummer,
            )
    }
}
