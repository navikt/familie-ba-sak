package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.overlapperHeltEllerDelvisMed
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.personident.dummyAktør
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import java.time.YearMonth

object YtelsePersonUtils {

    /**
     * Utleder hvilke konsekvenser _denne_ behandlingen har for personen og populerer "resultater" med utfallet.
     *
     * @param [behandlingsresultatPersoner] Personer som er vurdert i behandlingen med metadata
     * @param [uregistrerteBarn] Barn det er søkt for som ikke er folkeregistrert
     * @return Personer populert med utfall (resultater) etter denne behandlingen
     */
    fun utledYtelsePersonerMedResultat(
        behandlingsresultatPersoner: List<BehandlingsresultatPerson>,
        uregistrerteBarn: List<MinimertUregistrertBarn> = emptyList(),
        inneværendeMåned: YearMonth = YearMonth.now(),
    ): List<YtelsePerson> {
        val altOpphørt = behandlingsresultatPersoner.all { erYtelsenOpphørt(it.andeler, inneværendeMåned) }

        return behandlingsresultatPersoner.map { behandlingsresultatPerson ->
            val forrigeAndelerTidslinje = behandlingsresultatPerson.forrigeAndeler.tilTidslinje()
            val andelerTidslinje = behandlingsresultatPerson.andeler.tilTidslinje()

            val segmenterLagtTil = andelerTidslinje.disjoint(forrigeAndelerTidslinje)
            val segmenterFjernet = forrigeAndelerTidslinje.disjoint(andelerTidslinje)
            val harSammeTidslinje = !andelerTidslinje.isEmpty &&
                !forrigeAndelerTidslinje.isEmpty &&
                andelerTidslinje == forrigeAndelerTidslinje

            val eksplisittAvslag = behandlingsresultatPerson.eksplisittAvslag

            val resultater = mutableSetOf<YtelsePersonResultat>()
            val ytelsePerson = behandlingsresultatPerson.utledYtelsePerson()

            if (eksplisittAvslag || avslagPåNyPerson(
                    personSomSjekkes = ytelsePerson,
                    segmenterLagtTil = segmenterLagtTil
                )
            ) {
                resultater.add(YtelsePersonResultat.AVSLÅTT)
            }

            if (erYtelsenOpphørt(andeler = behandlingsresultatPerson.andeler, inneværendeMåned = inneværendeMåned)) {
                when {
                    // ytelsen er opphørt ved dødsfall. Da kan RV har samme tidstilnje men alle ytelesene er opphørt
                    harSammeTidslinje && altOpphørt -> resultater.add(YtelsePersonResultat.FORTSATT_OPPHØRT)
                    (segmenterFjernet + segmenterLagtTil).isNotEmpty() -> resultater.add(YtelsePersonResultat.OPPHØRT)
                }
            }

            if (finnesInnvilget(
                    behandlingsresultatPerson = behandlingsresultatPerson,
                    segmenterLagtTil = segmenterLagtTil
                )
            ) {
                resultater.add(YtelsePersonResultat.INNVILGET)
            }

            val ytelseSlutt: YearMonth? = if (behandlingsresultatPerson.andeler.isNotEmpty())
                behandlingsresultatPerson.andeler.maxByOrNull { it.stønadTom }?.stønadTom
                    ?: throw Feil("Finnes andel uten tom-dato") else TIDENES_MORGEN.toYearMonth()

            utledYtelsePersonResultatVedEndring(
                behandlingsresultatPerson,
                segmenterLagtTil,
                segmenterFjernet,
                ytelsePerson,
                inneværendeMåned
            )?.let { resultater.add(it) }

            ytelsePerson.copy(
                resultater = resultater.toSet(),
                ytelseSlutt = ytelseSlutt
            )
        } + uregistrerteBarn.map {
            YtelsePerson(
                aktør = dummyAktør,
                resultater = setOf(YtelsePersonResultat.AVSLÅTT),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                ytelseSlutt = TIDENES_MORGEN.toYearMonth(),
                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE)
            )
        }
    }

    private fun List<BehandlingsresultatAndelTilkjentYtelse>.tilTidslinje():
        LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse> =
        LocalDateTimeline(
            map {
                LocalDateSegment(
                    it.stønadFom.førsteDagIInneværendeMåned(),
                    it.stønadTom.sisteDagIInneværendeMåned(),
                    it
                )
            }
        )

    // metoden utleder YtelsesPersonResultat om det finnes en endring ellers sender null
    private fun utledYtelsePersonResultatVedEndring(
        behandlingsresultatPerson: BehandlingsresultatPerson,
        segmenterLagtTil: LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>,
        segmenterFjernet: LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>,
        ytelsePerson: YtelsePerson,
        inneværendeMåned: YearMonth
    ): YtelsePersonResultat? {
        val nesteMåned = inneværendeMåned.nesteMåned()
        val stønadSlutt = behandlingsresultatPerson.andeler.maxByOrNull { it.stønadFom }?.stønadTom
            ?: TIDENES_MORGEN.toYearMonth()

        val forrigeStønadSlutt = behandlingsresultatPerson.forrigeAndeler
            .maxByOrNull { it.stønadFom }?.stønadTom ?: TIDENES_MORGEN.toYearMonth()

        val opphører = stønadSlutt.isBefore(nesteMåned)

        return if (behandlingsresultatPerson.søktForPerson) {
            val beløpRedusert = (segmenterLagtTil + segmenterFjernet).isEmpty() &&
                (
                behandlingsresultatPerson.forrigeAndeler.sumOf { it.sumForPeriode() } -
                    behandlingsresultatPerson.andeler.sumOf { it.sumForPeriode() }
                ) > 0

            val finnesReduksjonerTilbakeITid = ytelsePerson.erFramstiltKravForITidligereBehandling() &&
                segmenterFjernet.harSegmentFør(inneværendeMåned)

            val finnesReduksjonerTilbakeITidMedBeløp = finnesReduksjonerTilbakeITid &&
                segmenterFjernet.any { it.value.kalkulertUtbetalingsbeløp > 0 }

            when {
                opphører -> null
                beløpRedusert || finnesReduksjonerTilbakeITidMedBeløp -> YtelsePersonResultat.ENDRET_UTBETALING
                finnesReduksjonerTilbakeITid -> YtelsePersonResultat.ENDRET_UTEN_UTBETALING
                else -> null
            }
            // Hvis det ikke finnes noen forrige andeler kan det aldri bety endring
        } else if (behandlingsresultatPerson.forrigeAndeler.isNotEmpty()) {
            val erAndelMedEndretBeløp = erAndelMedEndretBeløp(
                forrigeAndeler = behandlingsresultatPerson.forrigeAndeler,
                andeler = behandlingsresultatPerson.andeler
            )

            val erLagtTilSegmenter = ytelsePerson.erFramstiltKravForITidligereBehandling() &&
                segmenterLagtTil.harSegmentFør(if (opphører) stønadSlutt else nesteMåned)

            val erLagtTilSegmenterMedEndringIUtbetaling = erLagtTilSegmenter &&
                segmenterLagtTil.any { it.value.kalkulertUtbetalingsbeløp > 0 }

            val erFjernetSegmenter = segmenterFjernet.harSegmentFør(if (opphører) stønadSlutt else nesteMåned)

            val erFjernetSegmenterMedEndringIUtbetaling = erFjernetSegmenter &&
                segmenterFjernet.any { it.value.kalkulertUtbetalingsbeløp > 0 }

            val opphørsdatoErSattSenere = stønadSlutt.isAfter(forrigeStønadSlutt)

            when {
                erAndelMedEndretBeløp ||
                    erLagtTilSegmenterMedEndringIUtbetaling ||
                    erFjernetSegmenterMedEndringIUtbetaling ||
                    opphørsdatoErSattSenere -> YtelsePersonResultat.ENDRET_UTBETALING
                erLagtTilSegmenter || erFjernetSegmenter -> YtelsePersonResultat.ENDRET_UTEN_UTBETALING
                else -> null
            }
        } else null
    }

    fun erAndelMedEndretBeløp(
        forrigeAndeler: List<BehandlingsresultatAndelTilkjentYtelse>,
        andeler: List<BehandlingsresultatAndelTilkjentYtelse>
    ): Boolean =
        andelerMedEndretBeløp(
            forrigeAndeler = forrigeAndeler,
            andeler = andeler
        ).isNotEmpty()

    private fun andelerMedEndretBeløp(
        forrigeAndeler: List<BehandlingsresultatAndelTilkjentYtelse>,
        andeler: List<BehandlingsresultatAndelTilkjentYtelse>
    ): List<Int> = andeler.flatMap { andel ->
        val andelerFraForrigeBehandlingISammePeriode =
            forrigeAndeler.filter { it.periode.overlapperHeltEllerDelvisMed(andel.periode) }

        andelerFraForrigeBehandlingISammePeriode.map {
            andel.kalkulertUtbetalingsbeløp - it.kalkulertUtbetalingsbeløp
        }.filter { it != 0 }
    }

    private fun avslagPåNyPerson(
        personSomSjekkes: YtelsePerson,
        segmenterLagtTil: LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>
    ) =
        personSomSjekkes.kravOpprinnelse == listOf(KravOpprinnelse.INNEVÆRENDE) && segmenterLagtTil.isEmpty

    private fun finnesInnvilget(
        behandlingsresultatPerson: BehandlingsresultatPerson,
        segmenterLagtTil: LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>
    ) =
        behandlingsresultatPerson.utledYtelsePerson()
            .erFramstiltKravForIInneværendeBehandling() && (
            !segmenterLagtTil.isEmpty || andelerMedEndretBeløp(
                forrigeAndeler = behandlingsresultatPerson.forrigeAndeler,
                andeler = behandlingsresultatPerson.andeler
            ).any { it > 0 }
            )

    private fun erYtelsenOpphørt(
        andeler: List<BehandlingsresultatAndelTilkjentYtelse>,
        inneværendeMåned: YearMonth
    ) = andeler.none { it.erLøpende(inneværendeMåned) }

    private fun LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>.harSegmentFør(
        måned: YearMonth
    ) = !this.isEmpty && (
        this.any { it.tom < måned.sisteDagIInneværendeMåned() } ||
            this.any { it.fom < måned.sisteDagIInneværendeMåned() }
        )
}
