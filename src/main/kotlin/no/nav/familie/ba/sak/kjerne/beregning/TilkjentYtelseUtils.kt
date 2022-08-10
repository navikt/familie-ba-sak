package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.Utils.avrundetHeltallAvProsent
import no.nav.familie.ba.sak.common.erBack2BackIMånedsskifte
import no.nav.familie.ba.sak.common.erDagenFør
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inkluderer
import no.nav.familie.ba.sak.common.maksimum
import no.nav.familie.ba.sak.common.minimum
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.SatsPeriode
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.splittPeriodePå6Årsdag
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.PeriodeResultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

object TilkjentYtelseUtils {

    fun beregnTilkjentYtelse(
        vilkårsvurdering: Vilkårsvurdering,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        behandling: Behandling,
        hentPerioderMedFullOvergangsstønad: (aktør: Aktør) -> List<InternPeriodeOvergangsstønad> = { _ -> emptyList() },
    ): TilkjentYtelse {
        val identBarnMap = personopplysningGrunnlag.barna.associateBy { it.aktør.aktørId }

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
            .flatMap { periodeResultatBarn: PeriodeResultat ->
                relevanteSøkerPerioder
                    .flatMap { overlappendePerioderesultatSøker ->
                        val person = identBarnMap[periodeResultatBarn.aktør.aktørId]
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
                            val prosent =
                                if (periodeResultatBarn.erDeltBostedSomSkalDeles()) BigDecimal(50) else BigDecimal(100)
                            val nasjonaltPeriodebeløp = beløpsperiode.sats.avrundetHeltallAvProsent(prosent)
                            AndelTilkjentYtelse(
                                behandlingId = vilkårsvurdering.behandling.id,
                                tilkjentYtelse = tilkjentYtelse,
                                aktør = person.aktør,
                                stønadFom = beløpsperiode.fraOgMed,
                                stønadTom = beløpsperiode.tilOgMed,
                                kalkulertUtbetalingsbeløp = nasjonaltPeriodebeløp,
                                nasjonaltPeriodebeløp = nasjonaltPeriodebeløp,
                                type = finnYtelseType(behandling.underkategori, person.type),
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
                hentPerioderMedFullOvergangsstønad(
                    personopplysningGrunnlag.søker.aktør
                )

            SmåbarnstilleggBarnetrygdGenerator(
                behandlingId = vilkårsvurdering.behandling.id,
                tilkjentYtelse = tilkjentYtelse
            )
                .lagSmåbarnstilleggAndeler(
                    perioderMedFullOvergangsstønad = perioderMedFullOvergangsstønad,
                    andelerTilkjentYtelse = andelerTilkjentYtelseSøker + andelerTilkjentYtelseBarna,
                    barnasAktørerOgFødselsdatoer = personopplysningGrunnlag.barna.map {
                        Pair(
                            it.aktør,
                            it.fødselsdato
                        )
                    },
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

        val (andelerUtenSmåbarnstillegg, andelerMedSmåbarnstillegg) = andelTilkjentYtelser.partition { !it.erSmåbarnstillegg() }

        val nyeAndelTilkjentYtelse = mutableListOf<AndelTilkjentYtelse>()

        andelerUtenSmåbarnstillegg.groupBy { it.aktør }.forEach { andelerForPerson ->
            val aktør = andelerForPerson.key
            val endringerForPerson =
                endretUtbetalingAndeler.filter { it.person?.aktør == aktør }

            val nyeAndelerForPerson = mutableListOf<AndelTilkjentYtelse>()

            andelerForPerson.value.forEach { andelForPerson ->
                // Deler opp hver enkelt andel i perioder som hhv blir berørt av endringene og de som ikke berøres av de.
                val (perioderMedEndring, perioderUtenEndring) = andelForPerson.stønadsPeriode()
                    .perioderMedOgUtenOverlapp(
                        endringerForPerson.map { endringerForPerson -> endringerForPerson.periode }
                    )
                // Legger til nye AndelTilkjentYtelse for perioder som er berørt av endringer.
                nyeAndelerForPerson.addAll(
                    perioderMedEndring.map { månedPeriodeEndret ->
                        val endretUtbetalingAndel = endringerForPerson.single { it.overlapperMed(månedPeriodeEndret) }
                        val nyttNasjonaltPeriodebeløp = andelForPerson.sats
                            .avrundetHeltallAvProsent(endretUtbetalingAndel.prosent!!)
                        andelForPerson.copy(
                            prosent = endretUtbetalingAndel.prosent!!,
                            stønadFom = månedPeriodeEndret.fom,
                            stønadTom = månedPeriodeEndret.tom,
                            kalkulertUtbetalingsbeløp = nyttNasjonaltPeriodebeløp,
                            nasjonaltPeriodebeløp = nyttNasjonaltPeriodebeløp,
                            endretUtbetalingAndeler = (andelForPerson.endretUtbetalingAndeler + endretUtbetalingAndel).toMutableList(),
                        )
                    }
                )
                // Legger til nye AndelTilkjentYtelse for perioder som ikke berøres av endringer.
                nyeAndelerForPerson.addAll(
                    perioderUtenEndring.map { månedPeriodeUendret ->
                        andelForPerson.copy(stønadFom = månedPeriodeUendret.fom, stønadTom = månedPeriodeUendret.tom)
                    }
                )
            }

            val nyeAndelerForPersonEtterSammenslåing =
                slåSammenPerioderSomIkkeSkulleHaVærtSplittet(
                    andelerTilkjentYtelse = nyeAndelerForPerson,
                    skalAndelerSlåsSammen = ::skalAndelerSlåsSammen
                )

            nyeAndelTilkjentYtelse.addAll(nyeAndelerForPersonEtterSammenslåing)
        }

        // Ettersom vi aldri ønsker å overstyre småbarnstillegg perioder fjerner vi dem og legger dem til igjen her
        nyeAndelTilkjentYtelse.addAll(andelerMedSmåbarnstillegg)

        // Sorterer primært av hensyn til måten testene er implementert og kan muligens fjernes dersom dette skrives om.
        nyeAndelTilkjentYtelse.sortWith(
            compareBy(
                { it.aktør.aktivFødselsnummer() },
                { it.stønadFom }
            )
        )
        return nyeAndelTilkjentYtelse.toMutableSet()
    }

    fun slåSammenPerioderSomIkkeSkulleHaVærtSplittet(
        andelerTilkjentYtelse: MutableList<AndelTilkjentYtelse>,
        skalAndelerSlåsSammen: (førsteAndel: AndelTilkjentYtelse, nesteAndel: AndelTilkjentYtelse) -> Boolean
    ): MutableList<AndelTilkjentYtelse> {
        val sorterteAndeler = andelerTilkjentYtelse.sortedBy { it.stønadFom }.toMutableList()
        var periodenViSerPå: AndelTilkjentYtelse = sorterteAndeler.first()
        val oppdatertListeMedAndeler = mutableListOf<AndelTilkjentYtelse>()

        for (index in 0 until sorterteAndeler.size) {
            val andel = sorterteAndeler[index]
            val nesteAndel = if (index == sorterteAndeler.size - 1) null else sorterteAndeler[index + 1]

            periodenViSerPå = if (nesteAndel != null) {
                val andelerSkalSlåsSammen =
                    skalAndelerSlåsSammen(andel, nesteAndel)

                if (andelerSkalSlåsSammen) {
                    val nyAndel = periodenViSerPå.copy(stønadTom = nesteAndel.stønadTom)
                    nyAndel
                } else {
                    oppdatertListeMedAndeler.add(periodenViSerPå)
                    sorterteAndeler[index + 1]
                }
            } else {
                oppdatertListeMedAndeler.add(periodenViSerPå)
                break
            }
        }
        return oppdatertListeMedAndeler
    }

    /**
     * Slår sammen andeler for barn når beløpet er nedjuster til 0kr som er blitt splittet av
     * for eksempel satsendring.
     */
    private fun skalAndelerSlåsSammen(
        førsteAndel: AndelTilkjentYtelse,
        nesteAndel: AndelTilkjentYtelse
    ): Boolean =
        førsteAndel.stønadTom.sisteDagIInneværendeMåned()
            .erDagenFør(nesteAndel.stønadFom.førsteDagIInneværendeMåned()) && førsteAndel.prosent == BigDecimal(0) && nesteAndel.prosent == BigDecimal(
            0
        ) && førsteAndel.endretUtbetalingAndeler.isNotEmpty() && førsteAndel.endretUtbetalingAndeler.singleOrNull() == nesteAndel.endretUtbetalingAndeler.singleOrNull()

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
            periodeResultatBarn.periodeTom == null || periodeResultatBarn.periodeTom.toYearMonth() > minsteTom.toYearMonth()

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
                    periodeResultatBarn.aktør == periodeResultat.aktør
            }

        val oppfyltTom = if (skalVidereføresEnMånedEkstra) minsteTom.plusMonths(1) else minsteTom

        val oppfyltTomKommerFra18ÅrsVilkår =
            oppfyltTom == person.fødselsdato.til18ÅrsVilkårsdato()

        val skalAvsluttesMånedenFør =
            if (person.erDød()) {
                person.dødsfall!!.dødsfallDato.førsteDagIInneværendeMåned() >= person.fødselsdato.til18ÅrsVilkårsdato()
                    .førsteDagIInneværendeMåned()
            } else {
                oppfyltTomKommerFra18ÅrsVilkår
            }

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
            maxSatsGyldigFraOgMed = SatsService.tilleggEndringJanuar2022,
        ) else emptyList()

        val satsperioderEtterFylte6År = if (periodeOver6år != null) SatsService.hentGyldigSatsFor(
            satstype = SatsType.ORBA,
            stønadFraOgMed = settRiktigStønadFom(
                skalStarteSammeMåned =
                periodeUnder6År != null,
                fraOgMed = periodeOver6år.fom
            ),
            stønadTilOgMed = settRiktigStønadTom(
                skalAvsluttesMånedenFør = skalAvsluttesMånedenFør,
                tilOgMed = periodeOver6år.tom
            ),
            maxSatsGyldigFraOgMed = SatsService.tilleggEndringJanuar2022,
        ) else emptyList()

        return listOf(satsperioderFørFylte6År, satsperioderEtterFylte6År).flatten()
            .sortedBy { it.fraOgMed }
            .fold(mutableListOf(), ::slåSammenEtterfølgendePerioderMedSammeBeløp)
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
        underkategori: BehandlingUnderkategori,
        personType: PersonType
    ): YtelseType {
        return if (personType == PersonType.SØKER && underkategori == BehandlingUnderkategori.UTVIDET) {
            YtelseType.UTVIDET_BARNETRYGD
        } else if (personType == PersonType.BARN) {
            YtelseType.ORDINÆR_BARNETRYGD
        } else {
            throw Feil("Ikke støttet. Klarte ikke utlede YtelseType for underkategori $underkategori og persontype $personType.")
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
        alleMånederMedOverlappstatus[nesteMåned] = perioder.any { månedPeriode -> månedPeriode.inkluderer(nesteMåned) }
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
