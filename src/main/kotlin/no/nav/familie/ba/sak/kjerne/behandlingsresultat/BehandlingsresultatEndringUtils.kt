package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatOpphørUtils.utledOpphørsdatoForNåværendeBehandlingMedFallback
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.tilTidslinje
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIEndretUtbetalingAndelUtil
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIKompetanseUtil
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIUtenlandskPeriodebeløpUtil
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
    ENDRING_UTEN_BELØPSENDRING,
    INGEN_ENDRING,
}

private data class EndringForPerson(
    val erEndring: Boolean,
    val erBeløpsEndring: Boolean,
)

object BehandlingsresultatEndringUtils {
    internal fun utledEndringsresultat(
        behandling: Behandling,
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
        personerFremstiltKravFor: List<Aktør>,
        nåværendeKompetanser: List<Kompetanse>,
        forrigeKompetanser: List<Kompetanse>,
        nåværendePersonResultat: Set<PersonResultat>,
        nåværendeUtenlandskPeriodebeløp: List<UtenlandskPeriodebeløp>,
        forrigeUtenlandskPeriodebeløp: List<UtenlandskPeriodebeløp>,
        forrigePersonResultat: Set<PersonResultat>,
        nåværendeEndretAndeler: List<EndretUtbetalingAndel>,
        forrigeEndretAndeler: List<EndretUtbetalingAndel>,
        personerIBehandling: Set<Person>,
        personerIForrigeBehandling: Set<Person>,
        nåMåned: YearMonth,
        featureToggleService: FeatureToggleService,
    ): Endringsresultat {
        val relevantePersoner = (personerIBehandling.map { it.aktør } + personerIForrigeBehandling.map { it.aktør }).distinct()

        val endringsInfoForRelevantePersoner =
            relevantePersoner.map { aktør ->
                val erEndringIBeløpForPerson =
                    erEndringIBeløpForPerson(
                        aktør = aktør,
                        nåværendeAndeler = nåværendeAndeler,
                        forrigeAndeler = forrigeAndeler,
                        nåværendeEndretAndeler = nåværendeEndretAndeler,
                        forrigeEndretAndeler = forrigeEndretAndeler,
                        erFremstiltKravForPerson = personerFremstiltKravFor.contains(aktør),
                        nåMåned = nåMåned,
                    )

                val erEndringIVilkårsvurderingForPerson =
                    !behandling.erFinnmarksEllerSvalbardtillegg() &&
                        erEndringIVilkårsvurderingForPerson(
                            aktør = aktør,
                            nåværendePersonResultat = nåværendePersonResultat,
                            forrigePersonResultat = forrigePersonResultat,
                            nåværendeAndeler = nåværendeAndeler,
                            personerIBehandling = personerIBehandling,
                            personerIForrigeBehandling = personerIForrigeBehandling,
                            erEndringIBeløpForPerson = erEndringIBeløpForPerson,
                            featureToggleService = featureToggleService,
                        )

                val erEndringIKompetanseForPerson =
                    erEndringIKompetanseForPerson(
                        aktør = aktør,
                        nåværendeKompetanser = nåværendeKompetanser,
                        forrigeKompetanser = forrigeKompetanser,
                    )

                val erEndringIUtenlandskPeriodebeløpForPerson =
                    erEndringIUtenlandskPeriodebeløpForPerson(
                        aktør = aktør,
                        nåværendeUtenlandskPeriodebeløp = nåværendeUtenlandskPeriodebeløp,
                        forrigeUtenlandskPeriodebeløp = forrigeUtenlandskPeriodebeløp,
                    )

                val erEndringIEndretUtbetalingAndelerForPerson =
                    erEndringIEndretUtbetalingAndelerForPerson(
                        aktør = aktør,
                        nåværendeEndretAndeler = nåværendeEndretAndeler,
                        forrigeEndretAndeler = forrigeEndretAndeler,
                    )

                val erMinstEnEndringForPerson =
                    erEndringIBeløpForPerson ||
                        erEndringIKompetanseForPerson ||
                        erEndringIUtenlandskPeriodebeløpForPerson ||
                        erEndringIVilkårsvurderingForPerson ||
                        erEndringIEndretUtbetalingAndelerForPerson

                if (erMinstEnEndringForPerson) {
                    loggEndringerVedEndringForPerson(
                        erEndringIBeløpForPerson = erEndringIBeløpForPerson,
                        erEndringIKompetanseForPerson = erEndringIKompetanseForPerson,
                        erEndringIUtenlandskPeriodebeløpForPerson = erEndringIUtenlandskPeriodebeløpForPerson,
                        erEndringIVilkårsvurderingForPerson = erEndringIVilkårsvurderingForPerson,
                        erEndringIEndretUtbetalingAndelerForPerson = erEndringIEndretUtbetalingAndelerForPerson,
                        aktør = aktør,
                        nåværendeAndeler = nåværendeAndeler,
                        nåværendeKompetanser = nåværendeKompetanser,
                        nåværendeUtenlandskPeriodebeløp = nåværendeUtenlandskPeriodebeløp,
                        nåværendePersonResultat = nåværendePersonResultat,
                        nåværendeEndretAndeler = nåværendeEndretAndeler,
                    )
                }

                EndringForPerson(
                    erEndring = erMinstEnEndringForPerson,
                    erBeløpsEndring = erEndringIBeløpForPerson,
                )
            }

        return when {
            endringsInfoForRelevantePersoner.none { it.erEndring } -> {
                Endringsresultat.INGEN_ENDRING
            }

            endringsInfoForRelevantePersoner.any { it.erBeløpsEndring } -> {
                Endringsresultat.ENDRING
            }

            else -> {
                Endringsresultat.ENDRING_UTEN_BELØPSENDRING
            }
        }
    }

    private fun loggEndringerVedEndringForPerson(
        erEndringIBeløpForPerson: Boolean,
        erEndringIKompetanseForPerson: Boolean,
        erEndringIUtenlandskPeriodebeløpForPerson: Boolean,
        erEndringIVilkårsvurderingForPerson: Boolean,
        erEndringIEndretUtbetalingAndelerForPerson: Boolean,
        aktør: Aktør,
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        nåværendeKompetanser: List<Kompetanse>,
        nåværendeUtenlandskPeriodebeløp: List<UtenlandskPeriodebeløp>,
        nåværendePersonResultat: Set<PersonResultat>,
        nåværendeEndretAndeler: List<EndretUtbetalingAndel>,
    ) {
        logger.info(
            "Endringer: " +
                "erEndringIBeløp=$erEndringIBeløpForPerson for aktør ${aktør.aktørId}," +
                "erEndringIKompetanse=$erEndringIKompetanseForPerson for aktør ${aktør.aktørId}, " +
                "erEndringIUtenlandskPeriodebeløpForPerson=$erEndringIUtenlandskPeriodebeløpForPerson for aktør ${aktør.aktørId}," +
                "erEndringIVilkårsvurdering=$erEndringIVilkårsvurderingForPerson for aktør ${aktør.aktørId}, " +
                "erEndringIEndretUtbetalingAndeler=$erEndringIEndretUtbetalingAndelerForPerson for aktør ${aktør.aktørId}",
        )

        val endredeAndelTilkjentYtelseForPerson = if (erEndringIBeløpForPerson) "nye AndelerTilkjentYtelse for aktør ${aktør.aktørId}: $nåværendeAndeler , " else ""
        val endredeKompetanserForPerson = if (erEndringIKompetanseForPerson) "nye kompetanser for aktør ${aktør.aktørId}: $nåværendeKompetanser ," else ""
        val endredeUtenlandskPeriodebeløpForPerson = if (erEndringIUtenlandskPeriodebeløpForPerson) "nye utenlandsk periodebeløp for aktør ${aktør.aktørId}: $nåværendeUtenlandskPeriodebeløp ," else ""
        val endredeVilkårsvurderingerForPerson = if (erEndringIVilkårsvurderingForPerson) "nye personresultater for aktør ${aktør.aktørId}: $nåværendePersonResultat ," else ""
        val endredeEndretUtbetalingAndelerForPerson = if (erEndringIEndretUtbetalingAndelerForPerson) "nye endretUtbetalingAndeler for aktør ${aktør.aktørId}: $nåværendeEndretAndeler" else ""

        secureLogger.info(
            "Endringer: $endredeAndelTilkjentYtelseForPerson $endredeKompetanserForPerson $endredeUtenlandskPeriodebeløpForPerson $endredeVilkårsvurderingerForPerson $endredeEndretUtbetalingAndelerForPerson",
        )
    }

    // NB: For personer fremstilt krav for tar vi ikke hensyn til alle endringer i beløp i denne funksjonen
    internal fun erEndringIBeløpForPerson(
        aktør: Aktør,
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
        nåværendeEndretAndeler: List<EndretUtbetalingAndel>,
        forrigeEndretAndeler: List<EndretUtbetalingAndel>,
        erFremstiltKravForPerson: Boolean,
        nåMåned: YearMonth,
    ): Boolean {
        val opphørstidspunktForBehandling =
            nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
                forrigeAndelerIBehandling = forrigeAndeler,
                nåværendeEndretAndelerIBehandling = nåværendeEndretAndeler,
                endretAndelerForForrigeBehandling = forrigeEndretAndeler,
            )

        if (opphørstidspunktForBehandling == null) {
            return false // Verken forrige eller nåværende behandling har andeler
        }

        val nåværendeAndelerForPerson = nåværendeAndeler.filter { it.aktør == aktør }
        val forrigeAndelerForPerson = forrigeAndeler.filter { it.aktør == aktør }

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
    aktør: Aktør,
    nåværendeKompetanser: List<Kompetanse>,
    forrigeKompetanser: List<Kompetanse>,
): Boolean {
    val nåværendeKompetanserForPerson = nåværendeKompetanser.filter { it.barnAktører.contains(aktør) }
    val forrigeKompetanserForPerson = forrigeKompetanser.filter { it.barnAktører.contains(aktør) }

    val endringIKompetanseTidslinje =
        EndringIKompetanseUtil.lagEndringIKompetanseForPersonTidslinje(
            nåværendeKompetanserForPerson = nåværendeKompetanserForPerson,
            forrigeKompetanserForPerson = forrigeKompetanserForPerson,
        )

    return endringIKompetanseTidslinje.tilPerioder().any { it.verdi == true }
}

internal fun erEndringIUtenlandskPeriodebeløpForPerson(
    aktør: Aktør,
    nåværendeUtenlandskPeriodebeløp: List<UtenlandskPeriodebeløp>,
    forrigeUtenlandskPeriodebeløp: List<UtenlandskPeriodebeløp>,
): Boolean {
    val nåværendeUtenlandskPeriodebeløpForPerson = nåværendeUtenlandskPeriodebeløp.filter { it.barnAktører.contains(aktør) }
    val forrigeUtenlandskPeriodebeløpForPerson = forrigeUtenlandskPeriodebeløp.filter { it.barnAktører.contains(aktør) }

    val endringIUtenlandskPeriodebeløpTidslinje =
        EndringIUtenlandskPeriodebeløpUtil.lagEndringIUtenlandskPeriodebeløpForPersonTidslinje(
            nåværendeUtenlandskPeriodebeløpForPerson = nåværendeUtenlandskPeriodebeløpForPerson,
            forrigeUtenlandskPeriodebeløpForPerson = forrigeUtenlandskPeriodebeløpForPerson,
        )

    return endringIUtenlandskPeriodebeløpTidslinje.tilPerioder().any { it.verdi == true }
}

internal fun erEndringIVilkårsvurderingForPerson(
    aktør: Aktør,
    nåværendePersonResultat: Set<PersonResultat>,
    forrigePersonResultat: Set<PersonResultat>,
    nåværendeAndeler: List<AndelTilkjentYtelse>,
    personerIBehandling: Set<Person>,
    personerIForrigeBehandling: Set<Person>,
    featureToggleService: FeatureToggleService,
    erEndringIBeløpForPerson: Boolean,
): Boolean {
    val tidligsteRelevanteFomDatoForPersonIVilkårsvurdering =
        if (erEndringIBeløpForPerson) {
            TIDENES_MORGEN.toYearMonth()
        } else {
            nåværendeAndeler.filter { it.aktør == aktør }.minOfOrNull { it.stønadFom }?.minusMonths(1) ?: TIDENES_MORGEN.toYearMonth()
        }

    val nåværendePersonResultatForPerson = nåværendePersonResultat.singleOrNull { it.aktør == aktør }
    val forrigePersonResultatForPerson = forrigePersonResultat.singleOrNull { it.aktør == aktør }
    val personIBehandling = personerIBehandling.singleOrNull { it.aktør == aktør }
    val personIForrigeBehandling = personerIForrigeBehandling.singleOrNull { it.aktør == aktør }

    val endringIVilkårsvurderingTidslinje =
        EndringIVilkårsvurderingUtil.lagEndringIVilkårsvurderingTidslinje(
            tidligsteRelevanteFomDatoForPersonIVilkårsvurdering = tidligsteRelevanteFomDatoForPersonIVilkårsvurdering,
            nåværendePersonResultaterForPerson = nåværendePersonResultatForPerson,
            forrigePersonResultater = forrigePersonResultatForPerson,
            personIBehandling = personIBehandling,
            personIForrigeBehandling = personIForrigeBehandling,
            featureToggleService = featureToggleService,
        )

    return endringIVilkårsvurderingTidslinje.tilPerioder().any { it.verdi == true }
}

internal fun erEndringIEndretUtbetalingAndelerForPerson(
    aktør: Aktør,
    nåværendeEndretAndeler: List<EndretUtbetalingAndel>,
    forrigeEndretAndeler: List<EndretUtbetalingAndel>,
): Boolean {
    val nåværendeEndretAndelerForPerson = nåværendeEndretAndeler.filter { it.personer.any { person -> person.aktør == aktør } }
    val forrigeEndretAndelerForPerson = forrigeEndretAndeler.filter { it.personer.any { person -> person.aktør == aktør } }

    val endringIEndretUtbetalingAndelTidslinje =
        EndringIEndretUtbetalingAndelUtil.lagEndringIEndretUbetalingAndelPerPersonTidslinje(
            nåværendeEndretAndelerForPerson = nåværendeEndretAndelerForPerson,
            forrigeEndretAndelerForPerson = forrigeEndretAndelerForPerson,
        )

    return endringIEndretUtbetalingAndelTidslinje.tilPerioder().any { it.verdi == true }
}
