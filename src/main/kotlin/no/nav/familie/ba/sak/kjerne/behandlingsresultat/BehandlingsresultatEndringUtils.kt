package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatOpphørUtils.utledOpphørsdatoForNåværendeBehandlingMedFallback
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.tilTidslinje
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIEndretUtbetalingAndelUtil
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIKompetanseUtil
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIVilkårsvurderingUtil
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærTilOgMed
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.logger
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.time.YearMonth

internal enum class Endringsresultat {
    ENDRING,
    INGEN_ENDRING,
}

object BehandlingsresultatEndringUtils {
    internal fun utledEndringsresultat(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
        personerFremstiltKravFor: List<Aktør>,
        nåværendeKompetanser: List<Kompetanse>,
        forrigeKompetanser: List<Kompetanse>,
        nåværendePersonResultat: Set<PersonResultat>,
        forrigePersonResultat: Set<PersonResultat>,
        nåværendeEndretAndeler: List<EndretUtbetalingAndel>,
        forrigeEndretAndeler: List<EndretUtbetalingAndel>,
        personerIBehandling: Set<Person>,
        personerIForrigeBehandling: Set<Person>,
        nåMåned: YearMonth,
    ): Endringsresultat {
        val relevantePersoner = (personerIBehandling.map { it.aktør } + personerIForrigeBehandling.map { it.aktør }).distinct()

        val endringerForRelevantePersoner =
            relevantePersoner.any { aktør ->
                val nåværendePersonResultatForPerson = nåværendePersonResultat.filter { it.aktør == aktør }.toSet()
                val forrigePersonResultatForPerson = forrigePersonResultat.filter { it.aktør == aktør }.toSet()

                val nåværendeAndelerForPerson = nåværendeAndeler.filter { it.aktør == aktør }
                val forrigeAndelerForPerson = forrigeAndeler.filter { it.aktør == aktør }

                val nåværendeEndretAndelerForPerson = nåværendeEndretAndeler.filter { it.person?.aktør == aktør }
                val forrigeEndretAndelerForPerson = forrigeEndretAndeler.filter { it.person?.aktør == aktør }

                val nåværendeKompetanserForPerson = nåværendeKompetanser.filter { it.barnAktører.contains(aktør) }
                val forrigeKompetanserForPerson = forrigeKompetanser.filter { it.barnAktører.contains(aktør) }

                val personIBehandling = personerIBehandling.singleOrNull { it.aktør == aktør }
                val personIForrigeBehandling = personerIForrigeBehandling.singleOrNull { it.aktør == aktør }

                val opphørstidspunktForBehandling =
                    nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
                        forrigeAndelerIBehandling = forrigeAndeler,
                        nåværendeEndretAndelerIBehandling = nåværendeEndretAndeler,
                        endretAndelerForForrigeBehandling = forrigeEndretAndeler,
                    )

                val erEndringIBeløpForPerson =
                    opphørstidspunktForBehandling?.let {
                        erEndringIBeløpForPerson(
                            nåværendeAndelerForPerson = nåværendeAndelerForPerson,
                            forrigeAndelerForPerson = forrigeAndelerForPerson,
                            opphørstidspunktForBehandling = opphørstidspunktForBehandling,
                            erFremstiltKravForPerson = personerFremstiltKravFor.contains(aktør),
                            nåMåned = nåMåned,
                        )
                    } ?: false // false hvis verken forrige eller nåværende behandling har andeler

                val tidligsteRelevanteFomDatoForPersonIVilkårsvurdering =
                    if (erEndringIBeløpForPerson) {
                        TIDENES_MORGEN.toYearMonth()
                    } else {
                        nåværendeAndelerForPerson.minOfOrNull { it.stønadFom }?.minusMonths(1) ?: TIDENES_MORGEN.toYearMonth()
                    }

                val erEndringIVilkårsvurderingForPerson =
                    erEndringIVilkårsvurderingForPerson(
                        tidligsteRelevanteFomDatoForPersonIVilkårsvurdering = tidligsteRelevanteFomDatoForPersonIVilkårsvurdering,
                        nåværendePersonResultaterForPerson = nåværendePersonResultatForPerson,
                        forrigePersonResultaterForPerson = forrigePersonResultatForPerson,
                        personIBehandling = personIBehandling,
                        personIForrigeBehandling = personIForrigeBehandling,
                    )

                val erEndringIKompetanseForPerson =
                    erEndringIKompetanseForPerson(
                        nåværendeKompetanserForPerson = nåværendeKompetanserForPerson,
                        forrigeKompetanserForPerson = forrigeKompetanserForPerson,
                    )

                val erEndringIEndretUtbetalingAndelerForPerson =
                    erEndringIEndretUtbetalingAndelerForPerson(
                        nåværendeEndretAndelerForPerson = nåværendeEndretAndelerForPerson,
                        forrigeEndretAndelerForPerson = forrigeEndretAndelerForPerson,
                    )

                val erMinstEnEndringForPerson =
                    erEndringIBeløpForPerson ||
                        erEndringIKompetanseForPerson ||
                        erEndringIVilkårsvurderingForPerson ||
                        erEndringIEndretUtbetalingAndelerForPerson

                if (erMinstEnEndringForPerson) {
                    logger.info(
                        "Endringer: " +
                            "erEndringIBeløp=$erEndringIBeløpForPerson for aktør ${aktør.aktørId}," +
                            "erEndringIKompetanse=$erEndringIKompetanseForPerson for aktør ${aktør.aktørId}, " +
                            "erEndringIVilkårsvurdering=$erEndringIVilkårsvurderingForPerson for aktør ${aktør.aktørId}, " +
                            "erEndringIEndretUtbetalingAndeler=$erEndringIEndretUtbetalingAndelerForPerson for aktør ${aktør.aktørId}",
                    )

                    val endredeAndelTilkjentYtelseForPerson = if (erEndringIBeløpForPerson) "nye AndelerTilkjentYtelse for aktør ${aktør.aktørId}: $nåværendeAndeler , " else ""
                    val endredeKompetanserForPerson = if (erEndringIKompetanseForPerson) "nye kompetanser for aktør ${aktør.aktørId}: $nåværendeKompetanser ," else ""
                    val endredeVilkårsvurderingerForPerson = if (erEndringIVilkårsvurderingForPerson) "nye personresultater for aktør ${aktør.aktørId}: $nåværendePersonResultat ," else ""
                    val endredeEndretUtbetalingAndelerForPerson = if (erEndringIEndretUtbetalingAndelerForPerson) "nye endretUtbetalingAndeler for aktør ${aktør.aktørId}: $nåværendeEndretAndeler" else ""

                    secureLogger.info(
                        "Endringer: $endredeAndelTilkjentYtelseForPerson $endredeKompetanserForPerson $endredeVilkårsvurderingerForPerson $endredeEndretUtbetalingAndelerForPerson",
                    )
                }

                erMinstEnEndringForPerson
            }

        return if (endringerForRelevantePersoner) Endringsresultat.ENDRING else Endringsresultat.INGEN_ENDRING
    }

    // NB: For personer fremstilt krav for tar vi ikke hensyn til alle endringer i beløp i denne funksjonen
    internal fun erEndringIBeløpForPerson(
        nåværendeAndelerForPerson: List<AndelTilkjentYtelse>,
        forrigeAndelerForPerson: List<AndelTilkjentYtelse>,
        erFremstiltKravForPerson: Boolean,
        opphørstidspunktForBehandling: YearMonth,
        nåMåned: YearMonth,
    ): Boolean {
        val ytelseTyperForPerson = (nåværendeAndelerForPerson.map { it.type } + forrigeAndelerForPerson.map { it.type }).distinct()

        return ytelseTyperForPerson.any { ytelseType ->
            erEndringIBeløpForPersonOgType(
                nåværendeAndeler = nåværendeAndelerForPerson.filter { it.type == ytelseType },
                forrigeAndeler = forrigeAndelerForPerson.filter { it.type == ytelseType },
                opphørstidspunktForBehandling = opphørstidspunktForBehandling,
                erFremstiltKravForPerson = erFremstiltKravForPerson,
                nåMåned = nåMåned,
            )
        }
    }
}

// Kun interessert i endringer i beløp FØR opphørstidspunkt og perioder som ikke er lengre enn 1 måned fram i tid
private fun erEndringIBeløpForPersonOgType(
    nåværendeAndeler: List<AndelTilkjentYtelse>,
    forrigeAndeler: List<AndelTilkjentYtelse>,
    opphørstidspunktForBehandling: YearMonth,
    erFremstiltKravForPerson: Boolean,
    nåMåned: YearMonth,
): Boolean {
    val nåværendeTidslinje = nåværendeAndeler.tilTidslinje()
    val forrigeTidslinje = forrigeAndeler.tilTidslinje()

    val endringIBeløpTidslinje =
        nåværendeTidslinje
            .kombinerMed(forrigeTidslinje) { nåværende, forrige ->
                val nåværendeBeløp = nåværende?.kalkulertUtbetalingsbeløp
                val forrigeBeløp = forrige?.kalkulertUtbetalingsbeløp

                if (erFremstiltKravForPerson) {
                    // Hvis det er søkt for person vil vi kun ha med endringer som går fra beløp > 0 til 0/null
                    when {
                        forrigeBeløp.erStørreEnn0() && nåværendeBeløp.er0EllerNull() -> true
                        else -> false
                    }
                } else {
                    // Hvis det ikke er søkt for person vil vi ha med alle endringer i beløp
                    when {
                        forrigeBeløp != nåværendeBeløp -> true
                        else -> false
                    }
                }
            }.fjernPerioderEtterOpphørsdato(opphørstidspunktForBehandling)
            .fjernPerioderLengreEnnEnMånedFramITid(nåMåned)

    return endringIBeløpTidslinje.tilPerioder().any { it.verdi == true }
}

private fun Int?.erStørreEnn0(): Boolean = this != null && this > 0

private fun Int?.er0EllerNull(): Boolean = this == null || this == 0

private fun Tidslinje<Boolean>.fjernPerioderEtterOpphørsdato(opphørstidspunkt: YearMonth) = this.beskjærTilOgMed(opphørstidspunkt.forrigeMåned().sisteDagIInneværendeMåned())

private fun Tidslinje<Boolean>.fjernPerioderLengreEnnEnMånedFramITid(nåMåned: YearMonth) = this.beskjærTilOgMed(nåMåned.nesteMåned().sisteDagIInneværendeMåned())

internal fun erEndringIKompetanseForPerson(
    nåværendeKompetanserForPerson: List<Kompetanse>,
    forrigeKompetanserForPerson: List<Kompetanse>,
): Boolean {
    val endringIKompetanseTidslinje =
        EndringIKompetanseUtil.lagEndringIKompetanseForPersonTidslinje(
            nåværendeKompetanserForPerson = nåværendeKompetanserForPerson,
            forrigeKompetanserForPerson = forrigeKompetanserForPerson,
        )

    return endringIKompetanseTidslinje.tilPerioder().any { it.verdi == true }
}

internal fun erEndringIVilkårsvurderingForPerson(
    nåværendePersonResultaterForPerson: Set<PersonResultat>,
    forrigePersonResultaterForPerson: Set<PersonResultat>,
    personIBehandling: Person?,
    personIForrigeBehandling: Person?,
    tidligsteRelevanteFomDatoForPersonIVilkårsvurdering: YearMonth,
): Boolean {
    val endringIVilkårsvurderingTidslinje =
        EndringIVilkårsvurderingUtil.lagEndringIVilkårsvurderingTidslinje(
            tidligsteRelevanteFomDatoForPersonIVilkårsvurdering = tidligsteRelevanteFomDatoForPersonIVilkårsvurdering,
            nåværendePersonResultaterForPerson = nåværendePersonResultaterForPerson,
            forrigePersonResultater = forrigePersonResultaterForPerson,
            personIBehandling = personIBehandling,
            personIForrigeBehandling = personIForrigeBehandling,
        )

    return endringIVilkårsvurderingTidslinje.tilPerioder().any { it.verdi == true }
}

internal fun erEndringIEndretUtbetalingAndelerForPerson(
    nåværendeEndretAndelerForPerson: List<EndretUtbetalingAndel>,
    forrigeEndretAndelerForPerson: List<EndretUtbetalingAndel>,
): Boolean {
    val endringIEndretUtbetalingAndelTidslinje =
        EndringIEndretUtbetalingAndelUtil.lagEndringIEndretUbetalingAndelPerPersonTidslinje(
            nåværendeEndretAndelerForPerson = nåværendeEndretAndelerForPerson,
            forrigeEndretAndelerForPerson = forrigeEndretAndelerForPerson,
        )

    return endringIEndretUtbetalingAndelTidslinje.tilPerioder().any { it.verdi == true }
}
