package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

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
            this.erOppdatertForSats(
                personOpplysningGrunnlag = personOpplysningGrunnlag,
                satstype = it,
            )
        }

private fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.erOppdatertForSats(
    personOpplysningGrunnlag: PersonopplysningGrunnlag,
    satstype: SatsType,
): Boolean {
    val sisteSatsForSatstype = SatsService.finnSisteSatsFor(satstype)
    val fomSisteSatsForSatstype = sisteSatsForSatstype.gyldigFom.toYearMonth()

    return this
        .filter { it.stønadTom.isSameOrAfter(fomSisteSatsForSatstype) }
        .filter { andel ->
            val person = personOpplysningGrunnlag.personer.single { it.aktør == andel.aktør }
            // Bruker stønadTom siden

            val andelSatsTyper = andel.type.tilSatsType(person, andel.stønadFom, andel.stønadTom)

            andelSatsTyper.contains(satstype)
        }.filter { it.prosent != BigDecimal.ZERO }
        .all { andelTilkjentYtelse -> andelTilkjentYtelse.sats == SatsService.finnSisteSatsFor(satstype).beløp }
}
