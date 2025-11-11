package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.deltbosted

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresse
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrUkjentBostedDeltBosted")
@DiscriminatorValue("ukjentBosted")
data class GrUkjentBostedDeltBosted(
    @Column(name = "bostedskommune")
    val bostedskommune: String,
) : GrDeltBosted() {
    override fun tilKopiForNyPerson(): GrDeltBosted = GrUkjentBostedDeltBosted(bostedskommune)

    override fun toSecureString(): String = """GrUkjentBostedDeltBosted(bostedskommune=$bostedskommune)""".trimMargin()

    override fun tilFrontendString() = """Ukjent adresse, kommune $bostedskommune""".trimMargin()

    override fun toString(): String = "GrUkjentBostedDeltBosted(detaljer skjult)"

    override fun tilAdresse(): Adresse =
        Adresse(
            gyldigFraOgMed = periode?.fom,
            gyldigTilOgMed = periode?.tom,
            ukjentBosted = UkjentBosted(bostedskommune = bostedskommune),
        )

    companion object {
        fun fraUkjentBosted(ukjentBosted: UkjentBosted): GrUkjentBostedDeltBosted = GrUkjentBostedDeltBosted(bostedskommune = ukjentBosted.bostedskommune)
    }
}
