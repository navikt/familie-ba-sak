package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson
import java.time.LocalDate

class MinimertPerson(
    val type: PersonType,
    val fødselsdato: LocalDate,
    val aktørId: String,
    private val aktivPersonIdent: String,
) {
    fun hentSeksårsdag(): LocalDate = fødselsdato.plusYears(6)

    fun tilMinimertRestPerson() = MinimertRestPerson(
        personIdent = aktivPersonIdent,
        fødselsdato = fødselsdato,
        type = type
    )
}

fun PersonopplysningGrunnlag.tilMinimertePersoner(): List<MinimertPerson> =
    this.søkerOgBarn.map {
        MinimertPerson(
            it.type,
            it.fødselsdato,
            it.aktør.aktørId,
            it.aktør.aktivFødselsnummer()
        )
    }

fun List<MinimertPerson>.harBarnMedSeksårsdagPåFom(fom: LocalDate?) = this.any { person ->
    person
        .hentSeksårsdag()
        .toYearMonth() == (fom?.toYearMonth() ?: TIDENES_ENDE.toYearMonth())
}
