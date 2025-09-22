package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.oppholdsadresse

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted.PAA_SVALBARD

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrUkjentAdresseOppholdsadresse")
@DiscriminatorValue("UkjentAdresse")
class GrUkjentAdresseOppholdsadresse : GrOppholdsadresse() {
    override fun toString(): String = "GrUkjentAdresseOppholdsadresse(detaljer skjult)"

    override fun toSecureString(): String = "GrUkjentAdresseOppholdsadresse(${oppholdAnnetSted ?: ""})"

    override fun tilFrontendString(): String = "Ukjent adresse${oppholdAnnetSted.takeIf { it == PAA_SVALBARD }?.let { ", $it" } ?: ""}"

    override fun erPåSvalbard(): Boolean = oppholdAnnetSted == PAA_SVALBARD

    override fun tilKopiForNyPerson(): GrOppholdsadresse = GrUkjentAdresseOppholdsadresse()
}
