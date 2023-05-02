package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerBeløpOgType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.IUtfyltEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilIEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.UtfyltKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.tilIKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMedNullable
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærEtter
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.mapIkkeNull
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.alleOrdinæreVilkårErOppfylt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvetTidslinjerForHvertOppfylteVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilTidslinjeForSplittForPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

data class GrunnlagForVedtaksperioder(
    val persongrunnlag: PersonopplysningGrunnlag,
    val personResultater: Set<PersonResultat>,
    val fagsakType: FagsakType,
    val kompetanser: List<Kompetanse>,
    val endredeUtbetalinger: List<EndretUtbetalingAndel>,
    val andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    val perioderOvergangsstønad: List<InternPeriodeOvergangsstønad>
) {
    private val utfylteEndredeUtbetalinger = endredeUtbetalinger
        .map { it.tilIEndretUtbetalingAndel() }
        .filterIsInstance<IUtfyltEndretUtbetalingAndel>()

    private val utfylteKompetanser = kompetanser
        .map { it.tilIKompetanse() }
        .filterIsInstance<UtfyltKompetanse>()

    fun utledGrunnlagTidslinjePerPerson(): Map<AktørId, Tidslinje<GrunnlagForPerson, Måned>> {

        val søker = persongrunnlag.søker
        val søkerPersonResultater = personResultater.single { it.aktør == søker.aktør }

        val ordinæreVilkårForSøkerForskjøvetTidslinje = søkerPersonResultater.tilTidslinjeForSplittForPerson(
            personType = søker.type,
            fagsakType = fagsakType
        ).map {
            it?.filtrerVilkårErOrdinærtFor(søker)
        }

        val erOrdinæreVilkårOppfyltForMinstEttBarnTidslinje =
            hentErOrdinæreVilkårOppfyltForMinstEttBarnTidslinje(personResultater, søker, fagsakType)

        val grunnlagForPersonTidslinjer = personResultater
            .associate { personResultat ->
                val aktør = personResultat.aktør
                val person = persongrunnlag.personer.single { person -> aktør == person.aktør }

                val forskjøvedeVilkårResultaterForPersonsAndeler: Tidslinje<List<VilkårResultat>, Måned> =
                    personResultat.hentForskjøvedeVilkårResultaterForPersonsAndelerTidslinje(
                        person = person,
                        erOrdinæreVilkårOppfyltForMinstEttBarnTidslinje = erOrdinæreVilkårOppfyltForMinstEttBarnTidslinje,
                        ordinæreVilkårForSøkerTidslinje = ordinæreVilkårForSøkerForskjøvetTidslinje
                    )

                aktør.aktørId to forskjøvedeVilkårResultaterForPersonsAndeler.tilGrunnlagForPersonTidslinje(person)
            }

        return grunnlagForPersonTidslinjer
    }

    private fun Tidslinje<List<VilkårResultat>, Måned>.tilGrunnlagForPersonTidslinje(
        person: Person
    ): Tidslinje<GrunnlagForPerson, Måned> {
        val harRettPåUtbetalingTidslinje = this.tilHarRettPåUtbetalingTidslinje(person, fagsakType)

        val kompetanseTidslinje = utfylteKompetanser.filtrerPåAktør(person.aktør)
            .tilTidslinje().mapIkkeNull { KompetanseForVedtaksperiode(it) }

        val endredeUtbetalingerTidslinje = utfylteEndredeUtbetalinger.filtrerPåAktør(person.aktør)
            .tilTidslinje().mapIkkeNull { EndretUtbetalingAndelForVedtaksperiode(it) }

        val overgangsstønadTidslinje =
            perioderOvergangsstønad.filtrerPåAktør(person.aktør).tilPeriodeOvergangsstønadForVedtaksperiodeTidslinje()

        val grunnlagTidslinje = harRettPåUtbetalingTidslinje
            .kombinerMed(
                this.tilVilkårResultaterForVedtaksPeriodeTidslinje(),
                andelerTilkjentYtelse.filtrerPåAktør(person.aktør).tilAndelerForVedtaksPeriodeTidslinje()
            ) { erVilkårsvurderingOppfylt, vilkårResultater, andeler ->
                lagGrunnlagForVilkårOgAndel(
                    erVilkårsvurderingOppfylt = erVilkårsvurderingOppfylt,
                    vilkårResultater = vilkårResultater,
                    person = person,
                    andeler = andeler
                )
            }.kombinerMedNullable(kompetanseTidslinje) { grunnlagForPerson, kompetanse ->
                lagGrunnlagMedKompetanse(grunnlagForPerson, kompetanse)
            }.kombinerMedNullable(endredeUtbetalingerTidslinje) { grunnlagForPerson, endretUtbetalingAndel ->
                lagGrunnlagMedEndretUtbetalingAndel(grunnlagForPerson, endretUtbetalingAndel)
            }.kombinerMedNullable(overgangsstønadTidslinje) { grunnlagForPerson, overgangsstønad ->
                lagGrunnlagMedOvergangsstønad(grunnlagForPerson, overgangsstønad)
            }.filtrerIkkeNull()

        return grunnlagTidslinje
            .slåSammenLike()
            .perioder()
            .dropWhile { it.innhold !is GrunnlagForPersonInnvilget }
            .tilTidslinje()
    }
}

private fun List<VilkårResultat>.filtrerVilkårErOrdinærtFor(
    søker: Person
): List<VilkårResultat>? {
    val ordinæreVilkårForSøker = Vilkår.hentOrdinæreVilkårFor(søker.type)

    return this
        .filter { ordinæreVilkårForSøker.contains(it.vilkårType) }
        .takeIf { it.isNotEmpty() }
}

private fun hentErOrdinæreVilkårOppfyltForMinstEttBarnTidslinje(
    personResultater: Set<PersonResultat>,
    søker: Person,
    fagsakType: FagsakType
): Tidslinje<Boolean, Måned> = personResultater
    .filter { it.aktør != søker.aktør || søker.type == PersonType.BARN }
    .map { personResultat ->
        personResultat.tilTidslinjeForSplittForPerson(
            personType = PersonType.BARN,
            fagsakType = fagsakType
        ).map { it != null }
    }.kombiner { it.any() }

private fun PersonResultat.hentForskjøvedeVilkårResultaterForPersonsAndelerTidslinje(
    person: Person,
    erOrdinæreVilkårOppfyltForMinstEttBarnTidslinje: Tidslinje<Boolean, Måned>,
    ordinæreVilkårForSøkerTidslinje: Tidslinje<List<VilkårResultat>, Måned>
): Tidslinje<List<VilkårResultat>, Måned> {
    val forskjøvedeVilkårResultaterForPerson =
        this.vilkårResultater.tilForskjøvetTidslinjerForHvertOppfylteVilkår().kombiner { it }

    return when (person.type) {
        PersonType.SØKER -> forskjøvedeVilkårResultaterForPerson.map { vilkårResultater ->
            vilkårResultater?.filtrerErIkkeOrdinærtFor(person)
        }.beskjærEtter(erOrdinæreVilkårOppfyltForMinstEttBarnTidslinje)

        PersonType.BARN -> forskjøvedeVilkårResultaterForPerson.kombinerMed(
            ordinæreVilkårForSøkerTidslinje.beskjærEtter(forskjøvedeVilkårResultaterForPerson)
        ) { vilkårResultaterBarn, vilkårResultaterSøker ->
            slåSammenHvisMulig(vilkårResultaterBarn, vilkårResultaterSøker)?.toList()
        }

        PersonType.ANNENPART -> throw Feil("Ikke implementert for annenpart")
    }
}

private fun slåSammenHvisMulig(
    venstre: Iterable<VilkårResultat>?,
    høyre: Iterable<VilkårResultat>?
) = when {
    venstre == null -> høyre
    høyre == null -> venstre
    else -> høyre + venstre
}

private fun Iterable<VilkårResultat>.filtrerErIkkeOrdinærtFor(person: Person): List<VilkårResultat>? {
    val ordinæreVilkårForPerson = Vilkår.hentOrdinæreVilkårFor(person.type)

    return this
        .filterNot { ordinæreVilkårForPerson.contains(it.vilkårType) }
        .takeIf { it.isNotEmpty() }
}

private fun lagGrunnlagForVilkårOgAndel(
    erVilkårsvurderingOppfylt: Boolean?,
    vilkårResultater: List<VilkårResultatForVedtaksperiode>?,
    person: Person,
    andeler: Iterable<AndelForVedtaksperiode>?
) = if (erVilkårsvurderingOppfylt == true) {
    GrunnlagForPersonInnvilget(
        vilkårResultaterForVedtaksPeriode = vilkårResultater
            ?: error("vilkårResultatene burde alltid finnes om vi har rett"),
        person = person,
        andeler = andeler
    )
} else {
    GrunnlagForPersonIkkeInnvilget(
        vilkårResultaterForVedtaksPeriode = vilkårResultater ?: emptyList(),
        person = person
    )
}

private fun lagGrunnlagMedKompetanse(
    grunnlagForPerson: GrunnlagForPerson?,
    kompetanse: KompetanseForVedtaksperiode?
) = when (grunnlagForPerson) {
    is GrunnlagForPersonInnvilget -> grunnlagForPerson.copy(kompetanse = kompetanse)
    is GrunnlagForPersonIkkeInnvilget -> {
        if (kompetanse != null) {
            throw Feil("GrunnlagForPersonIkkeInnvilget for aktør ${grunnlagForPerson.person.aktør} kan ikke ha kompetanse siden den ikke er innvilget")
        }
        grunnlagForPerson
    }

    null -> null
}

private fun lagGrunnlagMedEndretUtbetalingAndel(
    grunnlagForPerson: GrunnlagForPerson?,
    endretUtbetalingAndel: EndretUtbetalingAndelForVedtaksperiode?
) = when (grunnlagForPerson) {
    is GrunnlagForPersonInnvilget -> grunnlagForPerson.copy(endretUtbetalingAndel = endretUtbetalingAndel)
    is GrunnlagForPersonIkkeInnvilget -> {
        if (endretUtbetalingAndel != null) {
            throw Feil("GrunnlagForPersonIkkeInnvilget for aktør ${grunnlagForPerson.person.aktør} kan ikke ha endretUtbetalingAndel siden den ikke er innvilget")
        }
        grunnlagForPerson
    }

    null -> null
}

private fun lagGrunnlagMedOvergangsstønad(
    grunnlagForPerson: GrunnlagForPerson?,
    overgangsstønad: OvergangsstønadForVedtaksperiode?
) = when (grunnlagForPerson) {
    is GrunnlagForPersonInnvilget -> grunnlagForPerson.copy(overgangsstønad = overgangsstønad)
    is GrunnlagForPersonIkkeInnvilget -> grunnlagForPerson
    null -> null
}

// TODO: Kan dette erstattes ved å se på hvorvidt det er andeler eller ikke i stedet?
private fun Tidslinje<List<VilkårResultat>, Måned>.tilHarRettPåUtbetalingTidslinje(
    person: Person,
    fagsakType: FagsakType
) = when (person.type) {
    PersonType.SØKER -> map { it?.toList()?.isNotEmpty() }

    PersonType.BARN -> map {
        it != null &&
            it.alleOrdinæreVilkårErOppfylt(
                PersonType.BARN,
                fagsakType
            ) && it.alleOrdinæreVilkårErOppfylt(
            PersonType.SØKER,
            fagsakType
        )
    }

    PersonType.ANNENPART -> throw Feil("Ikke implementert for annenpart")
}

private fun List<AndelTilkjentYtelse>.tilAndelerForVedtaksPeriodeTidslinje() =
    tilTidslinjerPerBeløpOgType()
        .values
        .map { tidslinje -> tidslinje.mapIkkeNull { AndelForVedtaksperiode(it) } }
        .kombiner { it }

private fun List<InternPeriodeOvergangsstønad>.tilPeriodeOvergangsstønadForVedtaksperiodeTidslinje() = this
    .map { OvergangsstønadForVedtaksperiode(it) }
    .map { Periode(it.fom.tilMånedTidspunkt(), it.tom.tilMånedTidspunkt(), it) }
    .tilTidslinje()

private fun Tidslinje<List<VilkårResultat>, Måned>.tilVilkårResultaterForVedtaksPeriodeTidslinje() =
    this.map { vilkårResultater -> vilkårResultater?.map { VilkårResultatForVedtaksperiode(it) } }

private fun List<InternPeriodeOvergangsstønad>.filtrerPåAktør(aktør: Aktør) =
    this.filter { it.personIdent == aktør.aktivFødselsnummer() }

private fun List<AndelTilkjentYtelse>.filtrerPåAktør(
    aktør: Aktør
) = filter { andelTilkjentYtelse -> andelTilkjentYtelse.aktør == aktør }

private fun List<IUtfyltEndretUtbetalingAndel>.filtrerPåAktør(
    aktør: Aktør
) = filter { endretUtbetaling -> endretUtbetaling.person.aktør == aktør }

private fun List<UtfyltKompetanse>.filtrerPåAktør(
    aktør: Aktør
) = this.filter { it.barnAktører.contains(aktør) }