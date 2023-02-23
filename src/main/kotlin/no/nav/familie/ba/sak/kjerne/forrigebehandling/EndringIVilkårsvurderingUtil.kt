package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringUtil.tilFørsteEndringstidspunktForDagtidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNullMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Dag
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilTidslinje
import java.time.LocalDate
import java.time.YearMonth

object EndringIVilkårsvurderingUtil {

    fun utledEndringstidspunktForVilkårsvurdering(
        nåværendePersonResultat: Set<PersonResultat>,
        forrigePersonResultat: Set<PersonResultat>,
        opphørstidspunkt: YearMonth
    ): YearMonth? {
        val endringIVilkårsvurderingTidslinje = lagEndringIVilkårsvurderingTidslinje(
            nåværendePersonResultat = nåværendePersonResultat,
            forrigePersonResultat = forrigePersonResultat,
            opphørstidspunkt = opphørstidspunkt
        )

        return endringIVilkårsvurderingTidslinje.tilFørsteEndringstidspunktForDagtidslinje()
    }

    fun lagEndringIVilkårsvurderingTidslinje(
        nåværendePersonResultat: Set<PersonResultat>,
        forrigePersonResultat: Set<PersonResultat>,
        opphørstidspunkt: YearMonth
    ): Tidslinje<Boolean, Dag> {
        val allePersonerMedPersonResultat =
            (nåværendePersonResultat.map { it.aktør } + forrigePersonResultat.map { it.aktør }).distinct()

        val tidslinjerPerPersonOgVilkår = allePersonerMedPersonResultat.flatMap { aktør ->
            Vilkår.values().map { vilkår ->
                lagEndringIVilkårsvurderingForPersonOgVilkårTidslinje(
                    nåværendePersonResultat
                        .filter { it.aktør == aktør }
                        .flatMap { it.vilkårResultater }
                        .filter { it.vilkårType == vilkår && it.resultat == Resultat.OPPFYLT },
                    forrigePersonResultat
                        .filter { it.aktør == aktør }
                        .flatMap { it.vilkårResultater }
                        .filter { it.vilkårType == vilkår && it.resultat == Resultat.OPPFYLT },
                    opphørstidspunkt = opphørstidspunkt
                )
            }
        }

        return tidslinjerPerPersonOgVilkår.kombiner { finnesMinstEnEndringIPeriode(it) }
    }

    private fun finnesMinstEnEndringIPeriode(
        endringer: Iterable<Boolean>
    ): Boolean = endringer.any { it }

    // Relevante endringer er
    // 1. Endringer i utdypende vilkårsvurdering
    // 2. Endringer i regelverk
    // 3. Splitt i vilkårsvurderingen
    private fun lagEndringIVilkårsvurderingForPersonOgVilkårTidslinje(
        nåværendeVilkårResultat: List<VilkårResultat>,
        forrigeVilkårResultat: List<VilkårResultat>,
        opphørstidspunkt: YearMonth
    ): Tidslinje<Boolean, Dag> {
        val nåværendeVilkårResultatTidslinje = nåværendeVilkårResultat.tilTidslinje()
        val tidligereVilkårResultatTidslinje = forrigeVilkårResultat.tilTidslinje()

        val endringIVilkårResultat =
            nåværendeVilkårResultatTidslinje.kombinerUtenNullMed(tidligereVilkårResultatTidslinje) { nåværende, forrige ->

                nåværende.utdypendeVilkårsvurderinger.toSet() != forrige.utdypendeVilkårsvurderinger.toSet() ||
                    nåværende.vurderesEtter != forrige.vurderesEtter ||
                    nåværende.periodeFom != forrige.periodeFom ||
                    (nåværende.periodeTom != forrige.periodeTom && nåværende.periodeTom.førerIkkeTilOpphør(opphørstidspunkt))
            }

        return endringIVilkårResultat
    }

    private fun LocalDate?.førerIkkeTilOpphør(opphørstidspunkt: YearMonth): Boolean = this?.isBefore(opphørstidspunkt.minusMonths(1).førsteDagIInneværendeMåned()) == true
}
