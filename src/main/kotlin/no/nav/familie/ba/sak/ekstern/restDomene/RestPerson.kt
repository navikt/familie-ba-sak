package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.oppholdsadresse.GrOppholdsadresse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilTidslinje
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.beskjærEtter
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.time.LocalDate

/**
 * NB: Bør ikke brukes internt, men kun ut mot eksterne tjenester siden klassen
 * inneholder aktiv personIdent og ikke aktørId.
 */
data class RestPerson(
    val type: PersonType,
    val fødselsdato: LocalDate?,
    val personIdent: String,
    val navn: String,
    val kjønn: Kjønn,
    val registerhistorikk: RestRegisterhistorikk? = null,
    val målform: Målform,
    val dødsfallDato: LocalDate? = null,
    val erManueltLagtTilISøknad: Boolean? = null,
)

fun List<Person>?.tilManglendeSvalbardmerkingPerioder(personResultater: Set<PersonResultat>?): List<RestManglendeSvalbardmerking> {
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
                .map { RestManglendeSvalbardmerkingPeriode(fom = it.fom, tom = it.tom) }

        if (perioderMedManglendeSvalbardMerking.isEmpty()) return@mapNotNull null

        RestManglendeSvalbardmerking(fnr, perioderMedManglendeSvalbardMerking)
    }
}

fun Person.tilRestPerson(erManueltLagtTilISøknad: Boolean? = null): RestPerson =
    RestPerson(
        type = this.type,
        fødselsdato = this.fødselsdato,
        personIdent = this.aktør.aktivFødselsnummer(),
        navn = this.navn,
        kjønn = this.kjønn,
        registerhistorikk = this.tilRestRegisterhistorikk(),
        målform = this.målform,
        dødsfallDato = this.dødsfall?.dødsfallDato,
        erManueltLagtTilISøknad = erManueltLagtTilISøknad,
    )
