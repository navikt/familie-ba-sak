package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import no.nav.familie.kontrakter.felles.personinfo.UkjentBosted
import javax.persistence.Column
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity(name = "ukjentBostedTpl")
@DiscriminatorValue("ukjentBosted")
data class UkjentBostedPdl(
        @Column(name = "bostedskommune")
        val bostedskommune: String

) : BostedsadressePdl() {

    override fun toSecureString(): String {
        return """UkjentadresseDao(bostedskommune=$bostedskommune""".trimMargin()
    }

    override fun toString(): String {
        return ""
    }

    companion object {
        fun fraUkjentBosted(ukjentBosted: UkjentBosted): UkjentBostedPdl =
                UkjentBostedPdl(bostedskommune = ukjentBosted.bostedskommune)
    }
}
