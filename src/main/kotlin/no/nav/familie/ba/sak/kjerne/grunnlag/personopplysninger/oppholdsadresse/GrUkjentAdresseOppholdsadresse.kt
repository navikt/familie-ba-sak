package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.oppholdsadresse

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresse
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted.PAA_SVALBARD
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrUkjentAdresseOppholdsadresse")
@DiscriminatorValue("UkjentAdresse")
class GrUkjentAdresseOppholdsadresse : GrOppholdsadresse() {
    override fun toString(): String = "GrUkjentAdresseOppholdsadresse(detaljer skjult)"

    override fun toSecureString(): String = "GrUkjentAdresseOppholdsadresse(${oppholdAnnetSted ?: ""})"

    override fun tilFrontendString(): String = "Ukjent adresse${oppholdAnnetSted.takeIf { it == PAA_SVALBARD }?.let { ", $it" } ?: ""}"

    override fun erPåSvalbard(): Boolean = oppholdAnnetSted == PAA_SVALBARD

    override fun tilKopiForNyPerson(): GrOppholdsadresse = GrUkjentAdresseOppholdsadresse()

    override fun tilAdresse(): Adresse =
        Adresse(
            gyldigFraOgMed = periode?.fom,
            gyldigTilOgMed = periode?.tom,
            oppholdAnnetSted = oppholdAnnetSted,
        )
}
