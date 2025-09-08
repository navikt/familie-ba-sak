package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.common.Utils.tilEtterfølgendePar
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.oppholdsadresse.GrOppholdsadresse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilTidslinje
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.beskjærEtter
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.time.LocalDate

data class ManglendeSvalbardmerkingDto(
    val ident: String,
    val manglendeSvalbardmerkingPerioder: List<ManglendeSvalbardmerkingPeriodeDto>,
)

data class ManglendeSvalbardmerkingPeriodeDto(
    val fom: LocalDate?,
    val tom: LocalDate?,
)

fun List<Person>?.tilManglendeSvalbardmerkingPerioder(personResultater: Set<PersonResultat>?): List<ManglendeSvalbardmerkingDto> {
    if (this == null || personResultater == null) return emptyList()

    val bosattIRiketVilkårTidslinjePerPerson: Map<String, Tidslinje<VilkårResultat>> = personResultater.associate { it.aktør.aktivFødselsnummer() to it.vilkårResultater.filter { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.BOSATT_I_RIKET && vilkårResultat.periodeFom != null }.tilTidslinje() }
    val svalbardOppholdTidslinjerPerPerson: Map<String, Tidslinje<GrOppholdsadresse>> = this.associate { it.aktør.aktivFødselsnummer() to it.oppholdsadresser.tilSvalbardOppholdTidslinje() }

    return bosattIRiketVilkårTidslinjePerPerson.mapNotNull { (fnr, bosattIRiketVilkårTidslinje) ->
        val svalbardOppholdTidslinje = svalbardOppholdTidslinjerPerPerson[fnr] ?: return@mapNotNull null

        // Svalbard opphold utenfor perioder med bosatt i riket vurderes ikke
        val beskjærtSvalbardOppholdTidslinje = svalbardOppholdTidslinje.beskjærEtter(bosattIRiketVilkårTidslinje)

        val perioderMedManglendeSvalbardMerking =
            bosattIRiketVilkårTidslinje
                .kombinerMed(beskjærtSvalbardOppholdTidslinje) { bosattIRiketVilkår, grOppholdsadresse ->
                    grOppholdsadresse != null && bosattIRiketVilkår != null && !bosattIRiketVilkår.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)
                }.tilPerioderIkkeNull()
                .filter { it.verdi }
                .map { ManglendeSvalbardmerkingPeriodeDto(fom = it.fom, tom = it.tom) }

        if (perioderMedManglendeSvalbardMerking.isEmpty()) return@mapNotNull null

        ManglendeSvalbardmerkingDto(fnr, perioderMedManglendeSvalbardMerking)
    }
}

fun List<GrOppholdsadresse>.tilSvalbardOppholdTidslinje(): Tidslinje<GrOppholdsadresse> =
    this
        .filter { it.erPåSvalbard() }
        .sortedBy { it.periode?.fom }
        .tilEtterfølgendePar { grOppholdsadresse, nesteGrOppholdsadresse ->
            Periode(
                verdi = grOppholdsadresse,
                fom = grOppholdsadresse.periode?.fom,
                tom = grOppholdsadresse.periode?.tom ?: nesteGrOppholdsadresse?.periode?.fom?.minusDays(1),
            )
        }.tilTidslinje()
