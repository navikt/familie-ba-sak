package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.EndretUtbetalingAndelTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNullMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjær
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilTidslinje
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import java.time.YearMonth

object BehandlingsresultatUtils {

    internal enum class Endringsresultat {
        ENDRING,
        INGEN_ENDRING
    }

    internal fun erEndringIKompetanse(
        nåværendeKompetanser: List<Kompetanse>,
        forrigeKompetanser: List<Kompetanse>
    ): Boolean {
        val allePersonerMedKompetanser = (nåværendeKompetanser.flatMap { it.barnAktører } + forrigeKompetanser.flatMap { it.barnAktører }).distinct()

        val finnesPersonMedEndretKompetanse = allePersonerMedKompetanser.any { aktør ->
            erEndringIKompetanseForPerson(
                nåværendeKompetanser = nåværendeKompetanser.filter { it.barnAktører.contains(aktør) },
                forrigeKompetanser = forrigeKompetanser.filter { it.barnAktører.contains(aktør) }
            )
        }

        return finnesPersonMedEndretKompetanse
    }

    private fun erEndringIKompetanseForPerson(
        nåværendeKompetanser: List<Kompetanse>,
        forrigeKompetanser: List<Kompetanse>
    ): Boolean {
        val nåværendeTidslinje = nåværendeKompetanser.tilTidslinje()
        val forrigeTidslinje = forrigeKompetanser.tilTidslinje()

        val endringerTidslinje = nåværendeTidslinje.kombinerUtenNullMed(forrigeTidslinje) { nåværende, forrige ->
            (
                nåværende.søkersAktivitet != forrige.søkersAktivitet ||
                    nåværende.søkersAktivitetsland != forrige.søkersAktivitetsland ||
                    nåværende.annenForeldersAktivitet != forrige.annenForeldersAktivitet ||
                    nåværende.annenForeldersAktivitetsland != forrige.annenForeldersAktivitetsland ||
                    nåværende.barnetsBostedsland != forrige.barnetsBostedsland ||
                    nåværende.resultat != forrige.resultat
                )
        }

        return endringerTidslinje.perioder().any { it.innhold == true }
    }

    internal fun erEndringIEndretUtbetalingAndeler(
        nåværendeEndretAndeler: List<EndretUtbetalingAndel>,
        forrigeEndretAndeler: List<EndretUtbetalingAndel>
    ): Boolean {
        val allePersoner = (nåværendeEndretAndeler.mapNotNull { it.person?.aktør } + forrigeEndretAndeler.mapNotNull { it.person?.aktør }).distinct()

        val finnesPersonerMedEndretEndretUtbetalingAndel = allePersoner.any { aktør ->
            erEndringIEndretUtbetalingAndelPerPerson(
                nåværendeEndretAndeler = nåværendeEndretAndeler.filter { it.person?.aktør == aktør },
                forrigeEndretAndeler = forrigeEndretAndeler.filter { it.person?.aktør == aktør }
            )
        }

        return finnesPersonerMedEndretEndretUtbetalingAndel
    }

    private fun erEndringIEndretUtbetalingAndelPerPerson(
        nåværendeEndretAndeler: List<EndretUtbetalingAndel>,
        forrigeEndretAndeler: List<EndretUtbetalingAndel>
    ): Boolean {
        val nåværendeTidslinje = EndretUtbetalingAndelTidslinje(nåværendeEndretAndeler)
        val forrigeTidslinje = EndretUtbetalingAndelTidslinje(forrigeEndretAndeler)

        val endringerTidslinje = nåværendeTidslinje.kombinerUtenNullMed(forrigeTidslinje) { nåværende, forrige ->
            (
                nåværende.avtaletidspunktDeltBosted != forrige.avtaletidspunktDeltBosted ||
                    nåværende.årsak != forrige.årsak ||
                    nåværende.søknadstidspunkt != forrige.søknadstidspunkt
                )
        }

        return endringerTidslinje.perioder().any { it.innhold == true }
    }

    internal fun utledEndringsresultat(
        nåværendeAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        forrigeAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        personerFremstiltKravFor: List<Aktør>,
        nåværendeKompetanser: List<Kompetanse>,
        forrigeKompetanser: List<Kompetanse>,
        nåværendePersonResultat: List<PersonResultat>,
        forrigePersonResultat: List<PersonResultat>,
        nåværendeEndretAndeler: List<EndretUtbetalingAndel>,
        forrigeEndretAndeler: List<EndretUtbetalingAndel>
    ): Endringsresultat {
        val erEndringIBeløp = erEndringIBeløp(
            nåværendeAndeler = nåværendeAndeler,
            forrigeAndeler = forrigeAndeler,
            personerFremstiltKravFor = personerFremstiltKravFor
        )

        val erEndringIKompetanse = erEndringIKompetanse(
            nåværendeKompetanser = nåværendeKompetanser,
            forrigeKompetanser = forrigeKompetanser
        )

        val erEndringIVilkårsvurdering = erEndringIVilkårvurdering(
            nåværendePersonResultat = nåværendePersonResultat,
            forrigePersonResultat = forrigePersonResultat
        )

        val erEndringIEndretUtbetalingAndeler = erEndringIEndretUtbetalingAndeler(
            nåværendeEndretAndeler = nåværendeEndretAndeler,
            forrigeEndretAndeler = forrigeEndretAndeler
        )

        val erMinstEnEndring = erEndringIBeløp || erEndringIKompetanse || erEndringIVilkårsvurdering || erEndringIEndretUtbetalingAndeler

        return if (erMinstEnEndring) Endringsresultat.ENDRING else Endringsresultat.INGEN_ENDRING
    }

    private fun ikkeStøttetFeil(behandlingsresultater: MutableSet<YtelsePersonResultat>) =
        Feil(
            frontendFeilmelding = "Behandlingsresultatet du har fått på behandlingen er ikke støttet i løsningen enda. Ta kontakt med Team familie om du er uenig i resultatet.",
            message = "Kombiansjonen av behandlingsresultatene $behandlingsresultater er ikke støttet i løsningen."
        )

    // NB: For personer fremstilt krav for tar vi ikke hensyn til alle endringer i beløp i denne funksjonen
    internal fun erEndringIBeløp(
        nåværendeAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        forrigeAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        personerFremstiltKravFor: List<Aktør>
    ): Boolean {
        val allePersonerMedAndeler = (nåværendeAndeler.map { it.aktør } + forrigeAndeler.map { it.aktør }).distinct()
        val opphørstidspunkt = nåværendeAndeler.maxOfOrNull { it.stønadTom } ?: TIDENES_MORGEN.toYearMonth()

        val erEndringIBeløpForMinstEnPerson = allePersonerMedAndeler.any { aktør ->
            erEndringIBeløpForPerson(
                nåværendeAndeler = nåværendeAndeler.filter { it.aktør == aktør },
                forrigeAndeler = forrigeAndeler.filter { it.aktør == aktør },
                opphørstidspunkt = opphørstidspunkt,
                erFremstiltKravForPerson = personerFremstiltKravFor.contains(aktør)
            )
        }

        return erEndringIBeløpForMinstEnPerson
    }

    // Kun interessert i endringer i beløp FØR opphørstidspunkt
    private fun erEndringIBeløpForPerson(
        nåværendeAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        forrigeAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        opphørstidspunkt: YearMonth,
        erFremstiltKravForPerson: Boolean
    ): Boolean {
        val nåværendeTidslinje = AndelTilkjentYtelseTidslinje(nåværendeAndeler)
        val forrigeTidslinje = AndelTilkjentYtelseTidslinje(forrigeAndeler)

        val endringIBeløpTidslinje = nåværendeTidslinje.kombinerMed(forrigeTidslinje) { nåværende, forrige ->
            val nåværendeBeløp = nåværende?.kalkulertUtbetalingsbeløp ?: 0
            val forrigeBeløp = forrige?.kalkulertUtbetalingsbeløp ?: 0

            if (erFremstiltKravForPerson) {
                // Hvis det er søkt for person vil vi kun ha med endringer som går fra beløp > 0 til 0/null
                when {
                    forrigeBeløp > 0 && nåværendeBeløp == 0 -> true
                    else -> false
                }
            } else {
                // Hvis det ikke er søkt for person vil vi ha med alle endringer i beløp
                when {
                    forrigeBeløp != nåværendeBeløp -> true
                    else -> false
                }
            }
        }.fjernPerioderEtterOpphørsdato(opphørstidspunkt)

        return endringIBeløpTidslinje.perioder().any { it.innhold == true }
    }

    private fun Tidslinje<Boolean, Måned>.fjernPerioderEtterOpphørsdato(opphørstidspunkt: YearMonth) =
        this.beskjær(fraOgMed = TIDENES_MORGEN.tilMånedTidspunkt(), tilOgMed = opphørstidspunkt.tilTidspunkt())

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
        ytelsePersoner: List<YtelsePerson>
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
                    ytelsePersoner
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
        ytelsePersoner: List<YtelsePerson>
    ): Boolean {
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

fun hentOpphørsresultatPåBehandling(
    nåværendeAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    forrigeAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
): Opphørsresultat {
    val nåværendeBehandlingOpphørsdato = nåværendeAndeler.maxOf { it.stønadTom }
    val forrigeBehandlingOpphørsdato = forrigeAndeler.maxOf { it.stønadTom }
    val dagensDato = YearMonth.now()

    return when {
        // Rekkefølgen av sjekkene er viktig for å komme fram til riktig opphørsresultat.
        nåværendeBehandlingOpphørsdato > dagensDato -> Opphørsresultat.IKKE_OPPHØRT
        forrigeBehandlingOpphørsdato > dagensDato || forrigeBehandlingOpphørsdato > nåværendeBehandlingOpphørsdato -> Opphørsresultat.OPPHØRT
        else -> Opphørsresultat.FORTSATT_OPPHØRT
    }
}

enum class Opphørsresultat {
    OPPHØRT,
    FORTSATT_OPPHØRT,
    IKKE_OPPHØRT
}

fun erEndringIVilkårvurdering(
    nåværendePersonResultat: List<PersonResultat>,
    forrigePersonResultat: List<PersonResultat>
): Boolean {
    val allePersonerMedPersonResultat =
        (nåværendePersonResultat.map { it.aktør } + forrigePersonResultat.map { it.aktør }).distinct()

    val finnesPersonMedEndretVilkårsvurdering = allePersonerMedPersonResultat.any { aktør ->

        Vilkår.values().any { vilkårType ->
            erEndringIVilkårvurderingForPerson(
                nåværendePersonResultat
                    .filter { it.aktør == aktør }
                    .flatMap { it.vilkårResultater }
                    .filter { it.vilkårType == vilkårType && it.resultat == Resultat.OPPFYLT },
                forrigePersonResultat
                    .filter { it.aktør == aktør }
                    .flatMap { it.vilkårResultater }
                    .filter { it.vilkårType == vilkårType && it.resultat == Resultat.OPPFYLT }
            )
        }
    }

    return finnesPersonMedEndretVilkårsvurdering
}

// Relevante endringer er
// 1. Endringer i utdypende vilkårsvurdering
// 2. Endringer i regelverk
// 3. Splitt i vilkårsvurderingen
fun erEndringIVilkårvurderingForPerson(
    nåværendeVilkårResultat: List<VilkårResultat>,
    forrigeVilkårResultat: List<VilkårResultat>
): Boolean {
    val nåværendeVilkårResultatTidslinje = nåværendeVilkårResultat.tilTidslinje()
    val tidligereVilkårResultatTidslinje = forrigeVilkårResultat.tilTidslinje()

    val endringIVilkårResultat =
        nåværendeVilkårResultatTidslinje.kombinerUtenNullMed(tidligereVilkårResultatTidslinje) { nåværende, forrige ->

            nåværende.utdypendeVilkårsvurderinger.toSet() != forrige.utdypendeVilkårsvurderinger.toSet() ||
                nåværende.vurderesEtter != forrige.vurderesEtter ||
                nåværende.periodeFom != forrige.periodeFom ||
                nåværende.periodeTom != forrige.periodeTom
        }

    return endringIVilkårResultat.perioder().any { it.innhold == true }
}
