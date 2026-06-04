package no.nav.familie.ba.sak.kjerne.registrertsøknadstidspunkt

import java.time.LocalDate

data class RegistrertSøknadstidspunkt(
    val personIdent: String,
    val søknadstidspunkt: LocalDate,
)
