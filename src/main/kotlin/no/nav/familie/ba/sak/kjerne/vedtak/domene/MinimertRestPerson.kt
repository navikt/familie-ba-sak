package no.nav.familie.ba.sak.kjerne.vedtak.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.restDomene.RestPerson
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import java.time.LocalDate

/**
 * NB: Bør ikke brukes internt, men kun ut mot eksterne tjenester siden klassen
 * inneholder aktiv personIdent og ikke aktørId.
 */
data class MinimertRestPerson(
    val personIdent: String,
    val fødselsdato: LocalDate,
    val type: PersonType,
)

fun RestPerson.tilMinimertPerson() =
    MinimertRestPerson(
        personIdent = this.personIdent,
        fødselsdato = fødselsdato ?: throw Feil("Fødselsdato mangler"),
        type = this.type,
    )
