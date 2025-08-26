package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.oppholdsadresse

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import no.nav.familie.ba.sak.common.Utils.nullableTilString
import no.nav.familie.ba.sak.common.Utils.storForbokstav
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.OppholdAnnetSted
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import java.util.Objects

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrVegadresseOppholdsadresse")
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
    val postnummer: String?,
) : GrOppholdsadresse() {
    override fun tilKopiForNyPerson(): GrOppholdsadresse =
        GrVegadresse(
            matrikkelId,
            husnummer,
            husbokstav,
            bruksenhetsnummer,
            adressenavn,
            kommunenummer,
            tilleggsnavn,
            postnummer,
        )

    override fun toSecureString(): String =
        """VegadresseDao(husnummer=$husnummer,husbokstav=$husbokstav,matrikkelId=$matrikkelId,bruksenhetsnummer=$bruksenhetsnummer,
|           adressenavn=$adressenavn,kommunenummer=$kommunenummer,tilleggsnavn=$tilleggsnavn,postnummer=$postnummer
        """.trimMargin()

    override fun toString(): String = "Vegadresse(detaljer skjult)"

    override fun tilFrontendString(): String {
        val adressenavn = adressenavn?.storForbokstav()
        val husnummer = husnummer.nullableTilString()
        val husbokstav = husbokstav.nullableTilString()
        val postnummer = postnummer?.let { ", $it" } ?: ""
        val oppholdAnnetSted = oppholdAnnetSted.takeIf { it == OppholdAnnetSted.PAA_SVALBARD }
        return when {
            adressenavn == null && oppholdAnnetSted == null -> "Ukjent adresse"
            adressenavn == null -> "$oppholdAnnetSted"
            oppholdAnnetSted == null -> "$adressenavn $husnummer$husbokstav$postnummer"
            else -> "$adressenavn $husnummer$husbokstav$postnummer, $oppholdAnnetSted"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val otherVegadresse = other as GrVegadresse

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

    companion object {
        fun fraVegadresse(vegadresse: Vegadresse): GrVegadresse =
            GrVegadresse(
                matrikkelId = vegadresse.matrikkelId,
                husnummer = vegadresse.husnummer.takeUnless { it.isNullOrBlank() },
                husbokstav = vegadresse.husbokstav.takeUnless { it.isNullOrBlank() },
                bruksenhetsnummer = vegadresse.bruksenhetsnummer.takeUnless { it.isNullOrBlank() },
                adressenavn = vegadresse.adressenavn.takeUnless { it.isNullOrBlank() },
                kommunenummer = vegadresse.kommunenummer.takeUnless { it.isNullOrBlank() },
                tilleggsnavn = vegadresse.tilleggsnavn.takeUnless { it.isNullOrBlank() },
                postnummer = vegadresse.postnummer.takeUnless { it.isNullOrBlank() },
            )
    }
}
