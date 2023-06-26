package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringUtil.tilFørsteEndringstidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNullMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvetTidslinjeForOppfyltVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import java.time.YearMonth

object EndringIVilkårsvurderingUtil {

    fun utledEndringstidspunktForVilkårsvurdering(
        nåværendePersonResultat: Set<PersonResultat>,
        forrigePersonResultat: Set<PersonResultat>,
    ): YearMonth? {
        val endringIVilkårsvurderingTidslinje = lagEndringIVilkårsvurderingTidslinje(
            nåværendePersonResultater = nåværendePersonResultat,
            forrigePersonResultater = forrigePersonResultat,
        )

        return endringIVilkårsvurderingTidslinje.tilFørsteEndringstidspunkt()
    }

    fun lagEndringIVilkårsvurderingTidslinje(
        nåværendePersonResultater: Set<PersonResultat>,
        forrigePersonResultater: Set<PersonResultat>,
    ): Tidslinje<Boolean, Måned> {
        val allePersonerMedPersonResultat =
            (nåværendePersonResultater.map { it.aktør } + forrigePersonResultater.map { it.aktør }).distinct()

        val tidslinjerPerPersonOgVilkår = allePersonerMedPersonResultat.flatMap { aktør ->
            Vilkår.values().map { vilkår ->
                lagEndringIVilkårsvurderingForPersonOgVilkårTidslinje(
                    nåværendeOppfylteVilkårResultater = nåværendePersonResultater
                        .filter { it.aktør == aktør }
                        .flatMap { it.vilkårResultater }
                        .filter { it.vilkårType == vilkår && it.resultat == Resultat.OPPFYLT },
                    forrigeOppfylteVilkårResultater = forrigePersonResultater
                        .filter { it.aktør == aktør }
                        .flatMap { it.vilkårResultater }
                        .filter { it.vilkårType == vilkår && it.resultat == Resultat.OPPFYLT },
                    vilkår = vilkår,
                )
            }
        }

        return tidslinjerPerPersonOgVilkår.kombiner { finnesMinstEnEndringIPeriode(it) }
    }

    private fun finnesMinstEnEndringIPeriode(
        endringer: Iterable<Boolean>,
    ): Boolean = endringer.any { it }

    // Relevante endringer er
    // 1. Endringer i utdypende vilkårsvurdering
    // 2. Endringer i regelverk
    // 3. Splitt i vilkårsvurderingen
    private fun lagEndringIVilkårsvurderingForPersonOgVilkårTidslinje(
        nåværendeOppfylteVilkårResultater: List<VilkårResultat>,
        forrigeOppfylteVilkårResultater: List<VilkårResultat>,
        vilkår: Vilkår,
    ): Tidslinje<Boolean, Måned> {
        // Antar fødselsdato er første oppfylte fom for 18-årsvilkåret.
        // Denne koden er ikke i bruk i prod og skal fjernes når denne toggelen:
        // https://unleash.nais.io/#/features/strategies/familie-ba-sak.endringstidspunkt
        // har levd lenge nok
        val nåværendeVilkårResultatTidslinje = nåværendeOppfylteVilkårResultater.tilForskjøvetTidslinjeForOppfyltVilkår(vilkår, nåværendeOppfylteVilkårResultater.mapNotNull { it.periodeFom }.minOrNull())
        val tidligereVilkårResultatTidslinje = forrigeOppfylteVilkårResultater.tilForskjøvetTidslinjeForOppfyltVilkår(vilkår, forrigeOppfylteVilkårResultater.mapNotNull { it.periodeFom }.minOrNull())

        val endringIVilkårResultat =
            nåværendeVilkårResultatTidslinje.kombinerUtenNullMed(tidligereVilkårResultatTidslinje) { nåværende, forrige ->

                val erEndringerIUtdypendeVilkårsvurdering = nåværende.utdypendeVilkårsvurderinger.toSet() != forrige.utdypendeVilkårsvurderinger.toSet()
                val erEndringerIRegelverk = nåværende.vurderesEtter != forrige.vurderesEtter
                val erVilkårSomErSplittetOpp = nåværende.periodeFom != forrige.periodeFom

                (forrige.obligatoriskUtdypendeVilkårsvurderingErSatt() && erEndringerIUtdypendeVilkårsvurdering) ||
                    erEndringerIRegelverk ||
                    erVilkårSomErSplittetOpp
            }

        return endringIVilkårResultat
    }

    private fun VilkårResultat.obligatoriskUtdypendeVilkårsvurderingErSatt(): Boolean {
        return this.utdypendeVilkårsvurderinger.isNotEmpty() || !this.utdypendeVilkårsvurderingErObligatorisk()
    }

    private fun VilkårResultat.utdypendeVilkårsvurderingErObligatorisk(): Boolean {
        return if (this.vurderesEtter == Regelverk.NASJONALE_REGLER) {
            false
        } else {
            when (this.vilkårType) {
                Vilkår.BOSATT_I_RIKET,
                Vilkår.BOR_MED_SØKER,
                -> true
                Vilkår.UNDER_18_ÅR,
                Vilkår.LOVLIG_OPPHOLD,
                Vilkår.GIFT_PARTNERSKAP,
                Vilkår.UTVIDET_BARNETRYGD,
                -> false
            }
        }
    }
}
