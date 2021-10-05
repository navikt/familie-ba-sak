package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import java.time.YearMonth

object YtelsePersonUtils {

    /**
     * Kun støttet for førstegangsbehandlinger som er fødselshendelse og ordinær barnetrygd
     */
    fun utledKravForFødselshendelseFGB(barnIdenterFraFødselshendelse: List<String>): List<YtelsePerson> =
        barnIdenterFraFødselshendelse.map {
            YtelsePerson(
                personIdent = it,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
            )
        }

    /**
     * Utleder hvilke konsekvenser _denne_ behandlingen har for personen og populerer "resultater" med utfallet.
     *
     * @param [behandlingsresultatPersoner] Personer som er vurdert i behandlingen med metadata
     * @return Personer populert med utfall (resultater) etter denne behandlingen
     */
    fun utledYtelsePersonerMedResultat(
        behandlingsresultatPersoner: List<BehandlingsresultatPerson>,
        inneværendeMåned: YearMonth = YearMonth.now()
    ): List<YtelsePerson> {
        return behandlingsresultatPersoner.map { behandlingsresultatPerson ->
            val forrigeAndelerTidslinje = LocalDateTimeline(
                behandlingsresultatPerson.forrigeAndeler.map {
                    LocalDateSegment(
                        it.stønadFom.førsteDagIInneværendeMåned(),
                        it.stønadTom.sisteDagIInneværendeMåned(),
                        it
                    )
                }
            )
            val andelerTidslinje = LocalDateTimeline(
                behandlingsresultatPerson.andeler.map {
                    LocalDateSegment(
                        it.stønadFom.førsteDagIInneværendeMåned(),
                        it.stønadTom.sisteDagIInneværendeMåned(),
                        it
                    )
                }
            )

            val segmenterLagtTil = andelerTidslinje.disjoint(forrigeAndelerTidslinje)
            val segmenterFjernet = forrigeAndelerTidslinje.disjoint(andelerTidslinje)
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

            if (erYtelsenOpphørt(
                    andeler = behandlingsresultatPerson.andeler,
                    inneværendeMåned = inneværendeMåned
                ) && (segmenterFjernet + segmenterLagtTil).isNotEmpty()
            ) {
                resultater.add(YtelsePersonResultat.OPPHØRT)
            }

            if (finnesInnvilget(personSomSjekkes = ytelsePerson, segmenterLagtTil = segmenterLagtTil)) {
                resultater.add(YtelsePersonResultat.INNVILGET)
            }

            val ytelseSlutt: YearMonth? = if (behandlingsresultatPerson.andeler.isNotEmpty())
                behandlingsresultatPerson.andeler.maxByOrNull { it.stønadTom }?.stønadTom
                    ?: throw Feil("Finnes andel uten tom-dato") else TIDENES_MORGEN.toYearMonth()

            if (behandlingsresultatPerson.søktForPerson) {
                val beløpRedusert = (segmenterLagtTil + segmenterFjernet).isEmpty() &&
                    (behandlingsresultatPerson.forrigeAndeler.sumOf { it.kalkulertUtbetalingsbeløp } - behandlingsresultatPerson.andeler.sumOf { it.kalkulertUtbetalingsbeløp }) > 0

                val finnesReduksjonerTilbakeITid = ytelsePerson.erFramstiltKravForITidligereBehandling() &&
                    finnesEndretSegmentTilbakeITid(segmenterFjernet)

                if (finnesReduksjonerTilbakeITid || beløpRedusert) {
                    resultater.add(YtelsePersonResultat.ENDRET)
                }
            } else {
                val beløpEndret = (segmenterLagtTil + segmenterFjernet).isEmpty() &&
                    behandlingsresultatPerson.forrigeAndeler.sumOf { it.kalkulertUtbetalingsbeløp } != behandlingsresultatPerson.andeler.sumOf { it.kalkulertUtbetalingsbeløp }

                val enesteEndringErLøpendeTilOpphørt =
                    enesteEndringErLøpendeTilOpphørt(
                        segmenterLagtTil = segmenterLagtTil,
                        segmenterFjernet = segmenterFjernet,
                        sisteAndelPåPerson = behandlingsresultatPerson.andeler.maxByOrNull { it.stønadFom },
                        inneværendeMåned = inneværendeMåned
                    )

                val finnesEndringerTilbakeITid = finnesEndringTilbakeITid(
                    personSomSjekkes = ytelsePerson,
                    segmenterLagtTil = segmenterLagtTil,
                    segmenterFjernet = segmenterFjernet
                )

                val harGåttFraOpphørtTilLøpende =
                    harGåttFraOpphørtTilLøpende(
                        forrigeTilstandForPerson = behandlingsresultatPerson.forrigeAndeler,
                        oppdatertTilstandForPerson = behandlingsresultatPerson.andeler,
                        inneværendeMåned = inneværendeMåned
                    )

                if ((beløpEndret || finnesEndringerTilbakeITid || harGåttFraOpphørtTilLøpende) && !enesteEndringErLøpendeTilOpphørt) {
                    resultater.add(YtelsePersonResultat.ENDRET)
                }
            }

            ytelsePerson.copy(
                resultater = resultater.toSet(),
                ytelseSlutt = ytelseSlutt
            )
        }
    }

    private fun avslagPåNyPerson(
        personSomSjekkes: YtelsePerson,
        segmenterLagtTil: LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>
    ) =
        personSomSjekkes.kravOpprinnelse == listOf(KravOpprinnelse.INNEVÆRENDE) && segmenterLagtTil.isEmpty

    private fun finnesInnvilget(
        personSomSjekkes: YtelsePerson,
        segmenterLagtTil: LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>
    ) =
        personSomSjekkes.erFramstiltKravForIInneværendeBehandling() && !segmenterLagtTil.isEmpty

    private fun erYtelsenOpphørt(
        andeler: List<BehandlingsresultatAndelTilkjentYtelse>,
        inneværendeMåned: YearMonth
    ) = andeler.none { it.erLøpende(inneværendeMåned) }

    private fun finnesEndretSegmentTilbakeITid(segmenter: LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>) =
        !segmenter.isEmpty && segmenter.any { !it.erLøpende() }

    private fun finnesEndringTilbakeITid(
        personSomSjekkes: YtelsePerson,
        segmenterLagtTil: LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>,
        segmenterFjernet: LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>
    ): Boolean {
        return personSomSjekkes.erFramstiltKravForITidligereBehandling() &&
            (finnesEndretSegmentTilbakeITid(segmenterLagtTil) || finnesEndretSegmentTilbakeITid(segmenterFjernet))
    }

    private fun harGåttFraOpphørtTilLøpende(
        forrigeTilstandForPerson: List<BehandlingsresultatAndelTilkjentYtelse>,
        oppdatertTilstandForPerson: List<BehandlingsresultatAndelTilkjentYtelse>,
        inneværendeMåned: YearMonth
    ) =
        forrigeTilstandForPerson.isNotEmpty() && forrigeTilstandForPerson.none { it.erLøpende(inneværendeMåned) } && oppdatertTilstandForPerson.any {
            it.erLøpende(inneværendeMåned)
        }

    private fun enesteEndringErLøpendeTilOpphørt(
        segmenterLagtTil: LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>,
        segmenterFjernet: LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>,
        sisteAndelPåPerson: BehandlingsresultatAndelTilkjentYtelse?,
        inneværendeMåned: YearMonth
    ): Boolean {
        if (sisteAndelPåPerson == null) return true

        return if (segmenterLagtTil.isEmpty && !segmenterFjernet.isEmpty) {
            val stønadSlutt = sisteAndelPåPerson.stønadTom
            val opphører = stønadSlutt.isBefore(inneværendeMåned.plusMonths(1))
            val sisteForrigeAndel = segmenterFjernet.maxByOrNull { it.fom }
            val ingenFjernetFørStønadslutt = segmenterFjernet.none { it.fom.isBefore(stønadSlutt.toLocalDate()) }
            opphører && ingenFjernetFørStønadslutt && sisteForrigeAndel != null && sisteForrigeAndel.tom.toYearMonth() > inneværendeMåned
        } else {
            false
        }
    }
}
