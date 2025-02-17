package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.integrasjoner.økonomi.sats
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

fun lagAndelTilkjentYtelse(
    fom: YearMonth,
    tom: YearMonth,
    ytelseType: YtelseType = YtelseType.ORDINÆR_BARNETRYGD,
    beløp: Int = sats(ytelseType),
    behandling: Behandling = lagBehandling(),
    person: Person = tilfeldigPerson(),
    aktør: Aktør = person.aktør,
    periodeIdOffset: Long? = null,
    forrigeperiodeIdOffset: Long? = null,
    tilkjentYtelse: TilkjentYtelse? = null,
    prosent: BigDecimal = BigDecimal(100),
    kildeBehandlingId: Long? = behandling.id,
    differanseberegnetPeriodebeløp: Int? = null,
    id: Long = 0,
    sats: Int = sats(ytelseType),
    kalkulertUtbetalingsbeløp: Int? = null,
    nasjonaltPeriodebeløp: Int = beløp,
): AndelTilkjentYtelse =
    AndelTilkjentYtelse(
        id = id,
        aktør = aktør,
        behandlingId = behandling.id,
        tilkjentYtelse = tilkjentYtelse ?: lagInitiellTilkjentYtelse(behandling),
        kalkulertUtbetalingsbeløp = if (kalkulertUtbetalingsbeløp == null) beløp else kalkulertUtbetalingsbeløp,
        nasjonaltPeriodebeløp = nasjonaltPeriodebeløp,
        stønadFom = fom,
        stønadTom = tom,
        type = ytelseType,
        periodeOffset = periodeIdOffset,
        forrigePeriodeOffset = forrigeperiodeIdOffset,
        sats = sats,
        prosent = prosent,
        kildeBehandlingId = kildeBehandlingId,
        differanseberegnetPeriodebeløp = differanseberegnetPeriodebeløp,
    )

fun lagAndelTilkjentYtelseMedEndreteUtbetalinger(
    fom: YearMonth,
    tom: YearMonth,
    ytelseType: YtelseType = YtelseType.ORDINÆR_BARNETRYGD,
    beløp: Int = sats(ytelseType),
    behandling: Behandling = lagBehandling(),
    person: Person = tilfeldigPerson(),
    aktør: Aktør = person.aktør,
    periodeIdOffset: Long? = null,
    forrigeperiodeIdOffset: Long? = null,
    tilkjentYtelse: TilkjentYtelse? = null,
    prosent: BigDecimal = BigDecimal(100),
    endretUtbetalingAndeler: List<EndretUtbetalingAndel> = emptyList(),
    differanseberegnetPeriodebeløp: Int? = null,
    sats: Int = beløp,
): AndelTilkjentYtelseMedEndreteUtbetalinger {
    val aty =
        AndelTilkjentYtelse(
            aktør = aktør,
            behandlingId = behandling.id,
            tilkjentYtelse = tilkjentYtelse ?: lagInitiellTilkjentYtelse(behandling),
            kalkulertUtbetalingsbeløp = beløp,
            nasjonaltPeriodebeløp = beløp,
            stønadFom = fom,
            stønadTom = tom,
            type = ytelseType,
            periodeOffset = periodeIdOffset,
            forrigePeriodeOffset = forrigeperiodeIdOffset,
            sats = sats,
            prosent = prosent,
            differanseberegnetPeriodebeløp = differanseberegnetPeriodebeløp,
        )

    return AndelTilkjentYtelseMedEndreteUtbetalinger(aty, endretUtbetalingAndeler)
}

fun lagAndelTilkjentYtelseUtvidet(
    fom: String,
    tom: String,
    ytelseType: YtelseType,
    beløp: Int = sats(ytelseType),
    behandling: Behandling = lagBehandling(),
    person: Person = tilfeldigSøker(),
    periodeIdOffset: Long? = null,
    forrigeperiodeIdOffset: Long? = null,
    tilkjentYtelse: TilkjentYtelse? = null,
): AndelTilkjentYtelse =
    AndelTilkjentYtelse(
        aktør = person.aktør,
        behandlingId = behandling.id,
        tilkjentYtelse = tilkjentYtelse ?: lagInitiellTilkjentYtelse(behandling),
        kalkulertUtbetalingsbeløp = beløp,
        nasjonaltPeriodebeløp = beløp,
        stønadFom = årMnd(fom),
        stønadTom = årMnd(tom),
        type = ytelseType,
        periodeOffset = periodeIdOffset,
        forrigePeriodeOffset = forrigeperiodeIdOffset,
        sats = beløp,
        prosent = BigDecimal(100),
    )

fun lagTilkjentYtelse(
    behandling: Behandling = lagBehandling(),
    stønadFom: YearMonth? = YearMonth.now(),
    stønadTom: YearMonth? = YearMonth.now(),
    opphørFom: YearMonth? = YearMonth.now(),
    opprettetDato: LocalDate = LocalDate.now(),
    endretDato: LocalDate = LocalDate.now(),
    utbetalingsoppdrag: String? = null,
    lagAndelerTilkjentYtelse: (tilkjentYtelse: TilkjentYtelse) -> Set<AndelTilkjentYtelse> = {
        emptySet()
    },
): TilkjentYtelse {
    val andelerTilkjentYtelse = mutableSetOf<AndelTilkjentYtelse>()
    val tilkjentYtelse =
        TilkjentYtelse(
            behandling = behandling,
            stønadFom = stønadFom,
            stønadTom = stønadTom,
            opphørFom = opphørFom,
            opprettetDato = opprettetDato,
            endretDato = endretDato,
            utbetalingsoppdrag = utbetalingsoppdrag,
            andelerTilkjentYtelse = andelerTilkjentYtelse,
        )
    tilkjentYtelse.andelerTilkjentYtelse.addAll(lagAndelerTilkjentYtelse(tilkjentYtelse))
    return tilkjentYtelse
}

fun lagInitiellTilkjentYtelse(
    behandling: Behandling = lagBehandling(),
    utbetalingsoppdrag: String? = null,
): TilkjentYtelse =
    TilkjentYtelse(
        behandling = behandling,
        opprettetDato = LocalDate.now(),
        endretDato = LocalDate.now(),
        utbetalingsoppdrag = utbetalingsoppdrag,
    )
