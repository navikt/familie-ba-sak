package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.registrertsøknadstidspunkt.RegistrertSøknadstidspunkt
import java.time.LocalDate

data class EndreSøknadstidspunktRequestDto(
    val søknadstidspunktPerPerson: List<RegistrertSøknadstidspunktDto>,
)

data class RegistrertSøknadstidspunktDto(
    val personIdent: String,
    val søknadstidspunkt: LocalDate,
)

fun RegistrertSøknadstidspunkt.tilRegistrertSøknadstidspunktDto() =
    RegistrertSøknadstidspunktDto(
        personIdent = aktør.aktivFødselsnummer(),
        søknadstidspunkt = søknadstidspunkt,
    )
