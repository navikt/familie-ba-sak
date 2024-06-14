package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.tilRestYtelsePerioder
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import java.time.LocalDate
import java.time.YearMonth

data class RestPersonMedAndeler(
    val personIdent: String?,
    val beløp: Int,
    val stønadFom: YearMonth,
    val stønadTom: YearMonth,
    val ytelsePerioder: List<RestYtelsePeriode>,
)

data class RestYtelsePeriode(
    val beløp: Int,
    val stønadFom: YearMonth,
    val stønadTom: YearMonth,
    val ytelseType: YtelseType,
    val skalUtbetales: Boolean,
)

fun PersonopplysningGrunnlag.tilRestPersonerMedAndeler(andelerKnyttetTilPersoner: List<AndelTilkjentYtelse>): List<RestPersonMedAndeler> =
    andelerKnyttetTilPersoner
        .groupBy { it.aktør }
        .map { andelerForPerson ->
            val personId = andelerForPerson.key
            val andeler = andelerForPerson.value

            val ytelsePerioder = andeler.tilRestYtelsePerioder()

            RestPersonMedAndeler(
                personIdent =
                    this.søkerOgBarn
                        .find { person -> person.aktør == personId }
                        ?.aktør
                        ?.aktivFødselsnummer(),
                beløp = ytelsePerioder.sumOf { it.beløp },
                stønadFom = ytelsePerioder.minOfOrNull { it.stønadFom } ?: LocalDate.MIN.toYearMonth(),
                stønadTom = ytelsePerioder.maxOfOrNull { it.stønadTom } ?: LocalDate.MAX.toYearMonth(),
                ytelsePerioder = ytelsePerioder,
            )
        }
