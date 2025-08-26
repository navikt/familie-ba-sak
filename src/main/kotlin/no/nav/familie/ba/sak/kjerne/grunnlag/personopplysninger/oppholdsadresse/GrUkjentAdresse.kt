package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.oppholdsadresse

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrUkjentAdresseOppholdsadresse")
@DiscriminatorValue("UkjentAdresse")
class GrUkjentAdresse : GrOppholdsadresse() {
    override fun toSecureString(): String = "UkjentAdresseDao(${oppholdAnnetSted ?: ""})"

    override fun tilFrontendString(): String = "Ukjent adresse${oppholdAnnetSted?.let { ", $it" } ?: ""}"

    override fun tilKopiForNyPerson(): GrOppholdsadresse = GrUkjentAdresse()
}
