package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIUtbetalingUtil
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringUtil.tilFørsteEndringstidspunkt
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjær
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class UtbetalingsgeneratorV2 {

    fun lagUtbetalingsoppdrag(vedtak: Vedtak, saksbehandlerId: String, erFørsteIverksatteBehandlingPåFagsak: Boolean): Utbetalingsoppdrag {
        // Finner og grupperer andeler tilkjent ytelse fra forrige behandling per aktør og type
        val forrigeAndelerPerAktørOgType = emptyMap<Aktør, Map<YtelseType, List<AndelTilkjentYtelse>>>()

        // Finner og grupperer andeler tilkjent ytelse fra forrige behandling per aktør og type
        val nåværendeAndelerPerAktørOgType = emptyMap<Aktør, Map<YtelseType, List<AndelTilkjentYtelse>>>()

        val allePersoner = (forrigeAndelerPerAktørOgType.keys + nåværendeAndelerPerAktørOgType.keys).distinct()

        val høyesteOffsetPåFagsak = finnHøyesteOffsetPåFagsak()
        var nesteOffsetPåFagsak = if (høyesteOffsetPåFagsak == null) 0 else høyesteOffsetPåFagsak + 1

        // For alle kjeder, finner offsets til ny behandling og lager utbetalingsoppdrag
        val utbetalingsperioder = allePersoner.flatMap { aktør ->
            val forrigeYtelserMedAndelerForAktør = forrigeAndelerPerAktørOgType[aktør] ?: emptyMap()
            val nåværendeYtelserMedAndelerForAktør = nåværendeAndelerPerAktørOgType[aktør] ?: emptyMap()
            val alleTyperForAktør = (forrigeYtelserMedAndelerForAktør.keys + nåværendeYtelserMedAndelerForAktør.keys).distinct()

            alleTyperForAktør.flatMap { ytelseType ->
                val forrigeAndelerForAktørOgType = forrigeYtelserMedAndelerForAktør[ytelseType] ?: emptyList()
                val nåværendeAndelerForAktørOgType = nåværendeYtelserMedAndelerForAktør[ytelseType] ?: emptyList()

                val sisteAndelIKjede = finnSisteAndelIKjede()

                val andelerMedOffset = genererAndelerMedOffsetPerAktørOgType(
                    forrigeAndeler = forrigeAndelerForAktørOgType,
                    nåværendeAndeler = nåværendeAndelerForAktørOgType,
                    nesteOffsetPåFagsak = nesteOffsetPåFagsak,
                    sisteOffsetIKjede = sisteAndelIKjede?.periodeOffset
                )

                nesteOffsetPåFagsak = maxOf(nesteOffsetPåFagsak, andelerMedOffset.maxOfOrNull { it.periodeOffset!! } ?: 0)
                // Finner første endringstidspunkt
                val førsteEndringstidspunkt = EndringIUtbetalingUtil.lagEndringIUtbetalingForPersonOgTypeTidslinje(
                    nåværendeAndeler = nåværendeAndelerForAktørOgType,
                    forrigeAndeler = forrigeAndelerForAktørOgType
                ).tilFørsteEndringstidspunkt() ?: throw Feil("Kan ikke ha kommet hit uten endring?")

                lagUtbetalingsperioderForAktørOgTypeKjede(
                    nåværendeAndelerMedOffset = andelerMedOffset,
                    endringstidspunkt = førsteEndringstidspunkt,
                    vedtak = vedtak,
                    sisteAndelIKjede = sisteAndelIKjede
                )
            }
        }

        return Utbetalingsoppdrag(
            saksbehandlerId = saksbehandlerId,
            kodeEndring = if (erFørsteIverksatteBehandlingPåFagsak) Utbetalingsoppdrag.KodeEndring.NY else Utbetalingsoppdrag.KodeEndring.ENDR,
            fagSystem = FAGSYSTEM,
            saksnummer = vedtak.behandling.fagsak.id.toString(),
            aktoer = vedtak.behandling.fagsak.aktør.aktivFødselsnummer(),
            utbetalingsperiode = utbetalingsperioder
        )
    }

    private data class AndelMedOffset(
        val andelId: Long,
        val kalkulertUtbetalingsbeløp: Int,
        val stønadFom: YearMonth,
        val stønadTom: YearMonth,
        val ytelseType: YtelseType,
        val periodeOffset: Long?, // vil ikke ha nullable
        val forrigePeriodeOffset: Long?,
        val kildeBehandlingId: Long? // vil ikke ha nullable
    )

    private fun genererAndelerMedOffsetPerAktørOgType(
        forrigeAndeler: List<AndelTilkjentYtelse>,
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        nesteOffsetPåFagsak: Long,
        sisteOffsetIKjede: Long?
    ): List<AndelMedOffset> {
        val forrigeTidslinje = AndelTilkjentYtelseTidslinje(forrigeAndeler)
        val nåværendeTidslinje = AndelTilkjentYtelseTidslinje(nåværendeAndeler)

        var nesteOffsetTeller = nesteOffsetPåFagsak
        var sisteOffsetIKjedeTeller = sisteOffsetIKjede

        // TODO: denne er jeg ikke sikker på om stemmer helt
        val andelerMedOffsetTidslinje = forrigeTidslinje.kombinerMed(nåværendeTidslinje) { forrige, nåværende ->
            when {
                forrige == null && nåværende != null -> {
                    // Er ny andel hvis det ikke fantes andel i perioden før (og skal da ha ny offset)
                    val andel = AndelMedOffset(
                        andelId = nåværende.id,
                        kalkulertUtbetalingsbeløp = nåværende.kalkulertUtbetalingsbeløp,
                        stønadFom = nåværende.stønadFom,
                        stønadTom = nåværende.stønadTom,
                        ytelseType = nåværende.type,
                        periodeOffset = nesteOffsetTeller,
                        forrigePeriodeOffset = sisteOffsetIKjedeTeller,
                        kildeBehandlingId = nåværende.behandlingId
                    )

                    nesteOffsetTeller = andel.periodeOffset!! + 1
                    sisteOffsetIKjedeTeller = andel.periodeOffset

                    andel
                }
                forrige != null && nåværende == null -> null
                forrige != null && nåværende != null ->
                    // Hvis beløp er likt nå som før, og den er knyttet til andel som startet likt som sist så bevarer vi offset fra forrige gang
                    if (forrige.stønadFom == nåværende.stønadFom && forrige.kalkulertUtbetalingsbeløp == nåværende.kalkulertUtbetalingsbeløp) {
                        AndelMedOffset(
                            andelId = nåværende.id,
                            kalkulertUtbetalingsbeløp = nåværende.kalkulertUtbetalingsbeløp,
                            stønadFom = nåværende.stønadFom,
                            stønadTom = nåværende.stønadTom,
                            ytelseType = nåværende.type,
                            periodeOffset = forrige.periodeOffset,
                            forrigePeriodeOffset = forrige.forrigePeriodeOffset,
                            kildeBehandlingId = forrige.kildeBehandlingId
                        )
                    } else {
                        // Hvis beløpet ikke er likt ELLER beløpet er likt, men de er ikke knyttet til andel som startet samtidig så er andelen ny (og skal da ha ny offset)
                        // TODO: Kan nok skrive om den del her, koden her er lik som over
                        val andel = AndelMedOffset(
                            andelId = nåværende.id,
                            kalkulertUtbetalingsbeløp = nåværende.kalkulertUtbetalingsbeløp,
                            stønadFom = nåværende.stønadFom,
                            stønadTom = nåværende.stønadTom,
                            ytelseType = nåværende.type,
                            periodeOffset = nesteOffsetTeller,
                            forrigePeriodeOffset = sisteOffsetIKjedeTeller,
                            kildeBehandlingId = nåværende.behandlingId
                        )

                        nesteOffsetTeller = andel.periodeOffset!! + 1
                        sisteOffsetIKjedeTeller = andel.periodeOffset

                        andel
                    }
                else -> throw Feil("SKal ikke komme hit")
            }
        }

        return andelerMedOffsetTidslinje.perioder().mapNotNull {
            it.innhold
        }
    }
    private fun lagUtbetalingsperioderForAktørOgTypeKjede(
        nåværendeAndelerMedOffset: List<AndelMedOffset>,
        endringstidspunkt: YearMonth,
        vedtak: Vedtak,
        sisteAndelIKjede: AndelMedOffset?
    ): List<Utbetalingsperiode> {
        // Finner utbetalinger etter endringstidspunktet
        val utbetalingerEtterEndringstidspunktTidslinje = tidslinje {
            nåværendeAndelerMedOffset.map {
                Periode(
                    fraOgMed = it.stønadFom.tilTidspunkt(),
                    tilOgMed = it.stønadTom.tilTidspunkt(),
                    innhold = it
                )
            }
        }.beskjær(fraOgMed = endringstidspunkt.tilTidspunkt(), tilOgMed = TIDENES_ENDE.tilMånedTidspunkt())

        // Sjekker om det første som skjer etter endringstidspunkt er opphør
        val startPåUtbetalingEtterEndringstidspunkt = utbetalingerEtterEndringstidspunktTidslinje.perioder().minOfOrNull { it.fraOgMed }?.tilYearMonth()

        val starterMedOpphør = endringstidspunkt != startPåUtbetalingEtterEndringstidspunkt

        // Lag utbetalingslinjer av utbetalingene etter endringstidspunktet
        val utbetalingerEtterEndringstidspunkt = utbetalingerEtterEndringstidspunktTidslinje.perioder().mapNotNull { it.innhold }

        val utbetalingsperioderForNyeAndeler = utbetalingerEtterEndringstidspunkt.map { it.tilUtbetalingsperiodeForNyeAndeler(vedtak) }

        // Hvis starter med opphør må vi lage opphørsperiode
        return if (starterMedOpphør) {
            if (sisteAndelIKjede == null) throw Feil("Må ha siste andel i kjede her")
            utbetalingsperioderForNyeAndeler + listOf(
                lagOpphørsperiode(
                    vedtak = vedtak,
                    opphørstidspunkt = endringstidspunkt,
                    andelSomSkalOpphøres = sisteAndelIKjede
                )
            )
        } else utbetalingsperioderForNyeAndeler
    }

    private fun finnHøyesteOffsetPåFagsak(): Long? {
        // TODO
        // Databasespørring
        return 0
    }

    private fun finnSisteAndelIKjede(): AndelMedOffset? {
        // TODO
        // Kall til databasen som vi snakket om i går?
        // Finne høyeste andel for person og type som har høyest offset og kilde_behandling_id=fk_behandling_id
        // returnerer null hvis det ikke finnes en siste andel
        return null
    }

    private fun AndelMedOffset.tilUtbetalingsperiodeForNyeAndeler(vedtak: Vedtak): Utbetalingsperiode {
        return Utbetalingsperiode(
            erEndringPåEksisterendePeriode = false,
            opphør = null,
            periodeId = this.periodeOffset!!, // TODO gjøre ikke nullable
            forrigePeriodeId = this.forrigePeriodeOffset,
            datoForVedtak = vedtak.vedtaksdato?.toLocalDate() ?: LocalDate.now(),
            klassifisering = this.ytelseType.klassifisering,
            vedtakdatoFom = this.stønadFom.førsteDagIInneværendeMåned(),
            vedtakdatoTom = this.stønadTom.sisteDagIInneværendeMåned(),
            sats = BigDecimal(this.kalkulertUtbetalingsbeløp),
            satsType = Utbetalingsperiode.SatsType.MND,
            utbetalesTil = "", // TODO
            behandlingId = vedtak.behandling.id,
            utbetalingsgrad = null
        )
    }

    private fun lagOpphørsperiode(vedtak: Vedtak, opphørstidspunkt: YearMonth, andelSomSkalOpphøres: AndelMedOffset): Utbetalingsperiode {
        return Utbetalingsperiode(
            erEndringPåEksisterendePeriode = true,
            opphør = Opphør(
                opphørDatoFom = opphørstidspunkt.førsteDagIInneværendeMåned()
            ),
            periodeId = andelSomSkalOpphøres.periodeOffset!!, // TODO gjøre ikke nullable
            forrigePeriodeId = andelSomSkalOpphøres.forrigePeriodeOffset,
            datoForVedtak = vedtak.vedtaksdato?.toLocalDate() ?: LocalDate.now(),
            klassifisering = andelSomSkalOpphøres.ytelseType.klassifisering,
            vedtakdatoFom = andelSomSkalOpphøres.stønadFom.førsteDagIInneværendeMåned(),
            vedtakdatoTom = andelSomSkalOpphøres.stønadTom.sisteDagIInneværendeMåned(),
            sats = BigDecimal(andelSomSkalOpphøres.kalkulertUtbetalingsbeløp),
            satsType = Utbetalingsperiode.SatsType.MND,
            utbetalesTil = "", // TODO
            behandlingId = vedtak.behandling.id,
            utbetalingsgrad = null
        )
    }
}
