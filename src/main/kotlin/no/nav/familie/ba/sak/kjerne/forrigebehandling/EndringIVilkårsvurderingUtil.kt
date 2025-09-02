package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNullMed
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvetTidslinjeForOppfyltVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import java.time.YearMonth

object EndringIVilkårsvurderingUtil {
    fun lagEndringIVilkårsvurderingTidslinje(
        nåværendePersonResultaterForPerson: Set<PersonResultat>,
        forrigePersonResultater: Set<PersonResultat>,
        personIBehandling: Person?,
        personIForrigeBehandling: Person?,
        tidligsteRelevanteFomDatoForPersonIVilkårsvurdering: YearMonth,
    ): Tidslinje<Boolean> {
        val tidslinjePerVilkår =
            Vilkår.entries.map { vilkår ->
                val vilkårTidslinje =
                    lagEndringIVilkårsvurderingForPersonOgVilkårTidslinje(
                        nåværendeOppfylteVilkårResultaterForPerson =
                            nåværendePersonResultaterForPerson
                                .flatMap { it.vilkårResultater }
                                .filter { it.vilkårType == vilkår && it.resultat == Resultat.OPPFYLT },
                        forrigeOppfylteVilkårResultaterForPerson =
                            forrigePersonResultater
                                .flatMap { it.vilkårResultater }
                                .filter { it.vilkårType == vilkår && it.resultat == Resultat.OPPFYLT },
                        vilkår = vilkår,
                        personIBehandling = personIBehandling,
                        personIForrigeBehandling = personIForrigeBehandling,
                    )
                vilkårTidslinje.beskjærFraOgMed(fraOgMed = tidligsteRelevanteFomDatoForPersonIVilkårsvurdering.førsteDagIInneværendeMåned())
            }

        return tidslinjePerVilkår.kombiner { finnesMinstEnEndringIPeriode(it) }
    }

    private fun finnesMinstEnEndringIPeriode(
        endringer: Iterable<Boolean>,
    ): Boolean = endringer.any { it }

    // Relevante endringer er
    // 1. Endringer i utdypende vilkårsvurdering
    // 2. Endringer i regelverk
    // 3. Splitt i vilkårsvurderingen
    private fun lagEndringIVilkårsvurderingForPersonOgVilkårTidslinje(
        nåværendeOppfylteVilkårResultaterForPerson: List<VilkårResultat>,
        forrigeOppfylteVilkårResultaterForPerson: List<VilkårResultat>,
        vilkår: Vilkår,
        personIBehandling: Person?,
        personIForrigeBehandling: Person?,
    ): Tidslinje<Boolean> {
        val nåværendeVilkårResultatTidslinje =
            nåværendeOppfylteVilkårResultaterForPerson
                .tilForskjøvetTidslinjeForOppfyltVilkår(vilkår = vilkår, fødselsdato = personIBehandling?.fødselsdato)

        val tidligereVilkårResultatTidslinje =
            forrigeOppfylteVilkårResultaterForPerson
                .tilForskjøvetTidslinjeForOppfyltVilkår(vilkår = vilkår, fødselsdato = personIForrigeBehandling?.fødselsdato)

        val endringIVilkårResultat =
            nåværendeVilkårResultatTidslinje.kombinerUtenNullMed(tidligereVilkårResultatTidslinje) { nåværende, forrige ->

                val erEndringerIUtdypendeVilkårsvurdering =
                    nåværende.relevanteUtdypendeVilkårsvurderinger() != forrige.relevanteUtdypendeVilkårsvurderinger()
                val erEndringerIRegelverk = nåværende.vurderesEtter != forrige.vurderesEtter
                val erVilkårSomErSplittetOpp = nåværende.periodeFom != forrige.periodeFom

                (forrige.obligatoriskUtdypendeVilkårsvurderingErSatt() && erEndringerIUtdypendeVilkårsvurdering) ||
                    erEndringerIRegelverk ||
                    erVilkårSomErSplittetOpp
            }

        return endringIVilkårResultat
    }

    private fun VilkårResultat.obligatoriskUtdypendeVilkårsvurderingErSatt(): Boolean = relevanteUtdypendeVilkårsvurderinger().isNotEmpty() || !this.utdypendeVilkårsvurderingErObligatorisk()

    private fun VilkårResultat.relevanteUtdypendeVilkårsvurderinger(): Set<UtdypendeVilkårsvurdering> = utdypendeVilkårsvurderinger.filterNot { it in setOf(BOSATT_I_FINNMARK_NORD_TROMS, BOSATT_PÅ_SVALBARD) }.toSet()

    private fun VilkårResultat.utdypendeVilkårsvurderingErObligatorisk(): Boolean =
        if (this.vurderesEtter == Regelverk.NASJONALE_REGLER) {
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
