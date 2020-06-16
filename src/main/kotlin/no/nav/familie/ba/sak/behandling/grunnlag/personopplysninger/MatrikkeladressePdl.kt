package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import javax.persistence.Column
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity(name = "MatrikkeladressePdl")
@DiscriminatorValue("Matrikkeladresse")
data class MatrikkeladressePdl(

        @Column(name = "bruksenhetsnummer")
        val bruksenhetsnummer: String,

        @Column(name = "tilleggsnavn")
        val tilleggsnavn: String,

        @Column(name = "postnummer")
        val postnummer: String,

        @Column(name = "kommunenummer")
        val kommunenummer: String

) : BostedsadressePdl() {

    override fun toString(): String {
        return """MatrikkeladresseDao(bruksenhetsnummer=$bruksenhetsnummer,tilleggsnavn=$tilleggsnavn,
|               postnummer=$postnummer,kommunenummer=$kommunenummer""".trimMargin()
    }
}
