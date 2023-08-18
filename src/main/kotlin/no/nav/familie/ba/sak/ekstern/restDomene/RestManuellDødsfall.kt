package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.kontrakter.felles.PersonIdent
import java.time.LocalDate

data class RestManuellDødsfall(
    val dødsfallDato: LocalDate,
    val personIdent: PersonIdent,
    val begrunnelse: String,
)
