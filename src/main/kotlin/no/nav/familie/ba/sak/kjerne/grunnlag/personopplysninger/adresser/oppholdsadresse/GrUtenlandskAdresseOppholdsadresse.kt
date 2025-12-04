package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.oppholdsadresse

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import no.nav.familie.ba.sak.common.Utils.storForbokstavIHvertOrd
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresse
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted.PAA_SVALBARD
import no.nav.familie.kontrakter.felles.personopplysning.UtenlandskAdresse

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrUtenlandskAdresseOppholdsadresse")
@DiscriminatorValue("UtenlandskAdresse")
data class GrUtenlandskAdresseOppholdsadresse(
    @Column(name = "adressenavn")
    val adressenavnNummer: String?,
    @Column(name = "husnummer")
    val bygningEtasjeLeilighet: String?,
    @Column(name = "postboks")
    val postboksNummerNavn: String?,
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
        GrUtenlandskAdresseOppholdsadresse(
            adressenavnNummer,
            bygningEtasjeLeilighet,
            postboksNummerNavn,
            postkode,
            bySted,
            regionDistriktOmraade,
            landkode,
        )

    override fun toSecureString(): String =
        "GrUtenlandskAdresseOppholdsadresse(" +
            "adressenavnNummer=$adressenavnNummer, " +
            "bygningEtasjeLeilighet=$bygningEtasjeLeilighet, " +
            "postboksNummerNavn=$postboksNummerNavn, " +
            "postkode=$postkode, " +
            "bySted=$bySted, " +
            "regionDistriktOmraade=$regionDistriktOmraade, " +
            "landkode=$landkode, " +
            "oppholdAnnetSted=$oppholdAnnetSted" +
            ")"

    override fun tilFrontendString(): String {
        val adressenavnNummer = adressenavnNummer?.storForbokstavIHvertOrd()
        val bygningEtasjeLeilighet = bygningEtasjeLeilighet?.let { ", $it" } ?: ""
        val postboks = postboksNummerNavn?.let { ", $it" } ?: ""
        val postkode = postkode?.let { ", $it" } ?: ""
        val bySted = bySted?.let { ", $it" } ?: ""
        val regionDistriktOmraade = regionDistriktOmraade?.let { ", $it" } ?: ""
        val landkode = landkode?.let { ", $it" } ?: ""
        return adressenavnNummer?.let {
            "$adressenavnNummer$bygningEtasjeLeilighet$postboks$postkode$bySted$regionDistriktOmraade$landkode"
        } ?: "Ukjent utenlandsk adresse$landkode"
    }

    override fun erPåSvalbard(): Boolean = oppholdAnnetSted == PAA_SVALBARD

    override fun toString(): String = "GrUtenlandskAdresseOppholdsadresse(detaljer skjult)"

    override fun tilAdresse(): Adresse =
        Adresse(
            gyldigFraOgMed = periode?.fom,
            gyldigTilOgMed = periode?.tom,
            oppholdAnnetSted = oppholdAnnetSted,
            utenlandskAdresse =
                UtenlandskAdresse(
                    adressenavnNummer = adressenavnNummer,
                    bygningEtasjeLeilighet = bygningEtasjeLeilighet,
                    postboksNummerNavn = postboksNummerNavn,
                    postkode = postkode,
                    bySted = bySted,
                    regionDistriktOmraade = regionDistriktOmraade,
                    landkode = landkode,
                ),
        )

    companion object {
        fun fraUtenlandskAdresse(utenlandskAdresse: UtenlandskAdresse): GrUtenlandskAdresseOppholdsadresse =
            GrUtenlandskAdresseOppholdsadresse(
                utenlandskAdresse.adressenavnNummer.takeUnless { it.isNullOrBlank() },
                utenlandskAdresse.bygningEtasjeLeilighet.takeUnless { it.isNullOrBlank() },
                utenlandskAdresse.postboksNummerNavn.takeUnless { it.isNullOrBlank() },
                utenlandskAdresse.postkode.takeUnless { it.isNullOrBlank() },
                utenlandskAdresse.bySted.takeUnless { it.isNullOrBlank() },
                utenlandskAdresse.regionDistriktOmraade.takeUnless { it.isNullOrBlank() },
                utenlandskAdresse.landkode,
            )
    }
}
