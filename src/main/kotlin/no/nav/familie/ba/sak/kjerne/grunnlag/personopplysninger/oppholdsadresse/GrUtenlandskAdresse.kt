package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.oppholdsadresse

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import no.nav.familie.ba.sak.common.Utils.storForbokstav
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlUtenlandskAdresssePersonUtenlandskAdresse
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrUtenlandskAdresseOppholdsadresse")
@DiscriminatorValue("UtenlandskAdresse")
data class GrUtenlandskAdresse(
    @Column(name = "adressenavn")
    val adressenavnNummer: String?,
    @Column(name = "husnummer")
    val bygningEtasjeLeilighet: String?,
    @Column(name = "postnummer")
    val postkode: String?,
    @Column(name = "by_sted")
    val bySted: String?,
    @Column(name = "region")
    val regionDistriktOmraade: String?,
    @Column(name = "landkode")
    val landkode: String,
) : GrOppholdsadresse() {
    override fun tilKopiForNyPerson(): GrOppholdsadresse =
        GrUtenlandskAdresse(
            adressenavnNummer,
            bygningEtasjeLeilighet,
            postkode,
            bySted,
            regionDistriktOmraade,
            landkode,
        )

    override fun toSecureString(): String =
        """
        UtenlandskAdresseDao(
            adressenavnNummer=$adressenavnNummer,
            bygningEtasjeLeilighet=$bygningEtasjeLeilighet,
            postkode=$postkode,
            bySted=$bySted,
            regionDistriktOmraade=$regionDistriktOmraade,
            landkode=$landkode,
            oppholdAnnetSted=$oppholdAnnetSted
        )
        """.trimMargin()

    override fun tilFrontendString(): String {
        val adressenavnNummer = adressenavnNummer?.storForbokstav()
        val bygningEtasjeLeilighet = bygningEtasjeLeilighet?.let { ", $it" } ?: ""
        val postkode = postkode?.let { ", $it" } ?: ""
        val bySted = bySted?.let { ", $it" } ?: ""
        val regionDistriktOmraade = regionDistriktOmraade?.let { ", $it" } ?: ""
        val landkode = landkode.let { ", $it" }
        return adressenavnNummer?.let {
            "$adressenavnNummer$bygningEtasjeLeilighet$postkode$bySted$regionDistriktOmraade$landkode"
        } ?: "Ukjent utenlandsk adresse$landkode"
    }

    override fun toString(): String = "UtenlandskAdresse(detaljer skjult)"

    companion object {
        fun fraUtenlandskAdresse(utenlandskAdresse: PdlUtenlandskAdresssePersonUtenlandskAdresse): GrUtenlandskAdresse =
            GrUtenlandskAdresse(
                utenlandskAdresse.adressenavnNummer.takeUnless { it.isNullOrBlank() },
                utenlandskAdresse.bygningEtasjeLeilighet.takeUnless { it.isNullOrBlank() },
                utenlandskAdresse.postkode.takeUnless { it.isNullOrBlank() },
                utenlandskAdresse.bySted.takeUnless { it.isNullOrBlank() },
                utenlandskAdresse.regionDistriktOmraade.takeUnless { it.isNullOrBlank() },
                utenlandskAdresse.landkode,
            )
    }
}
