package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.deltbosted

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import no.nav.familie.ba.sak.common.Utils.nullableTilString
import no.nav.familie.ba.sak.common.Utils.storForbokstavIHvertOrd
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresse
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import java.util.Objects

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrVegadresseDeltBosted")
@DiscriminatorValue("Vegadresse")
data class GrVegadresseDeltBosted(
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
) : GrDeltBosted() {
    override fun tilKopiForNyPerson(): GrDeltBosted =
        GrVegadresseDeltBosted(
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
        "GrVegadresseDeltBosted(" +
            "husnummer=$husnummer, " +
            "husbokstav=$husbokstav, " +
            "matrikkelId=$matrikkelId, " +
            "bruksenhetsnummer=$bruksenhetsnummer, " +
            "adressenavn=$adressenavn, " +
            "kommunenummer=$kommunenummer, " +
            "tilleggsnavn=$tilleggsnavn, " +
            "postnummer=$postnummer, " +
            "poststed=$poststed" +
            ")"

    override fun toString(): String = "GrVegadresseDeltBosted(detaljer skjult)"

    override fun tilFrontendString(): String {
        val adressenavn = adressenavn?.storForbokstavIHvertOrd()
        val husnummer = husnummer.nullableTilString()
        val husbokstav = husbokstav.nullableTilString()
        val postnummer = postnummer?.let { ", $it" } ?: ""
        val poststed = poststed?.let { ", $it" } ?: ""
        return when (adressenavn) {
            null -> "Ukjent adresse"
            else -> "$adressenavn $husnummer$husbokstav$postnummer$poststed"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val otherVegadresse = other as GrVegadresseDeltBosted

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
        ): GrVegadresseDeltBosted =
            GrVegadresseDeltBosted(
                matrikkelId = vegadresse.matrikkelId,
                husnummer = vegadresse.husnummer,
                husbokstav = vegadresse.husbokstav,
                bruksenhetsnummer = vegadresse.bruksenhetsnummer,
                adressenavn = vegadresse.adressenavn,
                kommunenummer = vegadresse.kommunenummer,
                tilleggsnavn = vegadresse.tilleggsnavn,
                postnummer = vegadresse.postnummer,
                poststed = poststed,
            )
    }
}
