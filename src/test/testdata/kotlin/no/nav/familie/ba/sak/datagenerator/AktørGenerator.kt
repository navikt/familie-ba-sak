package no.nav.familie.ba.sak.datagenerator

import no.nav.commons.foedselsnummer.testutils.FoedselsnummerGenerator
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.Personident
import java.time.LocalDate
import kotlin.random.Random

private val fødselsnummerGenerator = FoedselsnummerGenerator()

fun randomFnr(foedselsdato: LocalDate? = null): String =
    fødselsnummerGenerator
        .foedselsnummer(
            foedselsdato ?: LocalDate.now().minusYears((20..70).random().toLong()).minusDays((1..364).random().toLong()),
        ).asString

fun randomBarnFnr(alder: Int? = null): String =
    randomFnr(
        LocalDate.now().minusYears((alder ?: (1..16).random()).toLong()).minusDays((1..364).random().toLong()),
    )

fun randomAktør(fnr: String = randomFnr()): Aktør =
    Aktør(Random.nextLong(1000_000_000_000, 31_121_299_99999).toString()).also {
        it.personidenter.add(
            Personident(fødselsnummer = fnr, aktør = it),
        )
    }
