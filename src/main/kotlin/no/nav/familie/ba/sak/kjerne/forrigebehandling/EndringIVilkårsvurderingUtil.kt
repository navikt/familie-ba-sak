package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringUtil.tilFørsteEndringstidspunktForDagtidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNullMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Dag
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilTidslinje
import java.time.YearMonth

object EndringIVilkårsvurderingUtil {

    fun utledEndringstidspunktForVilkårsvurdering(
        nåværendePersonResultat: Set<PersonResultat>,
        forrigePersonResultat: Set<PersonResultat>
    ): YearMonth? {
        val endringIVilkårsvurderingTidslinje = lagEndringIVilkårsvurderingTidslinje(
            nåværendePersonResultat = nåværendePersonResultat,
            forrigePersonResultat = forrigePersonResultat
        )

        return endringIVilkårsvurderingTidslinje.tilFørsteEndringstidspunktForDagtidslinje()
    }

    fun lagEndringIVilkårsvurderingTidslinje(
        nåværendePersonResultat: Set<PersonResultat>,
        forrigePersonResultat: Set<PersonResultat>
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
                        .filter { it.vilkårType == vilkår && it.resultat == Resultat.OPPFYLT }
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
        forrigeVilkårResultat: List<VilkårResultat>
    ): Tidslinje<Boolean, Dag> {
        val nåværendeVilkårResultatTidslinje = nåværendeVilkårResultat.tilTidslinje()
        val tidligereVilkårResultatTidslinje = forrigeVilkårResultat.tilTidslinje()

        val endringIVilkårResultat =
            nåværendeVilkårResultatTidslinje.kombinerUtenNullMed(tidligereVilkårResultatTidslinje) { nåværende, forrige ->

                (forrige.erObligatoriskeFelterSatt() && nåværende.utdypendeVilkårsvurderinger.toSet() != forrige.utdypendeVilkårsvurderinger.toSet()) ||
                    nåværende.vurderesEtter != forrige.vurderesEtter ||
                    nåværende.periodeFom != forrige.periodeFom
            }

        return endringIVilkårResultat
    }

    private fun VilkårResultat.erObligatoriskeFelterSatt(): Boolean {
        return (this.utdypendeVilkårsvurderingErObligatorisk() && this.utdypendeVilkårsvurderinger.isNotEmpty()) || !this.utdypendeVilkårsvurderingErObligatorisk()
    }

    private fun VilkårResultat.utdypendeVilkårsvurderingErObligatorisk(): Boolean {
        return if (this.vurderesEtter == Regelverk.NASJONALE_REGLER) {
            false
        } else {
            when (this.vilkårType) {
                Vilkår.BOSATT_I_RIKET,
                Vilkår.BOR_MED_SØKER -> true
                Vilkår.UNDER_18_ÅR,
                Vilkår.LOVLIG_OPPHOLD,
                Vilkår.GIFT_PARTNERSKAP,
                Vilkår.UTVIDET_BARNETRYGD -> false
            }
        }
    }
}
