package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import no.nav.familie.kontrakter.felles.personinfo.Vegadresse
import javax.persistence.Column
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity(name = "VegadressePdl")
@DiscriminatorValue("Vegadresse")
data class VegadressePdl(
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

) : BostedsadressePdl() {

    override fun toString(): String {
        return """VegadresseDao(husnummer=$husnummer,husbokstav=$husbokstav,bruksenhetsnummer=$bruksenhetsnummer,
|           adressenavn=$adressenavn,kommunenummer=$kommunenummer,tilleggsnavn=$tilleggsnavn,postnummer=$postnummer""".trimMargin()
    }

    companion object {
        fun fraVegadresse(vegadresse: Vegadresse): VegadressePdl =
                VegadressePdl(
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
