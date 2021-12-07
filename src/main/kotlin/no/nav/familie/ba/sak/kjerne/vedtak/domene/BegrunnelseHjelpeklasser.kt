package no.nav.familie.ba.sak.kjerne.vedtak.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.ekstern.restDomene.RestPerson
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import java.time.LocalDate

data class BegrunnelsePerson(
    val personIdent: String,
    val fødselsdato: LocalDate,
    val type: PersonType
) {
    fun hentSeksårsdag(): LocalDate = fødselsdato.plusYears(6)
}

fun List<BegrunnelsePerson>.barnMedSeksårsdagPåFom(fom: LocalDate?): List<BegrunnelsePerson> {
    return this
        .filter { it.type == PersonType.BARN }
        .filter { person ->
            person.hentSeksårsdag().toYearMonth() == (
                fom?.toYearMonth()
                    ?: TIDENES_ENDE.toYearMonth()
                )
        }
}

fun List<BegrunnelsePerson>.harBarnMedSeksårsdagPåFom(fom: LocalDate?) =
    this.any { person ->
        person
            .hentSeksårsdag()
            .toYearMonth() == (fom?.toYearMonth() ?: TIDENES_ENDE.toYearMonth())
    }

fun List<BegrunnelsePerson>.hentSøker() =
    this.firstOrNull { it.type == PersonType.SØKER }
        ?: throw Feil("Fant ikke søker blant begrunnelsepersonene")

fun Person.tilBegrunnelsePerson() = BegrunnelsePerson(
    personIdent = this.aktør.aktivIdent().fødselsnummer,
    fødselsdato = this.fødselsdato,
    type = this.type
)

fun List<BegrunnelsePerson>.tilBarnasFødselsdatoer(): String =
    Utils.slåSammen(
        this
            .filter { it.type == PersonType.BARN }
            .sortedBy { person ->
                person.fødselsdato
            }
            .map { person ->
                person.fødselsdato.tilKortString() ?: ""
            }
    )

fun BegrunnelsePerson.tilRestPersonTilTester() = RestPerson(
    personIdent = this.personIdent,
    fødselsdato = this.fødselsdato,
    type = this.type,
    navn = "Mock Mockersen",
    kjønn = Kjønn.KVINNE,
    målform = Målform.NB
)
