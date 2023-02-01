package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.Utils.avrundetHeltallAvProsent
import no.nav.familie.ba.sak.common.erDagenFør
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inkluderer
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.medEndring
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.alleVilkårErOppfylt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilForskjøvetTidslinjerForHvertOppfylteVilkår
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

object TilkjentYtelseUtils {

    fun beregnTilkjentYtelse(
        vilkårsvurdering: Vilkårsvurdering,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        behandling: Behandling,
        endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse> = emptyList(),
        hentPerioderMedFullOvergangsstønad: (aktør: Aktør) -> List<InternPeriodeOvergangsstønad> = { _ -> emptyList() }
    ): TilkjentYtelse {
        val tilkjentYtelse = TilkjentYtelse(
            behandling = vilkårsvurdering.behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now()
        )

        val (endretUtbetalingAndelerSøker, endretUtbetalingAndelerBarna) = endretUtbetalingAndeler.partition { it.person?.type == PersonType.SØKER }

        val andelerTilkjentYtelseBarnaUtenEndringer = beregnAndelerTilkjentYtelseForBarna(
            personopplysningGrunnlag = personopplysningGrunnlag,
            personResultater = vilkårsvurdering.personResultater
        )
            .map {
                AndelTilkjentYtelse(
                    behandlingId = vilkårsvurdering.behandling.id,
                    tilkjentYtelse = tilkjentYtelse,
                    aktør = it.person.aktør,
                    stønadFom = it.stønadFom,
                    stønadTom = it.stønadTom,
                    kalkulertUtbetalingsbeløp = it.beløp,
                    nasjonaltPeriodebeløp = it.beløp,
                    type = finnYtelseType(behandling.underkategori, it.person.type),
                    sats = it.sats,
                    prosent = it.prosent
                )
            }

        val barnasAndelerInkludertEtterbetaling3ÅrEndringer = oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
            andelTilkjentYtelserUtenEndringer = andelerTilkjentYtelseBarnaUtenEndringer,
            endretUtbetalingAndeler = endretUtbetalingAndelerBarna.filter { it.årsak == Årsak.ETTERBETALING_3ÅR }
        )

        val andelerTilkjentYtelseUtvidetMedAlleEndringer = beregnTilkjentYtelseUtvidet(
            utvidetVilkår = finnUtvidetVilkår(vilkårsvurdering),
            tilkjentYtelse = tilkjentYtelse,
            andelerTilkjentYtelseBarnaMedEtterbetaling3ÅrEndringer = barnasAndelerInkludertEtterbetaling3ÅrEndringer,
            endretUtbetalingAndelerSøker = endretUtbetalingAndelerSøker
        )

        val småbarnstilleggErMulig = erSmåbarnstilleggMulig(
            utvidetAndeler = andelerTilkjentYtelseUtvidetMedAlleEndringer,
            barnasAndeler = barnasAndelerInkludertEtterbetaling3ÅrEndringer
        )

        val andelerTilkjentYtelseSmåbarnstillegg = if (småbarnstilleggErMulig) {
            SmåbarnstilleggBarnetrygdGenerator(
                behandlingId = vilkårsvurdering.behandling.id,
                tilkjentYtelse = tilkjentYtelse
            )
                .lagSmåbarnstilleggAndeler(
                    perioderMedFullOvergangsstønad = hentPerioderMedFullOvergangsstønad(
                        personopplysningGrunnlag.søker.aktør
                    ),
                    utvidetAndeler = andelerTilkjentYtelseUtvidetMedAlleEndringer,
                    barnasAndeler = barnasAndelerInkludertEtterbetaling3ÅrEndringer,
                    barnasAktørerOgFødselsdatoer = personopplysningGrunnlag.barna.map {
                        Pair(
                            it.aktør,
                            it.fødselsdato
                        )
                    }
                )
        } else {
            emptyList()
        }

        val andelerTilkjentYtelseBarnaMedAlleEndringer = oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
            andelTilkjentYtelserUtenEndringer = andelerTilkjentYtelseBarnaUtenEndringer,
            endretUtbetalingAndeler = endretUtbetalingAndelerBarna
        )

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelseBarnaMedAlleEndringer.map { it.andel } + andelerTilkjentYtelseUtvidetMedAlleEndringer.map { it.andel } + andelerTilkjentYtelseSmåbarnstillegg.map { it.andel })

        return tilkjentYtelse
    }

    fun erSmåbarnstilleggMulig(
        utvidetAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        barnasAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
    ): Boolean = utvidetAndeler.isNotEmpty() && barnasAndeler.isNotEmpty()

    internal fun beregnAndelerTilkjentYtelseForBarna(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        personResultater: Set<PersonResultat>
    ): List<BeregnetAndel> {
        val tidslinjerMedRettTilProsentPerBarn =
            personResultater.lagTidslinjerMedRettTilProsentPerBarn(personopplysningGrunnlag)

        return tidslinjerMedRettTilProsentPerBarn.flatMap { (barn, tidslinjeMedRettTilProsentForBarn) ->
            val satsTidslinje = lagOrdinærTidslinje(barn)
            val satsProsentTidslinje = kombinerProsentOgSatsTidslinjer(tidslinjeMedRettTilProsentForBarn, satsTidslinje)

            satsProsentTidslinje.perioder().map {
                val innholdIPeriode = it.innhold
                    ?: throw Feil("Finner ikke sats og prosent i periode (${it.fraOgMed} - ${it.tilOgMed}) ved generering av andeler tilkjent ytelse")
                BeregnetAndel(
                    person = barn,
                    stønadFom = it.fraOgMed.tilYearMonth(),
                    stønadTom = it.tilOgMed.tilYearMonth(),
                    beløp = innholdIPeriode.sats.avrundetHeltallAvProsent(innholdIPeriode.prosent),
                    sats = innholdIPeriode.sats,
                    prosent = innholdIPeriode.prosent
                )
            }
        }
    }

    private fun kombinerProsentOgSatsTidslinjer(
        tidslinjeMedRettTilProsentForBarn: Tidslinje<BigDecimal, Måned>,
        satsTidslinje: Tidslinje<Int, Måned>
    ) = tidslinjeMedRettTilProsentForBarn.kombinerMed(satsTidslinje) { rettTilProsent, sats ->
        when {
            rettTilProsent == null -> null
            sats == null -> throw Feil("Finner ikke sats i periode med rett til utbetaling")
            else -> SatsProsent(sats, rettTilProsent)
        }
    }.slåSammenLike().filtrerIkkeNull()

    private data class SatsProsent(
        val sats: Int,
        val prosent: BigDecimal
    )

    private fun Set<PersonResultat>.lagTidslinjerMedRettTilProsentPerBarn(personopplysningGrunnlag: PersonopplysningGrunnlag): Map<Person, Tidslinje<BigDecimal, Måned>> {
        val tidslinjerPerPerson = lagTidslinjerMedRettTilProsentPerPerson(personopplysningGrunnlag)

        if (tidslinjerPerPerson.isEmpty()) return emptyMap()

        val søkerTidslinje = tidslinjerPerPerson[personopplysningGrunnlag.søker] ?: return emptyMap()
        val barnasTidslinjer = tidslinjerPerPerson.filter { it.key in personopplysningGrunnlag.barna }

        return kombinerSøkerMedHvertBarnSinTidslinje(barnasTidslinjer, søkerTidslinje)
    }

    private fun kombinerSøkerMedHvertBarnSinTidslinje(
        barnasTidslinjer: Map<Person, Tidslinje<BigDecimal, Måned>>,
        søkerTidslinje: Tidslinje<BigDecimal, Måned>
    ) = barnasTidslinjer.mapValues { (_, barnTidslinje) ->
        barnTidslinje.kombinerMed(søkerTidslinje) { barnProsent, søkerProsent ->
            when {
                barnProsent == null || søkerProsent == null -> null
                else -> barnProsent
            }
        }.slåSammenLike().filtrerIkkeNull()
    }

    private fun Set<PersonResultat>.lagTidslinjerMedRettTilProsentPerPerson(
        personopplysningGrunnlag: PersonopplysningGrunnlag
    ) = this.associate { personResultat ->
        val person = personopplysningGrunnlag.personer.find { it.aktør == personResultat.aktør }
            ?: throw Feil("Finner ikke person med aktørId=${personResultat.aktør.aktørId} i persongrunnlaget ved generering av andeler tilkjent ytelse")
        person to personResultat.tilTidslinjeMedRettTilProsentForPerson(
            fødselsdato = person.fødselsdato,
            personType = person.type
        )
    }

    internal fun PersonResultat.tilTidslinjeMedRettTilProsentForPerson(
        fødselsdato: LocalDate,
        personType: PersonType
    ): Tidslinje<BigDecimal, Måned> {
        val tidslinjer = vilkårResultater.tilForskjøvetTidslinjerForHvertOppfylteVilkår(fødselsdato)

        return tidslinjer.kombiner { it.mapTilProsentEllerNull(personType) }.slåSammenLike().filtrerIkkeNull()
    }

    internal fun Iterable<VilkårResultat>.mapTilProsentEllerNull(personType: PersonType): BigDecimal? {
        return if (alleVilkårErOppfylt(personType)) {
            if (any { it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) }) {
                BigDecimal(50)
            } else {
                BigDecimal(100)
            }
        } else {
            null
        }
    }

    fun beregnTilkjentYtelseUtvidet(
        utvidetVilkår: List<VilkårResultat>,
        andelerTilkjentYtelseBarnaMedEtterbetaling3ÅrEndringer: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        tilkjentYtelse: TilkjentYtelse,
        endretUtbetalingAndelerSøker: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        val andelerTilkjentYtelseUtvidet = UtvidetBarnetrygdGenerator(
            behandlingId = tilkjentYtelse.behandling.id,
            tilkjentYtelse = tilkjentYtelse
        )
            .lagUtvidetBarnetrygdAndeler(
                utvidetVilkår = utvidetVilkår,
                andelerBarna = andelerTilkjentYtelseBarnaMedEtterbetaling3ÅrEndringer.map { it.andel }
            )

        return oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
            andelTilkjentYtelserUtenEndringer = andelerTilkjentYtelseUtvidet,
            endretUtbetalingAndeler = endretUtbetalingAndelerSøker
        )
    }

    private fun finnUtvidetVilkår(vilkårsvurdering: Vilkårsvurdering) =
        vilkårsvurdering.personResultater
            .flatMap { it.vilkårResultater }
            .filter { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD && it.resultat == Resultat.OPPFYLT }
            .also { utvidetVilkårsresultater -> utvidetVilkårsresultater.forEach { validerUtvidetVilkårsresultat(it) } }

    private fun validerUtvidetVilkårsresultat(vilkårResultat: VilkårResultat) {
        val fom = vilkårResultat.periodeFom?.toYearMonth()
        val tom = vilkårResultat.periodeTom?.toYearMonth()

        if (fom == null) {
            throw Feil("Fom må være satt på søkers periode ved utvidet barnetrygd")
        }
        if (fom == tom) {
            throw FunksjonellFeil("Du kan ikke legge inn fom og tom innenfor samme kalendermåned. Gå til utvidet barnetrygd vilkåret for å endre")
        }
    }

    fun oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
        andelTilkjentYtelserUtenEndringer: Collection<AndelTilkjentYtelse>,
        endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        // Denne bør slettes hvis det ikke har forekommet i prod
        if (andelTilkjentYtelserUtenEndringer.any { it.endretUtbetalingAndeler.size > 0 }) {
            throw IllegalArgumentException("Fikk andeler som inneholdt endringer. Det skulle ikke ha skjedd")
        }

        if (endretUtbetalingAndeler.isEmpty()) {
            return andelTilkjentYtelserUtenEndringer
                .map { AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(it.copy()) }
        }

        val (andelerUtenSmåbarnstillegg, andelerMedSmåbarnstillegg) = andelTilkjentYtelserUtenEndringer.partition { !it.erSmåbarnstillegg() }

        val nyeAndelTilkjentYtelse = mutableListOf<AndelTilkjentYtelseMedEndreteUtbetalinger>()

        andelerUtenSmåbarnstillegg.groupBy { it.aktør }.forEach { andelerForPerson ->
            val aktør = andelerForPerson.key
            val endringerForPerson =
                endretUtbetalingAndeler.filter { it.person?.aktør == aktør }

            val nyeAndelerForPerson = mutableListOf<AndelTilkjentYtelseMedEndreteUtbetalinger>()

            andelerForPerson.value.forEach { andelForPerson ->
                // Deler opp hver enkelt andel i perioder som hhv blir berørt av endringene og de som ikke berøres av de.
                val (perioderMedEndring, perioderUtenEndring) = andelForPerson.stønadsPeriode()
                    .perioderMedOgUtenOverlapp(
                        endringerForPerson.map { endringerForPerson -> endringerForPerson.periode }
                    )
                // Legger til nye AndelTilkjentYtelse for perioder som er berørt av endringer.
                nyeAndelerForPerson.addAll(
                    perioderMedEndring.map { månedPeriodeEndret ->
                        val endretUtbetalingMedAndeler =
                            endringerForPerson.single { it.overlapperMed(månedPeriodeEndret) }
                        val nyttNasjonaltPeriodebeløp = andelForPerson.sats
                            .avrundetHeltallAvProsent(endretUtbetalingMedAndeler.prosent!!)

                        val andelTilkjentYtelse = andelForPerson.copy(
                            prosent = endretUtbetalingMedAndeler.prosent!!,
                            stønadFom = månedPeriodeEndret.fom,
                            stønadTom = månedPeriodeEndret.tom,
                            kalkulertUtbetalingsbeløp = nyttNasjonaltPeriodebeløp,
                            nasjonaltPeriodebeløp = nyttNasjonaltPeriodebeløp,
                            endretUtbetalingAndeler = mutableListOf(endretUtbetalingMedAndeler.endretUtbetalingAndel)
                        )

                        andelTilkjentYtelse.medEndring(endretUtbetalingMedAndeler)
                    }
                )
                // Legger til nye AndelTilkjentYtelse for perioder som ikke berøres av endringer.
                nyeAndelerForPerson.addAll(
                    perioderUtenEndring.map { månedPeriodeUendret ->
                        val andelTilkjentYtelse = andelForPerson.copy(
                            stønadFom = månedPeriodeUendret.fom,
                            stønadTom = månedPeriodeUendret.tom
                        )
                        AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(andelTilkjentYtelse)
                    }
                )
            }

            val nyeAndelerForPersonEtterSammenslåing =
                slåSammenPerioderSomIkkeSkulleHaVærtSplittet(
                    andelerTilkjentYtelseMedEndreteUtbetalinger = nyeAndelerForPerson,
                    skalAndelerSlåsSammen = ::skalAndelerSlåsSammen
                )

            nyeAndelTilkjentYtelse.addAll(nyeAndelerForPersonEtterSammenslåing)
        }

        // Ettersom vi aldri ønsker å overstyre småbarnstillegg perioder fjerner vi dem og legger dem til igjen her
        nyeAndelTilkjentYtelse.addAll(
            andelerMedSmåbarnstillegg.map {
                AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(it)
            }
        )

        // Sorterer primært av hensyn til måten testene er implementert og kan muligens fjernes dersom dette skrives om.
        nyeAndelTilkjentYtelse.sortWith(
            compareBy(
                { it.aktør.aktivFødselsnummer() },
                { it.stønadFom }
            )
        )
        return nyeAndelTilkjentYtelse
    }

    fun slåSammenPerioderSomIkkeSkulleHaVærtSplittet(
        andelerTilkjentYtelseMedEndreteUtbetalinger: MutableList<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        skalAndelerSlåsSammen: (førsteAndel: AndelTilkjentYtelseMedEndreteUtbetalinger, nesteAndel: AndelTilkjentYtelseMedEndreteUtbetalinger) -> Boolean
    ): MutableList<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        val sorterteAndeler = andelerTilkjentYtelseMedEndreteUtbetalinger.sortedBy { it.stønadFom }.toMutableList()
        var periodenViSerPå = sorterteAndeler.first()
        val oppdatertListeMedAndeler = mutableListOf<AndelTilkjentYtelseMedEndreteUtbetalinger>()

        for (index in 0 until sorterteAndeler.size) {
            val andel = sorterteAndeler[index]
            val nesteAndel = if (index == sorterteAndeler.size - 1) null else sorterteAndeler[index + 1]

            periodenViSerPå = if (nesteAndel != null) {
                val andelerSkalSlåsSammen =
                    skalAndelerSlåsSammen(andel, nesteAndel)

                if (andelerSkalSlåsSammen) {
                    val nyAndel = periodenViSerPå.slåSammenMed(nesteAndel)
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
    fun skalAndelerSlåsSammen(
        førsteAndel: AndelTilkjentYtelseMedEndreteUtbetalinger,
        nesteAndel: AndelTilkjentYtelseMedEndreteUtbetalinger
    ): Boolean =
        førsteAndel.stønadTom.sisteDagIInneværendeMåned()
            .erDagenFør(nesteAndel.stønadFom.førsteDagIInneværendeMåned()) && førsteAndel.prosent == BigDecimal(0) && nesteAndel.prosent == BigDecimal(
            0
        ) && førsteAndel.endreteUtbetalinger.isNotEmpty() && førsteAndel.endreteUtbetalinger.singleOrNull() == nesteAndel.endreteUtbetalinger.singleOrNull()

    private fun finnYtelseType(
        underkategori: BehandlingUnderkategori,
        personType: PersonType
    ): YtelseType {
        return when (underkategori) {
            BehandlingUnderkategori.UTVIDET -> when (personType) {
                PersonType.SØKER -> YtelseType.UTVIDET_BARNETRYGD
                PersonType.BARN -> YtelseType.ORDINÆR_BARNETRYGD
                PersonType.ANNENPART -> throw Feil("Utvidet barnetrygd kan ikke værre knyttet til Annen part")
            }
            BehandlingUnderkategori.ORDINÆR -> when (personType) {
                PersonType.BARN -> YtelseType.ORDINÆR_BARNETRYGD
                PersonType.SØKER, PersonType.ANNENPART -> throw Feil("Ordinær barnetrygd kan bare være knyttet til barn")
            }
            BehandlingUnderkategori.INSTITUSJON -> when (personType) {
                PersonType.BARN -> YtelseType.ORDINÆR_BARNETRYGD
                PersonType.SØKER, PersonType.ANNENPART -> throw Feil("Institusjon kan ikke være knyttet til noe annet enn barn")
            }
        }
    }
}

fun MånedPeriode.perioderMedOgUtenOverlapp(perioder: List<MånedPeriode>): Pair<List<MånedPeriode>, List<MånedPeriode>> {
    if (perioder.isEmpty()) return Pair(emptyList(), listOf(this))

    val alleMånederMedOverlappstatus = mutableMapOf<YearMonth, Boolean>()
    var nesteMåned = this.fom
    while (nesteMåned <= this.tom) {
        alleMånederMedOverlappstatus[nesteMåned] =
            perioder.any { månedPeriode -> månedPeriode.inkluderer(nesteMåned) }
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

        if (periodeMedOverlapp) {
            perioderMedOverlapp.add(MånedPeriode(periodeStart, periodeSlutt))
        } else {
            perioderUtenOverlapp.add(MånedPeriode(periodeStart, periodeSlutt))
        }

        periodeStart = alleMånederMedOverlappstatus
            .filter { it.key > periodeSlutt }
            .minByOrNull { it.key }?.key
    }
    return Pair(perioderMedOverlapp, perioderUtenOverlapp)
}

internal data class BeregnetAndel(
    val person: Person,
    val stønadFom: YearMonth,
    val stønadTom: YearMonth,
    val beløp: Int,
    val sats: Int,
    val prosent: BigDecimal
)
