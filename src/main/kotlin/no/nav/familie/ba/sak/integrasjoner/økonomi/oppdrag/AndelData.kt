package no.nav.familie.ba.sak.integrasjoner.økonomi.oppdrag

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
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

data class Behandlingsinformasjon(
    val saksbehandlerId: String,
    val behandlingId: Long,
    val fagsakId: Long,
    val aktør: Aktør,
    val vedtak: Vedtak
)

data class AndelMedOffset(
    val id: Long,
    val offset: Long,
    val forrigeOffset: Long?,
    val kildeBehandlingId: Long
) {
    constructor(andel: AndelData, nyttKildeBehandlingId: Long? = null) :
        this(
            id = andel.id,
            offset = andel.offset ?: error("Mangler offset på andel=${andel.id}"),
            forrigeOffset = andel.forrigeOffset,
            kildeBehandlingId = nyttKildeBehandlingId ?: andel.kildeBehandlingId
            ?: error("Mangler kildebehandlingId på andel=${andel.id}")
        )
}

data class UtbetalingsoppdragOgAndelerMedOffset(
    val utbetalingsoppdrag: Utbetalingsoppdrag,
    val andeler: List<AndelMedOffset>
)

data class IdentOgType(
    val ident: String,
    val type: YtelseType
)

data class ResultatForKjede(
    val beståendeAndeler: List<AndelData>,
    val nyeAndeler: List<AndelData>,
    val opphørsandel: Pair<AndelData, YearMonth>?,
    val sisteOffset: Long,
)