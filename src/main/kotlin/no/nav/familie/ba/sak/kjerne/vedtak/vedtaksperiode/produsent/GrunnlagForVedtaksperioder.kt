package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
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
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.månedPeriodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.mapIkkeNull
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.alleOrdinæreVilkårErOppfylt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvedeVilkårTidslinjer
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilTidslinjeForSplittForPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

typealias AktørId = String

data class GrunnlagForPersonTidslinjerSplittetPåOverlappendeGenerelleAvslag(
    val overlappendeGenerelleAvslagGrunnlagForPerson: Tidslinje<GrunnlagForPerson, Måned>,
    val grunnlagForPerson: Tidslinje<GrunnlagForPerson, Måned>,
)

data class GrunnlagForVedtaksperioder(
    val persongrunnlag: PersonopplysningGrunnlag,
    val personResultater: Set<PersonResultat>,
    val fagsakType: FagsakType,
    val kompetanser: List<Kompetanse>,
    val endredeUtbetalinger: List<EndretUtbetalingAndel>,
    val andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    val perioderOvergangsstønad: List<InternPeriodeOvergangsstønad>,
) {
    private val utfylteEndredeUtbetalinger = endredeUtbetalinger
        .map { it.tilIEndretUtbetalingAndel() }
        .filterIsInstance<IUtfyltEndretUtbetalingAndel>()

    private val utfylteKompetanser = kompetanser
        .map { it.tilIKompetanse() }
        .filterIsInstance<UtfyltKompetanse>()

    fun utledGrunnlagTidslinjePerPerson(): Map<Aktør, GrunnlagForPersonTidslinjerSplittetPåOverlappendeGenerelleAvslag> {
        val søker = persongrunnlag.søker
        val ordinæreVilkårForSøkerForskjøvetTidslinje =
            hentOrdinæreVilkårForSøkerForskjøvetTidslinje(søker, personResultater)

        val erMinstEttBarnMedUtbetalingTidslinje =
            hentErMinstEttBarnMedUtbetalingTidslinje(personResultater, fagsakType, persongrunnlag)

        val grunnlagForPersonTidslinjer = personResultater.associate { personResultat ->
            val aktør = personResultat.aktør
            val person = persongrunnlag.personer.single { person -> aktør == person.aktør }

            val (overlappendeGenerelleAvslag, vilkårResultaterUtenGenerelleAvslag) = splittOppPåErOverlappendeGenerelleAvslag(
                personResultat,
            )

            val forskjøvedeVilkårResultaterForPersonsAndeler: Tidslinje<List<VilkårResultat>, Måned> =
                vilkårResultaterUtenGenerelleAvslag.hentForskjøvedeVilkårResultaterForPersonsAndelerTidslinje(
                    person = person,
                    erMinstEttBarnMedUtbetalingTidslinje = erMinstEttBarnMedUtbetalingTidslinje,
                    ordinæreVilkårForSøkerTidslinje = ordinæreVilkårForSøkerForskjøvetTidslinje,
                )

            aktør to GrunnlagForPersonTidslinjerSplittetPåOverlappendeGenerelleAvslag(
                overlappendeGenerelleAvslagGrunnlagForPerson = overlappendeGenerelleAvslag.generelleAvslagTilGrunnlagForPersonTidslinje(
                    person,
                ),
                grunnlagForPerson = forskjøvedeVilkårResultaterForPersonsAndeler.tilGrunnlagForPersonTidslinje(
                    person = person,
                    søker = søker,
                ),
            )
        }

        return grunnlagForPersonTidslinjer
    }

    private fun List<VilkårResultat>.generelleAvslagTilGrunnlagForPersonTidslinje(
        person: Person,
    ) = this
        .map {
            listOf(månedPeriodeAv(null, null, it))
                .tilTidslinje()
        }
        .kombinerUtenNull { it.toList() }
        .map { vilkårResultater ->
            vilkårResultater?.let {
                GrunnlagForPersonIkkeInnvilget(
                    person = person,
                    vilkårResultaterForVedtaksperiode = it.map { vilkårResultat ->
                        VilkårResultatForVedtaksperiode(
                            vilkårResultat,
                        )
                    },
                ) as GrunnlagForPerson
            }
        }

    private fun Tidslinje<List<VilkårResultat>, Måned>.tilGrunnlagForPersonTidslinje(
        person: Person,
        søker: Person,
    ): Tidslinje<GrunnlagForPerson, Måned> {
        val harRettPåUtbetalingTidslinje = this.tilHarRettPåUtbetalingTidslinje(person, fagsakType, søker)

        val kompetanseTidslinje = utfylteKompetanser.filtrerPåAktør(person.aktør)
            .tilTidslinje().mapIkkeNull { KompetanseForVedtaksperiode(it) }

        val endredeUtbetalingerTidslinje = utfylteEndredeUtbetalinger.filtrerPåAktør(person.aktør)
            .tilTidslinje().mapIkkeNull { EndretUtbetalingAndelForVedtaksperiode(it) }

        val overgangsstønadTidslinje =
            perioderOvergangsstønad.filtrerPåAktør(person.aktør).tilPeriodeOvergangsstønadForVedtaksperiodeTidslinje()

        val grunnlagTidslinje = harRettPåUtbetalingTidslinje
            .kombinerMed(
                this.tilVilkårResultaterForVedtaksPeriodeTidslinje(),
                andelerTilkjentYtelse.filtrerPåAktør(person.aktør).tilAndelerForVedtaksPeriodeTidslinje(),
            ) { personHarRettPåUtbetalingIPeriode, vilkårResultater, andeler ->
                lagGrunnlagForVilkårOgAndel(
                    personHarRettPåUtbetalingIPeriode = personHarRettPåUtbetalingIPeriode,
                    vilkårResultater = vilkårResultater,
                    person = person,
                    andeler = andeler,
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
            .dropWhile { !it.erInnvilgetEllerEksplisittAvslag() }
            .tilTidslinje()
    }
}

private fun splittOppPåErOverlappendeGenerelleAvslag(personResultat: PersonResultat): Pair<List<VilkårResultat>, List<VilkårResultat>> {
    val overlappendeGenerelleAvslag =
        personResultat.vilkårResultater.groupBy { it.vilkårType }.mapNotNull { (_, resultat) ->
            if (resultat.size > 1) {
                resultat.filter { it.erGenereltAvslag() }
            } else {
                null
            }
        }.flatten()

    val vilkårResultaterUtenGenerelleAvslag =
        personResultat.vilkårResultater.filterNot { overlappendeGenerelleAvslag.contains(it) }
    return Pair(overlappendeGenerelleAvslag, vilkårResultaterUtenGenerelleAvslag)
}

private fun List<VilkårResultat>.filtrerVilkårErOrdinærtFor(
    søker: Person,
): List<VilkårResultat>? {
    val ordinæreVilkårForSøker = Vilkår.hentOrdinæreVilkårFor(søker.type)

    return this
        .filter { ordinæreVilkårForSøker.contains(it.vilkårType) }
        .takeIf { it.isNotEmpty() }
}

fun hentOrdinæreVilkårForSøkerForskjøvetTidslinje(
    søker: Person,
    personResultater: Set<PersonResultat>,
): Tidslinje<List<VilkårResultat>, Måned> {
    val søkerPersonResultater = personResultater.single { it.aktør == søker.aktør }

    val (_, vilkårResultaterUtenOverlappendeGenerelleAvslag) = splittOppPåErOverlappendeGenerelleAvslag(
        søkerPersonResultater,
    )

    return vilkårResultaterUtenOverlappendeGenerelleAvslag
        .tilForskjøvedeVilkårTidslinjer(søker.fødselsdato)
        .kombiner { vilkårResultater -> vilkårResultater.toList().takeIf { it.isNotEmpty() } }
        .map { it?.toList()?.filtrerVilkårErOrdinærtFor(søker) }
}

fun VilkårResultat.erGenereltAvslag() =
    periodeFom == null && periodeTom == null && erEksplisittAvslagPåSøknad == true

private fun hentErMinstEttBarnMedUtbetalingTidslinje(
    personResultater: Set<PersonResultat>,
    fagsakType: FagsakType,
    persongrunnlag: PersonopplysningGrunnlag,
): Tidslinje<Boolean, Måned> {
    val søker = persongrunnlag.søker
    val søkerSinerOrdinæreVilkårErOppfyltTidslinje =
        personResultater.single { it.erSøkersResultater() }.tilTidslinjeForSplittForPerson(
            person = søker,
            fagsakType = fagsakType,
        ).map { it != null }

    val barnSineVilkårErOppfyltTidslinjer = personResultater
        .filter { it.aktør != søker.aktør || søker.type == PersonType.BARN }
        .map { personResultat ->
            personResultat.tilTidslinjeForSplittForPerson(
                person = persongrunnlag.barna.single { it.aktør == personResultat.aktør },
                fagsakType = fagsakType,
            ).map { it != null }
        }

    return barnSineVilkårErOppfyltTidslinjer
        .map {
            it.kombinerMed(søkerSinerOrdinæreVilkårErOppfyltTidslinje) { barnetHarAlleOrdinæreVilkårOppfylt, søkerHarAlleOrdinæreVilkårOppfylt ->
                barnetHarAlleOrdinæreVilkårOppfylt == true && søkerHarAlleOrdinæreVilkårOppfylt == true
            }
        }
        .kombiner { erOrdinæreVilkårOppfyltForSøkerOgBarn ->
            erOrdinæreVilkårOppfyltForSøkerOgBarn.any { it }
        }
}

private fun List<VilkårResultat>.hentForskjøvedeVilkårResultaterForPersonsAndelerTidslinje(
    person: Person,
    erMinstEttBarnMedUtbetalingTidslinje: Tidslinje<Boolean, Måned>,
    ordinæreVilkårForSøkerTidslinje: Tidslinje<List<VilkårResultat>, Måned>,
): Tidslinje<List<VilkårResultat>, Måned> {
    val forskjøvedeVilkårResultaterForPerson = this.tilForskjøvedeVilkårTidslinjer(person.fødselsdato).kombiner { it }

    return when (person.type) {
        PersonType.SØKER -> forskjøvedeVilkårResultaterForPerson.map { vilkårResultater ->
            vilkårResultater?.filtrerErIkkeOrdinærtFor(person)
        }.kombinerMed(erMinstEttBarnMedUtbetalingTidslinje) { vilkårResultaterForSøker, erMinstEttBarnMedUtbetaling ->
            vilkårResultaterForSøker?.takeIf { erMinstEttBarnMedUtbetaling == true || vilkårResultaterForSøker.any { it.erEksplisittAvslagPåSøknad == true } }
        }

        PersonType.BARN ->
            forskjøvedeVilkårResultaterForPerson
                .kombinerMed(ordinæreVilkårForSøkerTidslinje) { vilkårResultaterBarn, vilkårResultaterSøker ->
                    slåSammenHvisMulig(vilkårResultaterBarn, vilkårResultaterSøker)?.toList()
                }

        PersonType.ANNENPART -> throw Feil("Ikke implementert for annenpart")
    }
}

private fun slåSammenHvisMulig(
    venstre: Iterable<VilkårResultat>?,
    høyre: Iterable<VilkårResultat>?,
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
    personHarRettPåUtbetalingIPeriode: Boolean?,
    vilkårResultater: List<VilkårResultatForVedtaksperiode>?,
    person: Person,
    andeler: Iterable<AndelForVedtaksperiode>?,
) = if (personHarRettPåUtbetalingIPeriode == true) {
    if (andeler == null) {
        secureLogger.info(
            "Vi har fått en innvilget vedtaksperiode, men det finnes ingen andeler.\n" +
                "$person\n" +
                "$vilkårResultater\n" +
                "$andeler",
        )
    }

    GrunnlagForPersonInnvilget(
        vilkårResultaterForVedtaksperiode = vilkårResultater
            ?: error("vilkårResultatene burde alltid finnes om vi har innvilget vedtaksperiode."),
        person = person,
        andeler = andeler
            ?: error(
                "andeler må finnes for innvilgede vedtaksperioder. Vedtaksperioden er innenfor " +
                    "${vilkårResultater.firstOrNull()?.fom} -> ${vilkårResultater.firstOrNull()?.tom}",
            ),
    )
} else {
    GrunnlagForPersonIkkeInnvilget(
        vilkårResultaterForVedtaksperiode = vilkårResultater ?: emptyList(),
        person = person,
    )
}

private fun lagGrunnlagMedKompetanse(
    grunnlagForPerson: GrunnlagForPerson?,
    kompetanse: KompetanseForVedtaksperiode?,
) = when (grunnlagForPerson) {
    is GrunnlagForPersonInnvilget -> grunnlagForPerson.copy(kompetanse = kompetanse)
    is GrunnlagForPersonIkkeInnvilget -> grunnlagForPerson
    null -> null
}

private fun lagGrunnlagMedEndretUtbetalingAndel(
    grunnlagForPerson: GrunnlagForPerson?,
    endretUtbetalingAndel: EndretUtbetalingAndelForVedtaksperiode?,
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
    overgangsstønad: OvergangsstønadForVedtaksperiode?,
) = when (grunnlagForPerson) {
    is GrunnlagForPersonInnvilget -> grunnlagForPerson.copy(overgangsstønad = overgangsstønad)
    is GrunnlagForPersonIkkeInnvilget -> grunnlagForPerson
    null -> null
}

// TODO: Kan dette erstattes ved å se på hvorvidt det er andeler eller ikke i stedet?
private fun Tidslinje<List<VilkårResultat>, Måned>.tilHarRettPåUtbetalingTidslinje(
    person: Person,
    fagsakType: FagsakType,
    søker: Person,
): Tidslinje<Boolean, Måned> = this.map { vilkårResultater ->
    if (vilkårResultater.isNullOrEmpty()) {
        null
    } else {
        when (person.type) {
            PersonType.SØKER -> vilkårResultater.filtrerPåAktør(søker.aktør).all { it.erOppfylt() }

            PersonType.BARN -> {
                val barnSineVilkårErOppfylt = vilkårResultater.filtrerPåAktør(person.aktør)
                    .alleOrdinæreVilkårErOppfylt(
                        PersonType.BARN,
                        fagsakType,
                    )
                val søkerSineVilkårErOppfylt = vilkårResultater.filtrerPåAktør(søker.aktør)
                    .alleOrdinæreVilkårErOppfylt(
                        PersonType.SØKER,
                        fagsakType,
                    )

                barnSineVilkårErOppfylt && søkerSineVilkårErOppfylt
            }

            PersonType.ANNENPART -> throw Feil("Ikke implementert for annenpart")
        }
    }
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

@JvmName("internPeriodeOvergangsstønaderFiltrerPåAktør")
private fun List<InternPeriodeOvergangsstønad>.filtrerPåAktør(aktør: Aktør) =
    this.filter { it.personIdent == aktør.aktivFødselsnummer() }

@JvmName("andelerTilkjentYtelserFiltrerPåAktør")
private fun List<AndelTilkjentYtelse>.filtrerPåAktør(aktør: Aktør) =
    this.filter { andelTilkjentYtelse -> andelTilkjentYtelse.aktør == aktør }

@JvmName("endredeUtbetalingerFiltrerPåAktør")
private fun List<IUtfyltEndretUtbetalingAndel>.filtrerPåAktør(aktør: Aktør) =
    this.filter { endretUtbetaling -> endretUtbetaling.person.aktør == aktør }

@JvmName("utfyltKompetanseFiltrerPåAktør")
private fun List<UtfyltKompetanse>.filtrerPåAktør(aktør: Aktør) =
    this.filter { it.barnAktører.contains(aktør) }

@JvmName("vilkårResultatFiltrerPåAktør")
private fun List<VilkårResultat>.filtrerPåAktør(aktør: Aktør) =
    filter { it.personResultat?.aktør == aktør }

private fun Periode<GrunnlagForPerson, Måned>.erInnvilgetEllerEksplisittAvslag(): Boolean {
    val grunnlagForPerson = innhold ?: return false

    val erInnvilget = grunnlagForPerson is GrunnlagForPersonInnvilget
    val erEksplisittAvslag =
        grunnlagForPerson.vilkårResultaterForVedtaksperiode.any { it.erEksplisittAvslagPåSøknad == true }

    return erInnvilget || erEksplisittAvslag
}
