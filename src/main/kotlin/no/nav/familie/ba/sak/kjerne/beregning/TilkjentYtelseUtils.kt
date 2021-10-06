package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.Utils.avrundetHeltallAvProsent
import no.nav.familie.ba.sak.common.erDagenFør
import no.nav.familie.ba.sak.common.inkluderer
import no.nav.familie.ba.sak.common.maksimum
import no.nav.familie.ba.sak.common.minimum
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.SatsPeriode
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.splittPeriodePå6Årsdag
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.PeriodeResultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

object TilkjentYtelseUtils {

    fun beregnTilkjentYtelse(
        vilkårsvurdering: Vilkårsvurdering,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        behandling: Behandling,
        hentPerioderMedFullOvergangsstønad: (personIdent: String) -> List<PeriodeOvergangsstønad> = { emptyList() }
    ): TilkjentYtelse {
        val identBarnMap = personopplysningGrunnlag.barna
            .associateBy { it.personIdent.ident }

        val (innvilgetPeriodeResultatSøker, innvilgedePeriodeResultatBarna) = vilkårsvurdering.hentInnvilgedePerioder(
            personopplysningGrunnlag
        )

        val relevanteSøkerPerioder = innvilgetPeriodeResultatSøker
            .filter { søkerPeriode -> innvilgedePeriodeResultatBarna.any { søkerPeriode.overlapper(it) } }

        val tilkjentYtelse = TilkjentYtelse(
            behandling = vilkårsvurdering.behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now()
        )

        val andelerTilkjentYtelseBarna = innvilgedePeriodeResultatBarna
            .flatMap { periodeResultatBarn ->
                relevanteSøkerPerioder
                    .flatMap { overlappendePerioderesultatSøker ->
                        val person = identBarnMap[periodeResultatBarn.personIdent]
                            ?: error("Finner ikke barn på map over barna i behandlingen")
                        val beløpsperioder =
                            beregnBeløpsperioder(
                                overlappendePerioderesultatSøker,
                                periodeResultatBarn,
                                innvilgedePeriodeResultatBarna,
                                innvilgetPeriodeResultatSøker,
                                person
                            )

                        beløpsperioder.map { beløpsperiode ->
                            val prosent = if (periodeResultatBarn.erDeltBosted()) BigDecimal(50) else BigDecimal(100)
                            AndelTilkjentYtelse(
                                behandlingId = vilkårsvurdering.behandling.id,
                                tilkjentYtelse = tilkjentYtelse,
                                personIdent = person.personIdent.ident,
                                stønadFom = beløpsperiode.fraOgMed,
                                stønadTom = beløpsperiode.tilOgMed,
                                kalkulertUtbetalingsbeløp = beløpsperiode.sats.avrundetHeltallAvProsent(prosent),
                                type = finnYtelseType(behandling.kategori, behandling.underkategori, person.type),
                                sats = beløpsperiode.sats,
                                prosent = prosent
                            )
                        }
                    }
            }

        val andelerTilkjentYtelseSøker = UtvidetBarnetrygdGenerator(
            behandlingId = vilkårsvurdering.behandling.id,
            tilkjentYtelse = tilkjentYtelse
        )
            .lagUtvidetBarnetrygdAndeler(
                utvidetVilkår = vilkårsvurdering.personResultater
                    .flatMap { it.vilkårResultater }
                    .filter { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD && it.resultat == Resultat.OPPFYLT },
                andelerBarna = andelerTilkjentYtelseBarna
            )

        val andelerTilkjentYtelseSmåbarnstillegg = if (andelerTilkjentYtelseSøker.isNotEmpty()) {
            val perioderMedFullOvergangsstønad =
                hentPerioderMedFullOvergangsstønad(personopplysningGrunnlag.søker.personIdent.ident)

            SmåbarnstilleggBarnetrygdGenerator(
                behandlingId = vilkårsvurdering.behandling.id,
                tilkjentYtelse = tilkjentYtelse
            )
                .lagSmåbarnstilleggAndeler(
                    perioderMedFullOvergangsstønad = perioderMedFullOvergangsstønad,
                    andelerSøker = andelerTilkjentYtelseSøker,
                    barnasFødselsdatoer = personopplysningGrunnlag.barna.map { it.fødselsdato }
                )
        } else emptyList()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelseBarna + andelerTilkjentYtelseSøker + andelerTilkjentYtelseSmåbarnstillegg)

        return tilkjentYtelse
    }

    fun oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
        andelTilkjentYtelser: MutableSet<AndelTilkjentYtelse>,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>
    ): MutableSet<AndelTilkjentYtelse> {

        if (endretUtbetalingAndeler.isEmpty()) return andelTilkjentYtelser.map { it.copy() }.toMutableSet()

        val nyeAndelTilkjentYtelse = mutableListOf<AndelTilkjentYtelse>()

        andelTilkjentYtelser.distinctBy { it.personIdent }.forEach { barnMedAndeler ->
            val andelerForPerson = andelTilkjentYtelser.filter { it.personIdent == barnMedAndeler.personIdent }
            val endringerForPerson =
                endretUtbetalingAndeler.filter { it.person?.personIdent?.ident == barnMedAndeler.personIdent }

            andelerForPerson.forEach { andelForPerson ->
                // Deler opp hver enkelt andel i perioder som hhv blir berørt av endringene og de som ikke berøres av de.
                val (perioderMedEndring, perioderUtenEndring) = andelForPerson.stønadsPeriode()
                    .perioderMedOgUtenOverlapp(
                        endringerForPerson.map { endringerForPerson -> endringerForPerson.periode() }
                    )
                // Legger til nye AndelTilkjentYtelse for perioder som er berørt av endringer.
                nyeAndelTilkjentYtelse.addAll(
                    perioderMedEndring.map { månedPeriodeEndret ->
                        val endretUtbetalingAndel = endringerForPerson.single { it.overlapperMed(månedPeriodeEndret) }
                        andelForPerson.copy(
                            stønadFom = månedPeriodeEndret.fom,
                            stønadTom = månedPeriodeEndret.tom,
                            kalkulertUtbetalingsbeløp = andelForPerson.kalkulertUtbetalingsbeløp
                                .avrundetHeltallAvProsent(endretUtbetalingAndel.prosent!!)
                        )
                    }
                )
                // Legger til nye AndelTilkjentYtelse for perioder som ikke berøres av endringer.
                nyeAndelTilkjentYtelse.addAll(
                    perioderUtenEndring.map { månedPeriodeUendret ->
                        andelForPerson.copy(stønadFom = månedPeriodeUendret.fom, stønadTom = månedPeriodeUendret.tom)
                    }
                )
            }
        }
        // Sorterer primært av hensyn til måten testene er implementert og kan muligens fjernes dersom dette skrives om.
        nyeAndelTilkjentYtelse.sortWith(compareBy({ it.personIdent }, { it.stønadFom }))
        return nyeAndelTilkjentYtelse.toMutableSet()
    }

    private fun beregnBeløpsperioder(
        overlappendePerioderesultatSøker: PeriodeResultat,
        periodeResultatBarn: PeriodeResultat,
        innvilgedePeriodeResultatBarna: List<PeriodeResultat>,
        innvilgetPeriodeResultatSøker: List<PeriodeResultat>,
        person: Person
    ): MutableList<SatsPeriode> {
        val oppfyltFom =
            maksimum(overlappendePerioderesultatSøker.periodeFom, periodeResultatBarn.periodeFom)

        val minsteTom =
            minimum(overlappendePerioderesultatSøker.periodeTom, periodeResultatBarn.periodeTom)

        val barnetsPeriodeLøperVidere =
            if (periodeResultatBarn.periodeTom == null) true else periodeResultatBarn.periodeTom.toYearMonth() > minsteTom.toYearMonth()

        val skalVidereføresEnMånedEkstra =
            innvilgedePeriodeResultatBarna.any { periodeResultat ->
                innvilgetPeriodeResultatSøker.any { periodeResultatSøker ->
                    periodeResultatSøker.overlapper(periodeResultat)
                } &&
                    (
                        erBack2BackIMånedsskifte(periodeResultatBarn.periodeTom, periodeResultat.periodeFom) ||
                            søkerHarInnvilgetPeriodeEtterBarnsPeriode(
                                innvilgetPeriodeResultatSøker = innvilgetPeriodeResultatSøker,
                                tilOgMed = minsteTom,
                                barnetsPeriodeLøperVidere = barnetsPeriodeLøperVidere
                            )
                        ) &&
                    periodeResultatBarn.personIdent == periodeResultat.personIdent
            }

        val oppfyltTom = if (skalVidereføresEnMånedEkstra) minsteTom.plusMonths(1) else minsteTom

        val oppfyltTomKommerFra18ÅrsVilkår =
            oppfyltTom == periodeResultatBarn.vilkårResultater.find {
                it.vilkårType == Vilkår.UNDER_18_ÅR
            }?.periodeTom

        val (periodeUnder6År, periodeOver6år) = splittPeriodePå6Årsdag(
            person.hentSeksårsdag(),
            oppfyltFom,
            oppfyltTom
        )
        val satsperioderFørFylte6År = if (periodeUnder6År != null) SatsService.hentGyldigSatsFor(
            satstype = SatsType.TILLEGG_ORBA,
            stønadFraOgMed = settRiktigStønadFom(
                fraOgMed = periodeUnder6År.fom
            ),
            stønadTilOgMed = settRiktigStønadTom(tilOgMed = periodeUnder6År.tom),
            maxSatsGyldigFraOgMed = SatsService.tilleggEndringSeptember2021,
        ) else emptyList()

        val satsperioderEtterFylte6År = if (periodeOver6år != null) SatsService.hentGyldigSatsFor(
            satstype = SatsType.ORBA,
            stønadFraOgMed = settRiktigStønadFom(
                skalStarteSammeMåned =
                periodeUnder6År != null,
                fraOgMed = periodeOver6år.fom
            ),
            stønadTilOgMed = settRiktigStønadTom(
                skalAvsluttesMånedenFør = oppfyltTomKommerFra18ÅrsVilkår,
                tilOgMed = periodeOver6år.tom
            ),
            maxSatsGyldigFraOgMed = SatsService.tilleggEndringSeptember2021,
        ) else emptyList()

        return listOf(satsperioderFørFylte6År, satsperioderEtterFylte6År).flatten()
            .sortedBy { it.fraOgMed }
            .fold(mutableListOf(), ::slåSammenEtterfølgendePerioderMedSammeBeløp)
    }

    fun erBack2BackIMånedsskifte(tilOgMed: LocalDate?, fraOgMed: LocalDate?): Boolean {
        return tilOgMed?.erDagenFør(fraOgMed) == true &&
            tilOgMed.toYearMonth() != fraOgMed?.toYearMonth()
    }

    private fun søkerHarInnvilgetPeriodeEtterBarnsPeriode(
        innvilgetPeriodeResultatSøker: List<PeriodeResultat>,
        tilOgMed: LocalDate?,
        barnetsPeriodeLøperVidere: Boolean
    ): Boolean {
        return innvilgetPeriodeResultatSøker.any {
            erBack2BackIMånedsskifte(tilOgMed, it.periodeFom)
        } && barnetsPeriodeLøperVidere
    }

    private fun finnYtelseType(
        kategori: BehandlingKategori,
        underkategori: BehandlingUnderkategori,
        personType: PersonType
    ): YtelseType {
        return if (personType == PersonType.SØKER && underkategori == BehandlingUnderkategori.UTVIDET) {
            YtelseType.UTVIDET_BARNETRYGD
        } else if (personType == PersonType.BARN && kategori == BehandlingKategori.NASJONAL) {
            YtelseType.ORDINÆR_BARNETRYGD
        } else {
            throw Feil("Ikke støttet. Klarte ikke utlede YtelseType for kategori $kategori, underkategori $underkategori og persontype $personType.")
        }
    }

    private fun settRiktigStønadFom(skalStarteSammeMåned: Boolean = false, fraOgMed: LocalDate): YearMonth =
        if (skalStarteSammeMåned)
            YearMonth.from(fraOgMed.withDayOfMonth(1))
        else
            YearMonth.from(fraOgMed.plusMonths(1).withDayOfMonth(1))

    private fun settRiktigStønadTom(skalAvsluttesMånedenFør: Boolean = false, tilOgMed: LocalDate): YearMonth =
        if (skalAvsluttesMånedenFør)
            YearMonth.from(tilOgMed.plusDays(1).minusMonths(1).sisteDagIMåned())
        else
            YearMonth.from(tilOgMed.sisteDagIMåned())
}

fun MånedPeriode.perioderMedOgUtenOverlapp(perioder: List<MånedPeriode>): Pair<List<MånedPeriode>, List<MånedPeriode>> {
    if (perioder.isEmpty()) return Pair(emptyList(), listOf(this))

    val alleMånederMedOverlappstatus = mutableMapOf<YearMonth, Boolean>()
    var nesteMåned = this.fom
    while (nesteMåned <= this.tom) {
        alleMånederMedOverlappstatus.put(
            nesteMåned,
            perioder.any { månedPeriode -> månedPeriode.inkluderer(nesteMåned) }
        )
        nesteMåned = nesteMåned.plusMonths(1)
    }

    var periodeStart: YearMonth? = this.fom

    val perioderMedOverlapp = mutableListOf<MånedPeriode>()
    val perioderUtenOverlapp = mutableListOf<MånedPeriode>()
    while (periodeStart != null) {
        val periodeMedOverlapp = alleMånederMedOverlappstatus[periodeStart]!!

        val nesteMånedMedNyOverlappstatus = alleMånederMedOverlappstatus
            .filter { it.key > periodeStart && it.value != periodeMedOverlapp }
            .minByOrNull { it.key }
            ?.key?.minusMonths(1) ?: this.tom

        // Når tom skal utledes for en periode det eksisterer en endret periode for må den minste av følgende to datoer velges:
        // 1. tom for den aktuelle endrete perioden
        // 2. neste måned uten overlappende endret periode, eller hvis null, tom for this (som representerer en AndelTilkjentYtelse).
        // Dersom tom gjelder periode uberørt av endringer så vil alltid alt.2 være korrekt.
        val periodeSlutt = if (periodeMedOverlapp) {
            val nesteMånedUtenOverlapp = perioder.single { it.inkluderer(periodeStart!!) }.tom
            minOf(nesteMånedUtenOverlapp, nesteMånedMedNyOverlappstatus)
        } else {
            nesteMånedMedNyOverlappstatus
        }

        if (periodeMedOverlapp)
            perioderMedOverlapp.add(MånedPeriode(periodeStart, periodeSlutt))
        else perioderUtenOverlapp.add(MånedPeriode(periodeStart, periodeSlutt))

        periodeStart = alleMånederMedOverlappstatus
            .filter { it.key > periodeSlutt }
            .minByOrNull { it.key }?.key
    }
    return Pair(perioderMedOverlapp, perioderUtenOverlapp)
}

private fun slåSammenEtterfølgendePerioderMedSammeBeløp(
    sammenlagt: MutableList<SatsPeriode>,
    neste: SatsPeriode
): MutableList<SatsPeriode> {
    if (sammenlagt.isNotEmpty() && sammenlagt.last().sats == neste.sats) {
        val forrigeOgNeste = SatsPeriode(neste.sats, sammenlagt.last().fraOgMed, neste.tilOgMed)
        sammenlagt.removeLast()
        sammenlagt.add(forrigeOgNeste)
    } else {
        sammenlagt.add(neste)
    }
    return sammenlagt
}
