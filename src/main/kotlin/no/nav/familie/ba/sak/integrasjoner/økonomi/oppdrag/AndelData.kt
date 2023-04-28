package no.nav.familie.ba.sak.integrasjoner.økonomi.oppdrag

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import java.time.YearMonth

data class AndelData(
    val id: Long,
    val fom: YearMonth,
    val tom: YearMonth,
    val beløp: Int,
    val ident: String,
    val type: YtelseType,
    val offset: Long?,
    val forrigeOffset: Long?,
    val kildeBehandlingId: Long?
) {
    constructor(andel: AndelTilkjentYtelse) : this(
        id = andel.id,
        fom = andel.stønadFom,
        tom = andel.stønadTom,
        beløp = andel.kalkulertUtbetalingsbeløp,
        ident = andel.aktør.aktivFødselsnummer(),
        type = andel.type,
        offset = andel.periodeOffset,
        forrigeOffset = andel.forrigePeriodeOffset,
        kildeBehandlingId = andel.kildeBehandlingId
    )
}
