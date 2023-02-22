package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNullMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Dag
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilTidslinje
import java.time.LocalDate
import java.time.YearMonth

class EndringIVilkårsvurderingUtil {

    // Relevante endringer er
    // 1. Endringer i utdypende vilkårsvurdering
    // 2. Endringer i regelverk
    // 3. Splitt i vilkårsvurderingen
    fun lagEndringIVilkårsvurderingForPersonOgVilkårTidslinje(
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
