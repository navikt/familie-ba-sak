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
    fun utledYtelsePersonerMedResultat(behandlingsresultatPersoner: List<BehandlingsresultatPerson>): List<YtelsePerson> {
        return behandlingsresultatPersoner.map { behandlingsresultatPerson ->
            val forrigeAndelerTidslinje = LocalDateTimeline(behandlingsresultatPerson.forrigeAndeler.map {
                LocalDateSegment(
                        it.stønadFom.førsteDagIInneværendeMåned(),
                        it.stønadTom.sisteDagIInneværendeMåned(),
                        it
                )
            })
            val andelerTidslinje = LocalDateTimeline(behandlingsresultatPerson.andeler.map {
                LocalDateSegment(
                        it.stønadFom.førsteDagIInneværendeMåned(),
                        it.stønadTom.sisteDagIInneværendeMåned(),
                        it
                )
            })

            val segmenterLagtTil = andelerTidslinje.disjoint(forrigeAndelerTidslinje)
            val segmenterFjernet = forrigeAndelerTidslinje.disjoint(andelerTidslinje)
            val eksplisittAvslag = behandlingsresultatPerson.eksplisittAvslag

            /**
             * En temporær løsning for å håndtere use caset med delt bosted, hvor beløpet men ikke innvilget periode er
             * endret.
             */
            val beløpEndretIUforandretTidslinje = (segmenterLagtTil + segmenterFjernet).isEmpty()
                                                  && behandlingsresultatPerson.andeler.sumOf { it.kalkulertUtbetalingsbeløp } != behandlingsresultatPerson.forrigeAndeler.sumOf { it.kalkulertUtbetalingsbeløp }

            val resultater = mutableSetOf<YtelsePersonResultat>()
            val ytelsePerson = behandlingsresultatPerson.utledYtelsePerson()

            if (eksplisittAvslag || avslagPåNyPerson(personSomSjekkes = ytelsePerson,
                                                     segmenterLagtTil = segmenterLagtTil)) {
                resultater.add(YtelsePersonResultat.AVSLÅTT)
            }
            if (erYtelsenOpphørt(andeler = behandlingsresultatPerson.andeler) && (segmenterFjernet + segmenterLagtTil).isNotEmpty()) {
                resultater.add(YtelsePersonResultat.OPPHØRT)
            }

            if (finnesInnvilget(personSomSjekkes = ytelsePerson, segmenterLagtTil = segmenterLagtTil)) {
                resultater.add(YtelsePersonResultat.INNVILGET)
            }

            val ytelseSlutt: YearMonth? = if (behandlingsresultatPerson.andeler.isNotEmpty())
                behandlingsresultatPerson.andeler.maxByOrNull { it.stønadTom }?.stønadTom
                ?: throw Feil("Finnes andel uten tom-dato") else TIDENES_MORGEN.toYearMonth()

            if (behandlingsresultatPerson.andeler.isNotEmpty()
                && (beløpEndretIUforandretTidslinje
                    || finnesEndringTilbakeITid(personSomSjekkes = ytelsePerson,
                                                segmenterLagtTil = segmenterLagtTil,
                                                segmenterFjernet = segmenterFjernet)
                    || harGåttFraOpphørtTilLøpende(forrigeTilstandForPerson = behandlingsresultatPerson.forrigeAndeler,
                                                   oppdatertTilstandForPerson = behandlingsresultatPerson.andeler))
                && !enesteEndringErTidligereStønadslutt(segmenterLagtTil = segmenterLagtTil,
                                                        segmenterFjernet = segmenterFjernet,
                                                        sisteAndelPåPerson = behandlingsresultatPerson.andeler.maxByOrNull { it.stønadFom })) {
                resultater.add(YtelsePersonResultat.ENDRET)
            }

            ytelsePerson.copy(
                    resultater = resultater.toSet(),
                    ytelseSlutt = ytelseSlutt
            )
        }
    }

    private fun avslagPåNyPerson(personSomSjekkes: YtelsePerson,
                                 segmenterLagtTil: LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>) =
            personSomSjekkes.kravOpprinnelse == listOf(KravOpprinnelse.INNEVÆRENDE) && segmenterLagtTil.isEmpty

    private fun finnesInnvilget(personSomSjekkes: YtelsePerson,
                                segmenterLagtTil: LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>) =
            personSomSjekkes.erFramstiltKravForIInneværendeBehandling() && !segmenterLagtTil.isEmpty

    private fun erYtelsenOpphørt(andeler: List<BehandlingsresultatAndelTilkjentYtelse>) = andeler.none { it.erLøpende() }

    private fun finnesEndringTilbakeITid(personSomSjekkes: YtelsePerson,
                                         segmenterLagtTil: LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>,
                                         segmenterFjernet: LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>): Boolean {
        fun finnesEndretSegmentTilbakeITid(segmenter: LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>) =
                !segmenter.isEmpty && segmenter.any { !it.erLøpende() }

        return personSomSjekkes.erFramstiltKravForITidligereBehandling() &&
               (finnesEndretSegmentTilbakeITid(segmenterLagtTil) || finnesEndretSegmentTilbakeITid(segmenterFjernet))

    }

    private fun harGåttFraOpphørtTilLøpende(forrigeTilstandForPerson: List<BehandlingsresultatAndelTilkjentYtelse>,
                                            oppdatertTilstandForPerson: List<BehandlingsresultatAndelTilkjentYtelse>) =
            forrigeTilstandForPerson.isNotEmpty() && forrigeTilstandForPerson.none { it.erLøpende() } && oppdatertTilstandForPerson.any { it.erLøpende() }

    private fun enesteEndringErTidligereStønadslutt(segmenterLagtTil: LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>,
                                                    segmenterFjernet: LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>,
                                                    sisteAndelPåPerson: BehandlingsresultatAndelTilkjentYtelse?): Boolean {
        return if (sisteAndelPåPerson != null && segmenterLagtTil.isEmpty && !segmenterFjernet.isEmpty) {
            val stønadSlutt = sisteAndelPåPerson.stønadTom
            val opphører = stønadSlutt.isBefore(YearMonth.now().plusMonths(1))
            val ingenFjernetFørStønadslutt = segmenterFjernet.none { it.fom.isBefore(stønadSlutt.toLocalDate()) }
            opphører && ingenFjernetFørStønadslutt
        } else {
            false
        }
    }
}