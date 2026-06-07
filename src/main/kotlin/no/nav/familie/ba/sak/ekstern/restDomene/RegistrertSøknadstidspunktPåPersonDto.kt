package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.registrertsøknadstidspunkt.RegistrertSøknadstidspunkt
import no.nav.familie.ba.sak.kjerne.registrertsøknadstidspunkt.RegistrertSøknadstidspunktPåPerson
import java.time.LocalDate

data class EndreSøknadstidspunktRequestDto(
    val søknadstidspunktPerPerson: List<RegistrertSøknadstidspunktPåPersonDto>,
)

data class RegistrertSøknadstidspunktPåPersonDto(
    val personIdent: String,
    val søknadstidspunkt: LocalDate,
)

fun RegistrertSøknadstidspunktPåPerson.tilRegistrertSøknadstidspunktPåPersonDto() =
    RegistrertSøknadstidspunktPåPersonDto(
        personIdent = aktør.aktivFødselsnummer(),
        søknadstidspunkt = søknadstidspunkt,
    )

fun RegistrertSøknadstidspunktPåPersonDto.tilRegistrertSøknadstidspunkt() =
    RegistrertSøknadstidspunkt(
        personIdent = personIdent,
        søknadstidspunkt = søknadstidspunkt,
    )
