package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.oppholdsadresse

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresse
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted.PAA_SVALBARD
import no.nav.familie.kontrakter.felles.svalbard.erKommunePåSvalbard
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
    @Column(name = "poststed")
    val poststed: String?,
    @Column(name = "kommunenummer")
    val kommunenummer: String?,
) : GrOppholdsadresse() {
    override fun tilKopiForNyPerson(): GrOppholdsadresse =
        GrMatrikkeladresseOppholdsadresse(
            matrikkelId,
            bruksenhetsnummer,
            tilleggsnavn,
            postnummer,
            poststed,
            kommunenummer,
        )

    override fun toSecureString(): String =
        "GrMatrikkeladresseOppholdsadresse(" +
            "matrikkelId=$matrikkelId, " +
            "bruksenhetsnummer=$bruksenhetsnummer, " +
            "tilleggsnavn=$tilleggsnavn, " +
            "postnummer=$postnummer, " +
            "poststed=$poststed, " +
            "kommunenummer=$kommunenummer, " +
            "oppholdAnnetSted=$oppholdAnnetSted" +
            ")"

    override fun toString(): String = "GrMatrikkeladresseOppholdsadresse(detaljer skjult)"

    override fun tilFrontendString(): String {
        val postnummer = postnummer?.let { "Postnummer $postnummer" }
        val poststed = poststed?.let { ", $poststed" } ?: ""
        val oppholdAnnetSted = oppholdAnnetSted.takeIf { it == PAA_SVALBARD }?.let { ", $it" } ?: ""
        return postnummer?.let { "$postnummer$poststed$oppholdAnnetSted" } ?: "Ukjent adresse$oppholdAnnetSted"
    }

    override fun erPåSvalbard(): Boolean = (kommunenummer != null && erKommunePåSvalbard(kommunenummer)) || oppholdAnnetSted == PAA_SVALBARD

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val otherMatrikkeladresse = other as GrMatrikkeladresseOppholdsadresse
        return this === other ||
            (
                matrikkelId != null &&
                    matrikkelId == otherMatrikkeladresse.matrikkelId &&
                    bruksenhetsnummer == otherMatrikkeladresse.bruksenhetsnummer
            )
    }

    override fun hashCode(): Int = Objects.hash(matrikkelId)

    override fun tilAdresse(): Adresse =
        Adresse(
            gyldigFraOgMed = periode?.fom,
            gyldigTilOgMed = periode?.tom,
            oppholdAnnetSted = oppholdAnnetSted,
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
        ): GrMatrikkeladresseOppholdsadresse =
            GrMatrikkeladresseOppholdsadresse(
                matrikkelId = matrikkeladresse.matrikkelId,
                bruksenhetsnummer = matrikkeladresse.bruksenhetsnummer.takeUnless { it.isNullOrBlank() },
                tilleggsnavn = matrikkeladresse.tilleggsnavn.takeUnless { it.isNullOrBlank() },
                postnummer = matrikkeladresse.postnummer.takeUnless { it.isNullOrBlank() },
                poststed = poststed.takeUnless { it.isNullOrBlank() },
                kommunenummer = matrikkeladresse.kommunenummer.takeUnless { it.isNullOrBlank() },
            )
    }
}
