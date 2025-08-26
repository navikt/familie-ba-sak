package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.oppholdsadresse

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import java.util.Objects

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrMatrikkeladresseOppholdsadresse")
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
) : GrOppholdsadresse() {
    override fun tilKopiForNyPerson(): GrOppholdsadresse =
        GrMatrikkeladresse(
            matrikkelId,
            bruksenhetsnummer,
            tilleggsnavn,
            postnummer,
            kommunenummer,
        )

    override fun toSecureString(): String =
        """MatrikkeladresseDao(matrikkelId=$matrikkelId,bruksenhetsnummer=$bruksenhetsnummer,tilleggsnavn=$tilleggsnavn,
|               postnummer=$postnummer,kommunenummer=$kommunenummer
        """.trimMargin()

    override fun toString(): String = "Matrikkeladresse(detaljer skjult)"

    override fun tilFrontendString() = postnummer?.let { "Postnummer $postnummer" } ?: "Ukjent adresse"

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val otherMatrikkeladresse = other as GrMatrikkeladresse
        return this === other ||
            matrikkelId != null &&
            matrikkelId == otherMatrikkeladresse.matrikkelId &&
            bruksenhetsnummer == otherMatrikkeladresse.bruksenhetsnummer
    }

    override fun hashCode(): Int = Objects.hash(matrikkelId)

    companion object {
        fun fraMatrikkeladresse(matrikkeladresse: Matrikkeladresse): GrMatrikkeladresse =
            GrMatrikkeladresse(
                matrikkelId = matrikkeladresse.matrikkelId,
                bruksenhetsnummer = matrikkeladresse.bruksenhetsnummer.takeUnless { it.isNullOrBlank() },
                tilleggsnavn = matrikkeladresse.tilleggsnavn.takeUnless { it.isNullOrBlank() },
                postnummer = matrikkeladresse.postnummer.takeUnless { it.isNullOrBlank() },
                kommunenummer = matrikkeladresse.kommunenummer.takeUnless { it.isNullOrBlank() },
            )
    }
}
