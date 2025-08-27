package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrUkjentBostedBostedsadresse")
@DiscriminatorValue("ukjentBosted")
data class GrUkjentBostedBostedsadresse(
    @Column(name = "bostedskommune")
    val bostedskommune: String,
) : GrBostedsadresse() {
    override fun tilKopiForNyPerson(): GrBostedsadresse = GrUkjentBostedBostedsadresse(bostedskommune)

    override fun toSecureString(): String = """GrUkjentBostedBostedsadresse(bostedskommune=$bostedskommune""".trimMargin()

    override fun tilFrontendString() = """Ukjent adresse, kommune $bostedskommune""".trimMargin()

    override fun toString(): String = "GrUkjentBostedBostedsadresse(detaljer skjult)"

    companion object {
        fun fraUkjentBosted(ukjentBosted: UkjentBosted): GrUkjentBostedBostedsadresse = GrUkjentBostedBostedsadresse(bostedskommune = ukjentBosted.bostedskommune)
    }
}
