package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.oppholdsadresse

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted.PAA_SVALBARD
import java.util.Objects

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrMatrikkeladresseOppholdsadresse")
@DiscriminatorValue("Matrikkeladresse")
data class GrMatrikkeladresseOppholdsadresse(
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
        GrMatrikkeladresseOppholdsadresse(
            matrikkelId,
            bruksenhetsnummer,
            tilleggsnavn,
            postnummer,
            kommunenummer,
        )

    override fun toSecureString(): String =
        "GrMatrikkeladresseOppholdsadresse(" +
            "matrikkelId=$matrikkelId, " +
            "bruksenhetsnummer=$bruksenhetsnummer, " +
            "tilleggsnavn=$tilleggsnavn, " +
            "postnummer=$postnummer, " +
            "kommunenummer=$kommunenummer, " +
            "oppholdAnnetSted=$oppholdAnnetSted" +
            ")"

    override fun toString(): String = "GrMatrikkeladresseOppholdsadresse(detaljer skjult)"

    override fun tilFrontendString(): String {
        val postnummer = postnummer?.let { "Postnummer $postnummer" }
        val oppholdAnnetSted = oppholdAnnetSted.takeIf { it == PAA_SVALBARD }?.let { ", $it" } ?: ""
        return postnummer?.let { "$postnummer$oppholdAnnetSted" } ?: "Ukjent adresse$oppholdAnnetSted"
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val otherMatrikkeladresse = other as GrMatrikkeladresseOppholdsadresse
        return this === other ||
            matrikkelId != null &&
            matrikkelId == otherMatrikkeladresse.matrikkelId &&
            bruksenhetsnummer == otherMatrikkeladresse.bruksenhetsnummer
    }

    override fun hashCode(): Int = Objects.hash(matrikkelId)

    companion object {
        fun fraMatrikkeladresse(matrikkeladresse: Matrikkeladresse): GrMatrikkeladresseOppholdsadresse =
            GrMatrikkeladresseOppholdsadresse(
                matrikkelId = matrikkeladresse.matrikkelId,
                bruksenhetsnummer = matrikkeladresse.bruksenhetsnummer.takeUnless { it.isNullOrBlank() },
                tilleggsnavn = matrikkeladresse.tilleggsnavn.takeUnless { it.isNullOrBlank() },
                postnummer = matrikkeladresse.postnummer.takeUnless { it.isNullOrBlank() },
                kommunenummer = matrikkeladresse.kommunenummer.takeUnless { it.isNullOrBlank() },
            )
    }
}
