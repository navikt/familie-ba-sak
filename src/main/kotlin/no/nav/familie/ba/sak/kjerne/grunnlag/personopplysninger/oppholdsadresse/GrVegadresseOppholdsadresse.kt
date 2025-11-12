package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.oppholdsadresse

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import no.nav.familie.ba.sak.common.Utils.nullableTilString
import no.nav.familie.ba.sak.common.Utils.storForbokstavIHvertOrd
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresse
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted.PAA_SVALBARD
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import no.nav.familie.kontrakter.felles.svalbard.erKommunePåSvalbard
import java.util.Objects

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrVegadresseOppholdsadresse")
@DiscriminatorValue("Vegadresse")
data class GrVegadresseOppholdsadresse(
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
    val postnummer: String?,
    @Column(name = "poststed")
    val poststed: String?,
) : GrOppholdsadresse() {
    override fun tilKopiForNyPerson(): GrOppholdsadresse =
        GrVegadresseOppholdsadresse(
            matrikkelId,
            husnummer,
            husbokstav,
            bruksenhetsnummer,
            adressenavn,
            kommunenummer,
            tilleggsnavn,
            postnummer,
            poststed,
        )

    override fun toSecureString(): String =
        "GrVegadresseOppholdsadresse(" +
            "husnummer=$husnummer, " +
            "husbokstav=$husbokstav, " +
            "matrikkelId=$matrikkelId, " +
            "bruksenhetsnummer=$bruksenhetsnummer, " +
            "adressenavn=$adressenavn, " +
            "kommunenummer=$kommunenummer, " +
            "tilleggsnavn=$tilleggsnavn, " +
            "postnummer=$postnummer, " +
            "poststed=$poststed, " +
            "oppholdAnnetSted=$oppholdAnnetSted" +
            ")"

    override fun toString(): String = "GrVegadresseOppholdsadresse(detaljer skjult)"

    override fun tilFrontendString(): String {
        val adressenavn = adressenavn?.storForbokstavIHvertOrd()
        val husnummer = husnummer.nullableTilString()
        val husbokstav = husbokstav.nullableTilString()
        val postnummer = postnummer?.let { ", $it" } ?: ""
        val poststed = poststed?.let { ", $it" } ?: ""
        val oppholdAnnetSted = oppholdAnnetSted.takeIf { it == PAA_SVALBARD }?.let { ", $it" } ?: ""
        return when (adressenavn) {
            null -> "Ukjent adresse$oppholdAnnetSted"
            else -> "$adressenavn $husnummer$husbokstav$postnummer$poststed$oppholdAnnetSted"
        }
    }

    override fun erPåSvalbard(): Boolean = (kommunenummer != null && erKommunePåSvalbard(kommunenummer)) || oppholdAnnetSted == PAA_SVALBARD

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val otherVegadresse = other as GrVegadresseOppholdsadresse

        return this === other ||
            (
                (matrikkelId != null && matrikkelId == otherVegadresse.matrikkelId) ||
                    (
                        (matrikkelId == null && otherVegadresse.matrikkelId == null) &&
                            postnummer != null &&
                            !(adressenavn == null && husnummer == null && husbokstav == null) &&
                            (adressenavn == otherVegadresse.adressenavn) &&
                            (husnummer == otherVegadresse.husnummer) &&
                            (husbokstav == otherVegadresse.husbokstav) &&
                            (postnummer == otherVegadresse.postnummer)
                    )
            )
    }

    override fun hashCode(): Int = Objects.hash(matrikkelId)

    override fun tilAdresse(): Adresse =
        Adresse(
            gyldigFraOgMed = periode?.fom,
            gyldigTilOgMed = periode?.tom,
            oppholdAnnetSted = oppholdAnnetSted,
            vegadresse =
                Vegadresse(
                    matrikkelId = matrikkelId,
                    husnummer = husnummer,
                    husbokstav = husbokstav,
                    bruksenhetsnummer = bruksenhetsnummer,
                    adressenavn = adressenavn,
                    kommunenummer = kommunenummer,
                    tilleggsnavn = tilleggsnavn,
                    postnummer = postnummer,
                ),
        )

    companion object {
        fun fraVegadresse(
            vegadresse: Vegadresse,
            poststed: String?,
        ): GrVegadresseOppholdsadresse =
            GrVegadresseOppholdsadresse(
                matrikkelId = vegadresse.matrikkelId,
                husnummer = vegadresse.husnummer.takeUnless { it.isNullOrBlank() },
                husbokstav = vegadresse.husbokstav.takeUnless { it.isNullOrBlank() },
                bruksenhetsnummer = vegadresse.bruksenhetsnummer.takeUnless { it.isNullOrBlank() },
                adressenavn = vegadresse.adressenavn.takeUnless { it.isNullOrBlank() },
                kommunenummer = vegadresse.kommunenummer.takeUnless { it.isNullOrBlank() },
                tilleggsnavn = vegadresse.tilleggsnavn.takeUnless { it.isNullOrBlank() },
                postnummer = vegadresse.postnummer.takeUnless { it.isNullOrBlank() },
                poststed = poststed.takeUnless { it.isNullOrBlank() },
            )
    }
}
