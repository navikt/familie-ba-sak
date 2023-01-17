package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators

object BehandlingsresultatUtils {

    private enum class EndringIUtbetalingFraForrigeBehandling(val resultat: Behandlingsresultat) {
        INGEN_ENDRING_I_PERIODE(Behandlingsresultat.FORTSATT_INNVILGET),
        INNVILGET_PERIODE(Behandlingsresultat.INNVILGET),
        AVSLÅTT_PERIODE(Behandlingsresultat.AVSLÅTT)
    }

    internal fun utledResultatPåSøknadfghjk(
        personerFremstiltKravFor: List<Aktør>,
        andelerIDenneBehandlingen: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        andelerIForrigeBehandling: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        vilkårsvurdering: Vilkårsvurdering
    ): List<Behandlingsresultat> {
        return personerFremstiltKravFor.flatMap { aktørFremstiltKravFor ->
            val erEksplisittAvslagPåPerson = vilkårsvurdering.finnesEksplisittAvslagPåPerson(aktørFremstiltKravFor)
            val resultaterFraTilkjentYtelse = utledResultatForPersonForTilkjentYtelse(
                aktørFremstiltKravFor = aktørFremstiltKravFor,
                andelerIDenneBehandlingen = andelerIDenneBehandlingen,
                andelerIForrigeBehandling = andelerIForrigeBehandling
            )
            when (erEksplisittAvslagPåPerson) {
                true -> resultaterFraTilkjentYtelse.plus(Behandlingsresultat.AVSLÅTT)
                false -> resultaterFraTilkjentYtelse
            }.distinct()
        }
    }

    private fun Vilkårsvurdering.finnesEksplisittAvslagPåPerson(aktørFremstiltKravFor: Aktør): Boolean {
        val vilkårResultaterForPerson = this.personResultater.find { it.aktør == aktørFremstiltKravFor }?.vilkårResultater ?: throw Feil("Finner ikke personresultat for person det er fremstilt krav for")
        return vilkårResultaterForPerson.any { it.erEksplisittAvslagPåSøknad == true }
    }

    private fun utledResultatForPersonForTilkjentYtelse(aktørFremstiltKravFor: Aktør, andelerIDenneBehandlingen: List<AndelTilkjentYtelseMedEndreteUtbetalinger>, andelerIForrigeBehandling: List<AndelTilkjentYtelseMedEndreteUtbetalinger>): List<Behandlingsresultat> {
        val andelerNå = andelerIDenneBehandlingen.filter { it.aktør == aktørFremstiltKravFor && (it.type == YtelseType.UTVIDET_BARNETRYGD || it.type == YtelseType.ORDINÆR_BARNETRYGD) }
        val andelerForrige = andelerIForrigeBehandling.filter { it.aktør == aktørFremstiltKravFor && (it.type == YtelseType.UTVIDET_BARNETRYGD || it.type == YtelseType.ORDINÆR_BARNETRYGD) }

        val nåværendeTidslinje = AndelTilkjentYtelseTidslinje(andelerNå)
        val forrigeTidslinje = AndelTilkjentYtelseTidslinje(andelerForrige)

        val periodeResultatTidslinje = nåværendeTidslinje.kombinerMed(forrigeTidslinje) { nåværende, forrige ->
            when {
                nåværende == null -> null
                nåværende.kalkulertUtbetalingsbeløp > 0 && forrige?.kalkulertUtbetalingsbeløp != nåværende.kalkulertUtbetalingsbeløp -> EndringIUtbetalingFraForrigeBehandling.INNVILGET_PERIODE
                nåværende.kalkulertUtbetalingsbeløp == 0 && forrige?.kalkulertUtbetalingsbeløp != nåværende.kalkulertUtbetalingsbeløp -> when (nåværende.endreteUtbetalinger.single().årsak) {
                    Årsak.DELT_BOSTED -> EndringIUtbetalingFraForrigeBehandling.INNVILGET_PERIODE
                    Årsak.ETTERBETALING_3ÅR, Årsak.ENDRE_MOTTAKER, Årsak.ALLEREDE_UTBETALT -> EndringIUtbetalingFraForrigeBehandling.AVSLÅTT_PERIODE
                    null -> null
                }
                else -> EndringIUtbetalingFraForrigeBehandling.INGEN_ENDRING_I_PERIODE
            }
        }

        val periodeResultater = periodeResultatTidslinje.perioder().mapNotNull { it.innhold }

        return if (periodeResultater.all { it == EndringIUtbetalingFraForrigeBehandling.INGEN_ENDRING_I_PERIODE }) listOf(Behandlingsresultat.FORTSATT_INNVILGET)
        else periodeResultater.filter { it != EndringIUtbetalingFraForrigeBehandling.INGEN_ENDRING_I_PERIODE }.map { it.resultat }.distinct()
    }

    private fun ikkeStøttetFeil(behandlingsresultater: MutableSet<YtelsePersonResultat>) =
        Feil(
            frontendFeilmelding = "Behandlingsresultatet du har fått på behandlingen er ikke støttet i løsningen enda. Ta kontakt med Team familie om du er uenig i resultatet.",
            message = "Kombiansjonen av behandlingsresultatene $behandlingsresultater er ikke støttet i løsningen."
        )

    internal fun utledBehandlingsresultatDataForPerson(
        person: Person,
        personerFremstiltKravFor: List<Aktør>,
        andelerFraForrigeTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        erEksplisittAvslag: Boolean
    ): BehandlingsresultatPerson {
        val aktør = person.aktør

        return BehandlingsresultatPerson(
            aktør = aktør,
            personType = person.type,
            søktForPerson = personerFremstiltKravFor.contains(aktør),
            forrigeAndeler = when (person.type) {
                PersonType.SØKER -> kombinerOverlappendeAndelerForSøker(
                    andelerFraForrigeTilkjentYtelse.filter { it.aktør == aktør }
                )

                else -> andelerFraForrigeTilkjentYtelse.filter { it.aktør == aktør }
                    .map { andelTilkjentYtelse ->
                        BehandlingsresultatAndelTilkjentYtelse(
                            stønadFom = andelTilkjentYtelse.stønadFom,
                            stønadTom = andelTilkjentYtelse.stønadTom,
                            kalkulertUtbetalingsbeløp = andelTilkjentYtelse.kalkulertUtbetalingsbeløp
                        )
                    }
            },
            andeler = when (person.type) {
                PersonType.SØKER -> kombinerOverlappendeAndelerForSøker(andelerTilkjentYtelse.filter { it.aktør == aktør })
                else -> andelerTilkjentYtelse.filter { it.aktør == aktør }
                    .map { andelTilkjentYtelse ->
                        BehandlingsresultatAndelTilkjentYtelse(
                            stønadFom = andelTilkjentYtelse.stønadFom,
                            stønadTom = andelTilkjentYtelse.stønadTom,
                            kalkulertUtbetalingsbeløp = andelTilkjentYtelse.kalkulertUtbetalingsbeløp
                        )
                    }
            },
            eksplisittAvslag = erEksplisittAvslag
        )
    }

    internal fun utledBehandlingsresultatBasertPåYtelsePersoner(
        ytelsePersoner: List<YtelsePerson>,
        sjekkOmUtvidaBarnetrygdErEndra: Boolean = true
    ): Behandlingsresultat {
        validerYtelsePersoner(ytelsePersoner)

        val samledeResultater = ytelsePersoner.flatMap { it.resultater }.toMutableSet()
        val erKunFremstilKravIDenneBehandling =
            ytelsePersoner.all { it.kravOpprinnelse == listOf(KravOpprinnelse.INNEVÆRENDE) }

        val altOpphører = ytelsePersoner.all { it.ytelseSlutt!!.isSameOrBefore(inneværendeMåned()) }
        val erAvslått = ytelsePersoner.all { it.resultater == setOf(YtelsePersonResultat.AVSLÅTT) }
        val opphørPåSammeTid = altOpphører &&
            (
                ytelsePersoner.filter { it.resultater != setOf(YtelsePersonResultat.AVSLÅTT) }
                    .groupBy { it.ytelseSlutt }.size == 1 || erAvslått
                )
        val kunFortsattOpphørt = ytelsePersoner.all { it.resultater == setOf(YtelsePersonResultat.FORTSATT_OPPHØRT) }
        val noeOpphørerPåTidligereBarn = ytelsePersoner.any {
            it.resultater.contains(YtelsePersonResultat.OPPHØRT) && !it.kravOpprinnelse.contains(KravOpprinnelse.INNEVÆRENDE)
        }

        if (noeOpphørerPåTidligereBarn && !altOpphører) {
            samledeResultater.add(YtelsePersonResultat.ENDRET_UTBETALING)
        }

        val opphørSomFørerTilEndring =
            (
                altOpphører || erUtvidaBarnetrygdEndra(
                    ytelsePersoner,
                    sjekkOmUtvidaBarnetrygdErEndra
                )
                ) && !opphørPåSammeTid && !erKunFremstilKravIDenneBehandling && !kunFortsattOpphørt
        if (opphørSomFørerTilEndring) {
            samledeResultater.add(YtelsePersonResultat.ENDRET_UTBETALING)
        }

        if (!altOpphører) {
            samledeResultater.remove(YtelsePersonResultat.OPPHØRT)
        }

        return finnBehandlingsresultat(samledeResultater)
    }

    private fun erUtvidaBarnetrygdEndra(
        ytelsePersoner: List<YtelsePerson>,
        sjekkOmUtvidaBarnetrygdErEndra: Boolean
    ): Boolean {
        if (!sjekkOmUtvidaBarnetrygdErEndra) {
            return false
        }
        val utvidaBarnetrygd = ytelsePersoner
            .filter { it.ytelseType == YtelseType.UTVIDET_BARNETRYGD }

        return if (utvidaBarnetrygd.isEmpty()) {
            false
        } else {
            utvidaBarnetrygd.all {
                it.resultater == setOf(YtelsePersonResultat.OPPHØRT)
            }
        }
    }

    private fun finnBehandlingsresultat(samledeResultater: MutableSet<YtelsePersonResultat>): Behandlingsresultat =
        when {
            samledeResultater.isEmpty() -> Behandlingsresultat.FORTSATT_INNVILGET
            samledeResultater == setOf(YtelsePersonResultat.FORTSATT_OPPHØRT) -> Behandlingsresultat.FORTSATT_OPPHØRT
            samledeResultater == setOf(YtelsePersonResultat.ENDRET_UTBETALING) -> Behandlingsresultat.ENDRET_UTBETALING
            samledeResultater == setOf(YtelsePersonResultat.ENDRET_UTEN_UTBETALING) -> Behandlingsresultat.ENDRET_UTEN_UTBETALING
            samledeResultater == setOf(
                YtelsePersonResultat.ENDRET_UTBETALING,
                YtelsePersonResultat.ENDRET_UTEN_UTBETALING
            ) -> Behandlingsresultat.ENDRET_UTBETALING

            samledeResultater.matcherAltOgHarBådeEndretOgOpphørtResultat(emptySet()) -> Behandlingsresultat.ENDRET_OG_OPPHØRT
            samledeResultater == setOf(YtelsePersonResultat.OPPHØRT, YtelsePersonResultat.FORTSATT_OPPHØRT) ||
                samledeResultater == setOf(YtelsePersonResultat.OPPHØRT) -> Behandlingsresultat.OPPHØRT

            samledeResultater == setOf(YtelsePersonResultat.INNVILGET) -> Behandlingsresultat.INNVILGET
            samledeResultater.matcherAltOgHarOpphørtResultat(setOf(YtelsePersonResultat.INNVILGET)) -> Behandlingsresultat.INNVILGET_OG_OPPHØRT
            samledeResultater.matcherAltOgHarEndretResultat(setOf(YtelsePersonResultat.INNVILGET)) -> Behandlingsresultat.INNVILGET_OG_ENDRET
            samledeResultater.matcherAltOgHarBådeEndretOgOpphørtResultat(setOf(YtelsePersonResultat.INNVILGET)) -> Behandlingsresultat.INNVILGET_ENDRET_OG_OPPHØRT
            samledeResultater == setOf(
                YtelsePersonResultat.INNVILGET,
                YtelsePersonResultat.AVSLÅTT
            ) -> Behandlingsresultat.DELVIS_INNVILGET

            samledeResultater.matcherAltOgHarOpphørtResultat(
                setOf(
                    YtelsePersonResultat.INNVILGET,
                    YtelsePersonResultat.AVSLÅTT
                )
            ) -> Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT

            samledeResultater.matcherAltOgHarEndretResultat(
                setOf(
                    YtelsePersonResultat.INNVILGET,
                    YtelsePersonResultat.AVSLÅTT
                )
            ) -> Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET

            samledeResultater.matcherAltOgHarBådeEndretOgOpphørtResultat(
                setOf(
                    YtelsePersonResultat.INNVILGET,
                    YtelsePersonResultat.AVSLÅTT
                )
            ) -> Behandlingsresultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT

            samledeResultater == setOf(YtelsePersonResultat.AVSLÅTT) -> Behandlingsresultat.AVSLÅTT
            samledeResultater == setOf(
                YtelsePersonResultat.AVSLÅTT,
                YtelsePersonResultat.FORTSATT_OPPHØRT
            ) -> Behandlingsresultat.AVSLÅTT // for å få riktig brevmål AVSLÅTT siden det var ingen endring fra forrige
            samledeResultater == setOf(
                YtelsePersonResultat.AVSLÅTT,
                YtelsePersonResultat.OPPHØRT
            ) -> Behandlingsresultat.AVSLÅTT_OG_OPPHØRT

            samledeResultater == setOf(
                YtelsePersonResultat.AVSLÅTT,
                YtelsePersonResultat.OPPHØRT,
                YtelsePersonResultat.FORTSATT_OPPHØRT
            ) -> Behandlingsresultat.AVSLÅTT_OG_OPPHØRT

            samledeResultater.matcherAltOgHarEndretResultat(setOf(YtelsePersonResultat.AVSLÅTT)) -> Behandlingsresultat.AVSLÅTT_OG_ENDRET
            samledeResultater.matcherAltOgHarBådeEndretOgOpphørtResultat(
                setOf(YtelsePersonResultat.AVSLÅTT)
            ) -> Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT

            else -> throw ikkeStøttetFeil(samledeResultater)
        }
}

private fun kombinerOverlappendeAndelerForSøker(andeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>): List<BehandlingsresultatAndelTilkjentYtelse> {
    val utbetalingstidslinjeForSøker = hentUtbetalingstidslinjeForSøker(andeler)

    return utbetalingstidslinjeForSøker.toSegments().map { andelTilkjentYtelse ->
        BehandlingsresultatAndelTilkjentYtelse(
            stønadFom = andelTilkjentYtelse.fom.toYearMonth(),
            stønadTom = andelTilkjentYtelse.tom.toYearMonth(),
            kalkulertUtbetalingsbeløp = andelTilkjentYtelse.value
        )
    }
}

fun hentUtbetalingstidslinjeForSøker(andeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>): LocalDateTimeline<Int> {
    val utvidetTidslinje = LocalDateTimeline(
        andeler.filter { it.type == YtelseType.UTVIDET_BARNETRYGD }
            .map {
                LocalDateSegment(
                    it.stønadFom.førsteDagIInneværendeMåned(),
                    it.stønadTom.sisteDagIInneværendeMåned(),
                    it.kalkulertUtbetalingsbeløp
                )
            }
    )
    val småbarnstilleggAndeler = LocalDateTimeline(
        andeler.filter { it.type == YtelseType.SMÅBARNSTILLEGG }.map {
            LocalDateSegment(
                it.stønadFom.førsteDagIInneværendeMåned(),
                it.stønadTom.sisteDagIInneværendeMåned(),
                it.kalkulertUtbetalingsbeløp
            )
        }
    )

    return utvidetTidslinje.combine(
        småbarnstilleggAndeler,
        StandardCombinators::sum,
        LocalDateTimeline.JoinStyle.CROSS_JOIN
    )
}

private fun Set<YtelsePersonResultat>.matcherAltOgHarEndretResultat(andreElementer: Set<YtelsePersonResultat>): Boolean {
    val endretResultat = this.singleOrNull {
        it == YtelsePersonResultat.ENDRET_UTBETALING ||
            it == YtelsePersonResultat.ENDRET_UTEN_UTBETALING
    } ?: return false
    return this == setOf(endretResultat) + andreElementer
}

private fun Set<YtelsePersonResultat>.matcherAltOgHarOpphørtResultat(andreElementer: Set<YtelsePersonResultat>): Boolean {
    val opphørtResultat = this.intersect(setOf(YtelsePersonResultat.OPPHØRT, YtelsePersonResultat.FORTSATT_OPPHØRT))
    return if (opphørtResultat.isEmpty()) false else this == andreElementer + opphørtResultat
}

private fun Set<YtelsePersonResultat>.matcherAltOgHarBådeEndretOgOpphørtResultat(andreElementer: Set<YtelsePersonResultat>): Boolean {
    val endretResultat = this.singleOrNull {
        it == YtelsePersonResultat.ENDRET_UTBETALING ||
            it == YtelsePersonResultat.ENDRET_UTEN_UTBETALING
    } ?: return false

    val opphørtResultat = this.intersect(setOf(YtelsePersonResultat.OPPHØRT, YtelsePersonResultat.FORTSATT_OPPHØRT))

    return if (opphørtResultat.isEmpty()) false else this == setOf(endretResultat) + opphørtResultat + andreElementer
}
