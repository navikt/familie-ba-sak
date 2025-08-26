package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.oppholdsadresse

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.OppholdAnnetSted.PAA_SVALBARD
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrUkjentAdresseOppholdsadresse")
@DiscriminatorValue("UkjentAdresse")
class GrUkjentAdresse : GrOppholdsadresse() {
    override fun toString(): String = "UkjentAdresse(detaljer skjult)"

    override fun toSecureString(): String = "UkjentAdresseDao(${oppholdAnnetSted ?: ""})"

    override fun tilFrontendString(): String = "Ukjent adresse${oppholdAnnetSted.takeIf { it == PAA_SVALBARD }?.let { ", $it" } ?: ""}"

    override fun tilKopiForNyPerson(): GrOppholdsadresse = GrUkjentAdresse()
}
