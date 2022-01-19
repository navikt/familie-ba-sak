package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.overlapperHeltEllerDelvisMed
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
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
        personidentService: PersonidentService,
    ): List<YtelsePerson> {
        return behandlingsresultatPersoner.map { behandlingsresultatPerson ->
            val forrigeAndelerTidslinje = behandlingsresultatPerson.forrigeAndeler.tilTidslinje()
            val andelerTidslinje = behandlingsresultatPerson.andeler.tilTidslinje()

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

            if (finnesInnvilget(personSomSjekkes = ytelsePerson, segmenterLagtTil = segmenterLagtTil) || finnesØkning(
                    behandlingsresultatPerson = behandlingsresultatPerson
                )
            ) {
                resultater.add(YtelsePersonResultat.INNVILGET)
            }

            val ytelseSlutt: YearMonth? = if (behandlingsresultatPerson.andeler.isNotEmpty())
                behandlingsresultatPerson.andeler.maxByOrNull { it.stønadTom }?.stønadTom
                    ?: throw Feil("Finnes andel uten tom-dato") else TIDENES_MORGEN.toYearMonth()

            val erEndring =
                erEndring(behandlingsresultatPerson, segmenterLagtTil, segmenterFjernet, ytelsePerson, inneværendeMåned)

            if (erEndring) {
                resultater.add(YtelsePersonResultat.ENDRET)
            }

            ytelsePerson.copy(
                resultater = resultater.toSet(),
                ytelseSlutt = ytelseSlutt
            )
        } + uregistrerteBarn.map {
            YtelsePerson(
                aktør = personidentService.hentOgLagreAktør(it.personIdent),
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

    private fun erEndring(
        behandlingsresultatPerson: BehandlingsresultatPerson,
        segmenterLagtTil: LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>,
        segmenterFjernet: LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>,
        ytelsePerson: YtelsePerson,
        inneværendeMåned: YearMonth
    ) = if (behandlingsresultatPerson.søktForPerson) {
        val beløpRedusert = (segmenterLagtTil + segmenterFjernet).isEmpty() &&
            (behandlingsresultatPerson.forrigeAndeler.sumOf { it.sumForPeriode() } - behandlingsresultatPerson.andeler.sumOf { it.sumForPeriode() }) > 0

        val finnesReduksjonerTilbakeITid = ytelsePerson.erFramstiltKravForITidligereBehandling() &&
            segmenterFjernet.harSegmentMedTomFør(inneværendeMåned)

        finnesReduksjonerTilbakeITid || beløpRedusert
    } else {
        val stønadSlutt =
            behandlingsresultatPerson.andeler.maxByOrNull { it.stønadFom }?.stønadTom ?: TIDENES_MORGEN.toYearMonth()

        val forrigeStønadSlutt =
            behandlingsresultatPerson
                .forrigeAndeler
                .maxByOrNull { it.stønadFom }
                ?.stønadTom ?: TIDENES_MORGEN.toYearMonth()

        val opphører = stønadSlutt.isBefore(inneværendeMåned.plusMonths(1))

        val erAndelMedEndretBeløp = erAndelMedEndretBeløp(
            forrigeAndeler = behandlingsresultatPerson.forrigeAndeler,
            andeler = behandlingsresultatPerson.andeler
        )

        val erLagtTilSegmenterPåPersonFraTidligereBehandling = ytelsePerson.erFramstiltKravForITidligereBehandling() &&
            segmenterLagtTil.harSegmentMedTomFør(if (opphører) stønadSlutt else inneværendeMåned)

        val erFjernetSegmenter =
            segmenterFjernet.harSegmentMedTomFør(if (opphører) stønadSlutt else inneværendeMåned)

        val opphørsdatoErSattSenere = stønadSlutt.isAfter(forrigeStønadSlutt)

        erAndelMedEndretBeløp ||
            erLagtTilSegmenterPåPersonFraTidligereBehandling ||
            erFjernetSegmenter ||
            opphørsdatoErSattSenere
    }

    fun erAndelMedEndretBeløp(
        forrigeAndeler: List<BehandlingsresultatAndelTilkjentYtelse>,
        andeler: List<BehandlingsresultatAndelTilkjentYtelse>
    ): Boolean =
        andelerMedEndretBeløp(
            forrigeAndeler = forrigeAndeler,
            andeler = andeler
        ).any { andel -> andel.any { it != 0 } }

    private fun andelerMedEndretBeløp(
        forrigeAndeler: List<BehandlingsresultatAndelTilkjentYtelse>,
        andeler: List<BehandlingsresultatAndelTilkjentYtelse>
    ): List<List<Int>> = andeler.map { andel ->
        val andelerFraForrigeBehandlingISammePeriode =
            forrigeAndeler.filter { it.periode.overlapperHeltEllerDelvisMed(andel.periode) }

        andelerFraForrigeBehandlingISammePeriode.map {
            andel.kalkulertUtbetalingsbeløp - it.kalkulertUtbetalingsbeløp
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

    private fun finnesØkning(
        behandlingsresultatPerson: BehandlingsresultatPerson,
    ) =
        behandlingsresultatPerson.utledYtelsePerson()
            .erFramstiltKravForIInneværendeBehandling() && andelerMedEndretBeløp(
            forrigeAndeler = behandlingsresultatPerson.forrigeAndeler,
            andeler = behandlingsresultatPerson.andeler
        ).any { andel -> andel.any { it > 0 } }

    private fun erYtelsenOpphørt(
        andeler: List<BehandlingsresultatAndelTilkjentYtelse>,
        inneværendeMåned: YearMonth
    ) = andeler.none { it.erLøpende(inneværendeMåned) }

    private fun LocalDateTimeline<BehandlingsresultatAndelTilkjentYtelse>.harSegmentMedTomFør(
        måned: YearMonth
    ) = !this.isEmpty && this.any { it.tom < måned.sisteDagIInneværendeMåned() }
}
