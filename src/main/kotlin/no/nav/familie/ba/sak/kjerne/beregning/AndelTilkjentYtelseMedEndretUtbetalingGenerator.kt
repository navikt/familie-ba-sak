package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Utils.avrundetHeltallAvProsent
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.medEndring
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon.ZipPadding
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon.zipMedNeste
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.mapVerdi
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.math.BigDecimal

object AndelTilkjentYtelseMedEndretUtbetalingGenerator {
    fun lagAndelerMedEndretUtbetalingAndeler(
        andelTilkjentYtelserUtenEndringer: Collection<AndelTilkjentYtelse>,
        endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>,
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        if (endretUtbetalingAndeler.isEmpty()) {
            return andelTilkjentYtelserUtenEndringer
                .map { AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(it.copy()) }
        }

        val andelerPerAktørOgType = andelTilkjentYtelserUtenEndringer.groupBy { Pair(it.aktør, it.type) }
        val endringerPerAktør =
            endretUtbetalingAndeler.groupBy {
                it.person?.aktør
                    ?: throw Feil("Endret utbetaling andel ${it.endretUtbetalingAndel.id} i behandling ${tilkjentYtelse.behandling.id} er ikke knyttet til en person")
            }

        val oppdaterteAndeler =
            andelerPerAktørOgType.flatMap { (aktørOgType, andelerForAktørOgType) ->
                val aktør = aktørOgType.first
                val ytelseType = aktørOgType.second

                when (ytelseType) {
                    YtelseType.ORDINÆR_BARNETRYGD,
                    YtelseType.UTVIDET_BARNETRYGD,
                    ->
                        lagAndelerMedEndretUtbetalingAndelerForPerson(
                            andelerAvTypeForPerson = andelerForAktørOgType,
                            endretUtbetalingAndelerForPerson = endringerPerAktør.getOrDefault(aktør, emptyList()),
                            tilkjentYtelse = tilkjentYtelse,
                        )
                    YtelseType.SMÅBARNSTILLEGG ->
                        throw Feil("Småbarnstillegg kan ikke oppdateres med endret utbetaling andeler i behandling=${tilkjentYtelse.behandling.id}")
                }
            }

        return oppdaterteAndeler
    }

    private fun lagAndelerMedEndretUtbetalingAndelerForPerson(
        andelerAvTypeForPerson: List<AndelTilkjentYtelse>,
        endretUtbetalingAndelerForPerson: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>,
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        if (endretUtbetalingAndelerForPerson.isEmpty()) {
            return andelerAvTypeForPerson
                .map { AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(it.copy()) }
        }

        val andelerTidslinje = andelerAvTypeForPerson.map { it.tilPeriode() }.tilTidslinje()
        val endretUtbetalingTidslinje = endretUtbetalingAndelerForPerson.tilTidslinje()

        val andelerMedEndringerTidslinje =
            andelerTidslinje.kombinerMed(endretUtbetalingTidslinje) { andelTilkjentYtelse, endretUtbetalingAndel ->
                if (andelTilkjentYtelse == null) {
                    null
                } else {
                    val nyttBeløp =
                        if (endretUtbetalingAndel != null) {
                            andelTilkjentYtelse.sats.avrundetHeltallAvProsent(
                                endretUtbetalingAndel.prosent!!,
                            )
                        } else {
                            andelTilkjentYtelse.kalkulertUtbetalingsbeløp
                        }
                    val prosent =
                        if (endretUtbetalingAndel != null) endretUtbetalingAndel.prosent!! else andelTilkjentYtelse.prosent

                    AndelMedEndretUtbetalingForTidslinje(
                        aktør = andelTilkjentYtelse.aktør,
                        beløp = nyttBeløp,
                        sats = andelTilkjentYtelse.sats,
                        ytelseType = andelTilkjentYtelse.type,
                        prosent = prosent,
                        endretUtbetalingAndel = endretUtbetalingAndel,
                    )
                }
            }

        return andelerMedEndringerTidslinje
            .slåSammenEtterfølgende0krAndelerPgaSammeEndretAndel()
            .tilAndelerTilkjentYtelseMedEndreteUtbetalinger(tilkjentYtelse)
    }

    internal data class AndelMedEndretUtbetalingForTidslinje(
        val aktør: Aktør,
        val beløp: Int,
        val sats: Int,
        val ytelseType: YtelseType,
        val prosent: BigDecimal,
        val endretUtbetalingAndel: EndretUtbetalingAndelMedAndelerTilkjentYtelse?,
    )

    internal fun Tidslinje<AndelMedEndretUtbetalingForTidslinje>.tilAndelerTilkjentYtelseMedEndreteUtbetalinger(
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> =
        this
            .tilPerioderIkkeNull()
            .map { it.tilAndelTilkjentYtelseMedEndreteUtbetalinger(tilkjentYtelse) }

    internal fun Periode<AndelMedEndretUtbetalingForTidslinje>.tilAndelTilkjentYtelseMedEndreteUtbetalinger(
        tilkjentYtelse: TilkjentYtelse,
    ): AndelTilkjentYtelseMedEndreteUtbetalinger {
        val andelTilkjentYtelse =
            AndelTilkjentYtelse(
                behandlingId = tilkjentYtelse.behandling.id,
                tilkjentYtelse = tilkjentYtelse,
                aktør = this.verdi.aktør,
                type = this.verdi.ytelseType,
                kalkulertUtbetalingsbeløp = this.verdi.beløp,
                nasjonaltPeriodebeløp = this.verdi.beløp,
                differanseberegnetPeriodebeløp = null,
                sats = this.verdi.sats,
                prosent = this.verdi.prosent,
                stønadFom = this.fom?.toYearMonth() ?: throw Feil("Fra og med-dato ikke satt"),
                stønadTom = this.tom?.toYearMonth() ?: throw Feil("Til og med-dato ikke satt"),
            )

        val endretUtbetalingAndel = this.verdi.endretUtbetalingAndel

        return if (endretUtbetalingAndel == null) {
            AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(andelTilkjentYtelse)
        } else {
            andelTilkjentYtelse.medEndring(endretUtbetalingAndel)
        }
    }

    internal fun Tidslinje<AndelMedEndretUtbetalingForTidslinje>.slåSammenEtterfølgende0krAndelerPgaSammeEndretAndel() =
        this
            .zipMedNeste(ZipPadding.FØR)
            .mapVerdi {
                val forrigeAndelMedEndring = it?.first
                val nåværendeAndelMedEndring = it?.second

                val forrigeOgNåværendeAndelEr0kr =
                    forrigeAndelMedEndring?.prosent == BigDecimal.ZERO && nåværendeAndelMedEndring?.prosent == BigDecimal.ZERO
                val forrigeOgNåværendeAndelErPåvirketAvSammeEndring =
                    forrigeAndelMedEndring?.endretUtbetalingAndel?.endretUtbetalingAndel == nåværendeAndelMedEndring?.endretUtbetalingAndel?.endretUtbetalingAndel &&
                        nåværendeAndelMedEndring?.endretUtbetalingAndel != null

                if (forrigeOgNåværendeAndelEr0kr && forrigeOgNåværendeAndelErPåvirketAvSammeEndring) forrigeAndelMedEndring else nåværendeAndelMedEndring
            }.filtrerIkkeNull()
}
