package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import java.math.BigDecimal

fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.erOppdatertMedSisteSatser(personOpplysningGrunnlag: PersonopplysningGrunnlag): Boolean =
    SatsType.entries
        .filter { it != SatsType.FINN_SVAL }
        .all {
            this.erOppdatertFor(
                personOpplysningGrunnlag = personOpplysningGrunnlag,
                satstype = it,
            )
        }

private fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.erOppdatertFor(
    personOpplysningGrunnlag: PersonopplysningGrunnlag,
    satstype: SatsType,
): Boolean {
    val sisteSatsForSatstype = SatsService.finnSisteSatsFor(satstype)
    val fomSisteSatsForSatstype = sisteSatsForSatstype.gyldigFom.toYearMonth()

    val satsTyperMedTilsvarendeYtelsestype =
        satstype
            .tilYtelseType()
            .hentSatsTyper()

    return this.filter { it.stønadTom.isSameOrAfter(fomSisteSatsForSatstype) }
        .filter { andel ->
            val person = personOpplysningGrunnlag.personer.single { it.aktør == andel.aktør }
            val andelSatsType = andel.type.tilSatsType(person, andel.stønadFom.førsteDagIInneværendeMåned())

            andelSatsType == satstype
        }
        .filter { it.prosent != BigDecimal.ZERO }
        .all { andelTilkjentYtelse ->
            satsTyperMedTilsvarendeYtelsestype
                .any { andelTilkjentYtelse.sats == SatsService.finnSisteSatsFor(it).beløp }
        }
}
