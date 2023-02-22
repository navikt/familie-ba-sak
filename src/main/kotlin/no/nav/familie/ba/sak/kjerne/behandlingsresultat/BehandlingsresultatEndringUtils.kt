package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatOpphørUtils.utledOpphørsdatoForNåværendeBehandlingMedFallback
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.EndretUtbetalingAndelTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIKompetanseUtil
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIVilkårsvurderingUtil
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
import java.time.YearMonth

internal enum class Endringsresultat {
    ENDRING,
    INGEN_ENDRING
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
        forrigeEndretAndeler: List<EndretUtbetalingAndel>
    ): Endringsresultat {
        val erEndringIBeløp = erEndringIBeløp(
            nåværendeAndeler = nåværendeAndeler,
            forrigeAndeler = forrigeAndeler,
            nåværendeEndretAndeler = nåværendeEndretAndeler,
            personerFremstiltKravFor = personerFremstiltKravFor
        )

        val erEndringIKompetanse = erEndringIKompetanse(
            nåværendeKompetanser = nåværendeKompetanser,
            forrigeKompetanser = forrigeKompetanser
        )

        val erEndringIVilkårsvurdering = erEndringIVilkårvurdering(
            nåværendePersonResultat = nåværendePersonResultat,
            forrigePersonResultat = forrigePersonResultat,
            opphørstidspunkt = nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(forrigeAndeler = forrigeAndeler, nåværendeEndretAndeler = nåværendeEndretAndeler)
        )

        val erEndringIEndretUtbetalingAndeler = erEndringIEndretUtbetalingAndeler(
            nåværendeEndretAndeler = nåværendeEndretAndeler,
            forrigeEndretAndeler = forrigeEndretAndeler
        )

        val erMinstEnEndring = erEndringIBeløp || erEndringIKompetanse || erEndringIVilkårsvurdering || erEndringIEndretUtbetalingAndeler

        return if (erMinstEnEndring) Endringsresultat.ENDRING else Endringsresultat.INGEN_ENDRING
    }

    // NB: For personer fremstilt krav for tar vi ikke hensyn til alle endringer i beløp i denne funksjonen
    internal fun erEndringIBeløp(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        nåværendeEndretAndeler: List<EndretUtbetalingAndel>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
        personerFremstiltKravFor: List<Aktør>
    ): Boolean {
        val allePersonerMedAndeler = (nåværendeAndeler.map { it.aktør } + forrigeAndeler.map { it.aktør }).distinct()
        val opphørstidspunkt = nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
            forrigeAndeler = forrigeAndeler,
            nåværendeEndretAndeler = nåværendeEndretAndeler
        ) ?: return false // Returnerer false hvis verken forrige eller nåværende behandling har andeler

        val erEndringIBeløpForMinstEnPerson = allePersonerMedAndeler.any { aktør ->
            val ytelseTyperForPerson = (nåværendeAndeler.map { it.type } + forrigeAndeler.map { it.type }).distinct()

            ytelseTyperForPerson.any { ytelseType ->
                erEndringIBeløpForPersonOgType(
                    nåværendeAndeler = nåværendeAndeler.filter { it.aktør == aktør && it.type == ytelseType },
                    forrigeAndeler = forrigeAndeler.filter { it.aktør == aktør && it.type == ytelseType },
                    opphørstidspunkt = opphørstidspunkt,
                    erFremstiltKravForPerson = personerFremstiltKravFor.contains(aktør)
                )
            }
        }

        return erEndringIBeløpForMinstEnPerson
    }

    // Kun interessert i endringer i beløp FØR opphørstidspunkt
    private fun erEndringIBeløpForPersonOgType(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
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
        this.beskjær(fraOgMed = TIDENES_MORGEN.tilMånedTidspunkt(), tilOgMed = opphørstidspunkt.forrigeMåned().tilTidspunkt())

    internal fun erEndringIKompetanse(
        nåværendeKompetanser: List<Kompetanse>,
        forrigeKompetanser: List<Kompetanse>
    ): Boolean {
        val endringIKompetanseTidslinje = EndringIKompetanseUtil().lagEndringIKompetanseTidslinje(
            nåværendeKompetanser = nåværendeKompetanser,
            forrigeKompetanser = forrigeKompetanser
        )

        return endringIKompetanseTidslinje.perioder().any { it.innhold == true }
    }

    fun erEndringIVilkårvurdering(
        nåværendePersonResultat: Set<PersonResultat>,
        forrigePersonResultat: Set<PersonResultat>,
        opphørstidspunkt: YearMonth?
    ): Boolean {
        if (opphørstidspunkt == null) return false // Returnerer false hvis verken forrige eller nåværende behandling har andeler

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
                        .filter { it.vilkårType == vilkårType && it.resultat == Resultat.OPPFYLT },
                    opphørstidspunkt = opphørstidspunkt
                )
            }
        }

        return finnesPersonMedEndretVilkårsvurdering
    }

    fun erEndringIVilkårvurderingForPerson(
        nåværendeVilkårResultat: List<VilkårResultat>,
        forrigeVilkårResultat: List<VilkårResultat>,
        opphørstidspunkt: YearMonth
    ): Boolean {
        val endringIVilkårResultat = EndringIVilkårsvurderingUtil().lagEndringIVilkårsvurderingForPersonOgVilkårTidslinje(
            nåværendeVilkårResultat = nåværendeVilkårResultat,
            forrigeVilkårResultat = forrigeVilkårResultat,
            opphørstidspunkt = opphørstidspunkt
        )

        return endringIVilkårResultat.perioder().any { it.innhold == true }
    }

    internal fun erEndringIEndretUtbetalingAndeler(
        nåværendeEndretAndeler: List<EndretUtbetalingAndel>,
        forrigeEndretAndeler: List<EndretUtbetalingAndel>
    ): Boolean {
        val allePersoner = (nåværendeEndretAndeler.mapNotNull { it.person?.aktør } + forrigeEndretAndeler.mapNotNull { it.person?.aktør }).distinct()

        val finnesPersonerMedEndretEndretUtbetalingAndel = allePersoner.any { aktør ->
            erEndringIEndretUtbetalingAndelPerPerson(
                nåværendeEndretAndelerForPerson = nåværendeEndretAndeler.filter { it.person?.aktør == aktør },
                forrigeEndretAndelerForPerson = forrigeEndretAndeler.filter { it.person?.aktør == aktør }
            )
        }

        return finnesPersonerMedEndretEndretUtbetalingAndel
    }

    private fun erEndringIEndretUtbetalingAndelPerPerson(
        nåværendeEndretAndelerForPerson: List<EndretUtbetalingAndel>,
        forrigeEndretAndelerForPerson: List<EndretUtbetalingAndel>
    ): Boolean {
        val nåværendeTidslinje = EndretUtbetalingAndelTidslinje(nåværendeEndretAndelerForPerson)
        val forrigeTidslinje = EndretUtbetalingAndelTidslinje(forrigeEndretAndelerForPerson)

        val endringerTidslinje = nåværendeTidslinje.kombinerUtenNullMed(forrigeTidslinje) { nåværende, forrige ->
            (
                nåværende.avtaletidspunktDeltBosted != forrige.avtaletidspunktDeltBosted ||
                    nåværende.årsak != forrige.årsak ||
                    nåværende.søknadstidspunkt != forrige.søknadstidspunkt
                )
        }

        return endringerTidslinje.perioder().any { it.innhold == true }
    }
}
