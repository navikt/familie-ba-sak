package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import no.nav.familie.ba.sak.integrasjoner.domene.UkjentBosted
import no.nav.familie.ba.sak.integrasjoner.domene.Vegadresse
import javax.persistence.Column
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity(name = "VegadressePdl")
@DiscriminatorValue("Vegadresse")
data class VegadressePdl(
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

    fun toVegadressePdl (vegadresse: Vegadresse): VegadressePdl =
            VegadressePdl()
}
