package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.VilkårFeil
import no.nav.familie.ba.sak.common.erBack2BackIMånedsskifte
import no.nav.familie.ba.sak.common.erDagenFør
import no.nav.familie.ba.sak.common.erMellom
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.slåSammenOverlappendePerioder
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.common.tilMånedPeriode
import no.nav.familie.ba.sak.common.toPeriode
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.hentGyldigEtterbetaling3MndFom
import no.nav.familie.ba.sak.kjerne.beregning.hentGyldigEtterbetaling3ÅrFom
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonEnkel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

object EndretUtbetalingAndelValidering {
    fun validerOgSettTomDatoHvisNull(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        andreEndredeAndelerPåBehandling: List<EndretUtbetalingAndel>,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
        vilkårsvurdering: Vilkårsvurdering,
    ) {
        val gyldigTomEtterDagensDato =
            beregnGyldigTom(
                andreEndredeAndelerPåBehandling = andreEndredeAndelerPåBehandling,
                endretUtbetalingAndel = endretUtbetalingAndel,
                andelTilkjentYtelser = andelerTilkjentYtelse,
            )

        validerTomDato(
            tomDato = endretUtbetalingAndel.tom,
            gyldigTomEtterDagensDato = gyldigTomEtterDagensDato,
            årsak = endretUtbetalingAndel.årsak,
        )

        if (endretUtbetalingAndel.tom == null) {
            endretUtbetalingAndel.tom = gyldigTomEtterDagensDato
        }

        validerÅrsak(
            endretUtbetalingAndel = endretUtbetalingAndel,
            vilkårsvurdering = vilkårsvurdering,
        )

        validerUtbetalingMotÅrsak(
            årsak = endretUtbetalingAndel.årsak,
            skalUtbetales = endretUtbetalingAndel.prosent != BigDecimal(0),
        )

        validerIngenOverlappendeEndring(
            endretUtbetalingAndel = endretUtbetalingAndel,
            eksisterendeEndringerPåBehandling = andreEndredeAndelerPåBehandling,
        )

        validerPeriodeInnenforTilkjentytelse(
            endretUtbetalingAndel = endretUtbetalingAndel,
            andelTilkjentYtelser = andelerTilkjentYtelse,
        )
    }

    fun validerIngenOverlappendeEndring(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        eksisterendeEndringerPåBehandling: List<EndretUtbetalingAndel>,
    ) {
        endretUtbetalingAndel.validerUtfyltEndring()
        if (eksisterendeEndringerPåBehandling.any {
                it.overlapperMed(endretUtbetalingAndel.periode) &&
                    it.personer.intersect(endretUtbetalingAndel.personer).isNotEmpty()
            }
        ) {
            throw FunksjonellFeil(
                melding = "Perioden som blir forsøkt lagt til overlapper med eksisterende periode for en av personene.",
                frontendFeilmelding = "Perioden du forsøker å legge til overlapper med eksisterende periode for en av personene. Om dette er ønskelig må du først endre den eksisterende perioden.",
            )
        }
    }

    fun validerPeriodeInnenforTilkjentytelse(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        andelTilkjentYtelser: Collection<AndelTilkjentYtelse>,
    ) {
        endretUtbetalingAndel.validerUtfyltEndring()
        endretUtbetalingAndel.personer.forEach { person ->
            val minsteDatoForTilkjentYtelse =
                andelTilkjentYtelser
                    .filter { it.aktør == person.aktør }
                    .minByOrNull { it.stønadFom }
                    ?.stønadFom

            val størsteDatoForTilkjentYtelse =
                andelTilkjentYtelser
                    .filter { it.aktør == person.aktør }
                    .maxByOrNull { it.stønadTom }
                    ?.stønadTom

            if ((minsteDatoForTilkjentYtelse == null || størsteDatoForTilkjentYtelse == null) ||
                (
                    endretUtbetalingAndel.fom!!.isBefore(minsteDatoForTilkjentYtelse) ||
                        endretUtbetalingAndel.tom!!.isAfter(størsteDatoForTilkjentYtelse)
                )
            ) {
                throw FunksjonellFeil(
                    melding = "Det er ingen tilkjent ytelse for en av personene det blir forsøkt lagt til en endret periode for.",
                    frontendFeilmelding = "Du har valgt en periode der det ikke finnes utbetalinger for en av de valgte persone i hele eller deler av perioden.",
                )
            }
        }
    }

    fun validerPeriodeInnenforTilkjentytelse(
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
        andelTilkjentYtelser: Collection<AndelTilkjentYtelse>,
    ) = endretUtbetalingAndeler.forEach { validerPeriodeInnenforTilkjentytelse(it, andelTilkjentYtelser) }

    fun validerÅrsak(
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
        vilkårsvurdering: Vilkårsvurdering?,
    ) = endretUtbetalingAndeler.forEach { validerÅrsak(it, vilkårsvurdering) }

    fun validerÅrsak(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        vilkårsvurdering: Vilkårsvurdering?,
    ) {
        val årsak = endretUtbetalingAndel.årsak ?: return

        return when (årsak) {
            Årsak.DELT_BOSTED -> {
                endretUtbetalingAndel.personer.forEach { person ->
                    val deltBostedPerioder =
                        finnDeltBostedPerioder(
                            person = person,
                            vilkårsvurdering = vilkårsvurdering,
                        ).map { it.tilMånedPeriode() }

                    validerDeltBosted(
                        endretUtbetalingAndel = endretUtbetalingAndel,
                        deltBostedPerioder = deltBostedPerioder,
                    )
                }
            }

            Årsak.ETTERBETALING_3MND,
            Årsak.ETTERBETALING_3ÅR,
            ->
                validerEtterbetalingMaks3ÅrEller3MndFørSøknadstidspunkt(
                    endretUtbetalingAndel = endretUtbetalingAndel,
                    behandlingOpprettetTidspunkt = vilkårsvurdering?.behandling?.opprettetTidspunkt?.toLocalDate(),
                )

            Årsak.ALLEREDE_UTBETALT -> validerAlleredeUtbetalt(endretUtbetalingAndel = endretUtbetalingAndel)

            Årsak.ENDRE_MOTTAKER -> {}
        }
    }

    private fun validerAlleredeUtbetalt(endretUtbetalingAndel: EndretUtbetalingAndel) {
        if (endretUtbetalingAndel.tom?.isAfter(YearMonth.now()) == true) {
            throw FunksjonellFeil("Du har valgt årsaken allerede utbetalt. Du kan ikke velge denne årsaken og en til og med dato frem i tid. Ta kontakt med superbruker om du er usikker på hva du skal gjøre.")
        }
    }

    private fun validerEtterbetalingMaks3ÅrEller3MndFørSøknadstidspunkt(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        behandlingOpprettetTidspunkt: LocalDate?,
    ) {
        val kravDato = endretUtbetalingAndel.søknadstidspunkt ?: behandlingOpprettetTidspunkt
        val (feilmeldingPeriode, gyldigEtterbetalingFom) =
            when (endretUtbetalingAndel.årsak) {
                Årsak.ETTERBETALING_3ÅR -> "tre år" to hentGyldigEtterbetaling3ÅrFom(kravDato = kravDato ?: LocalDate.now())
                Årsak.ETTERBETALING_3MND -> "tre måneder" to hentGyldigEtterbetaling3MndFom(kravDato = kravDato ?: LocalDate.now())
                else -> throw FunksjonellFeil("Ugyldig årsak for etterbetaling")
            }

        if (endretUtbetalingAndel.prosent == BigDecimal.valueOf(100)) {
            throw FunksjonellFeil("Du kan ikke endre til full utbetaling når det er mer enn $feilmeldingPeriode siden søknadstidspunktet.")
        } else if (endretUtbetalingAndel.tom?.isSameOrAfter(gyldigEtterbetalingFom) == true) {
            throw FunksjonellFeil("Du kan kun stoppe etterbetaling for en periode som strekker seg mer enn $feilmeldingPeriode tilbake i tid.")
        }
    }

    internal fun validerDeltBosted(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        deltBostedPerioder: List<MånedPeriode>,
    ) {
        if (endretUtbetalingAndel.årsak != Årsak.DELT_BOSTED) return

        if (endretUtbetalingAndel.fom == null || endretUtbetalingAndel.tom == null) {
            throw FunksjonellFeil("Du må sette fom og tom.")
        }
        val endringsperiode = MånedPeriode(fom = endretUtbetalingAndel.fom!!, tom = endretUtbetalingAndel.tom!!)

        if (
            !deltBostedPerioder.any {
                endringsperiode.erMellom(MånedPeriode(fom = it.fom, tom = it.tom))
            }
        ) {
            throw FunksjonellFeil(
                melding = "Det er ingen sats for delt bosted i perioden det opprettes en endring med årsak delt bosted for.",
                frontendFeilmelding = "Du har valgt årsaken 'delt bosted', denne samstemmer ikke med vurderingene gjort på vilkårsvurderingssiden i perioden du har valgt.",
            )
        }
    }

    fun validerAtAlleOpprettedeEndringerErUtfylt(endretUtbetalingAndeler: List<EndretUtbetalingAndel>) {
        runCatching {
            endretUtbetalingAndeler.forEach { it.validerUtfyltEndring() }
        }.onFailure {
            throw FunksjonellFeil(
                melding = "Det er opprettet instanser av EndretUtbetalingandel som ikke er fylt ut før navigering til neste steg.",
                frontendFeilmelding = "Du har opprettet en eller flere endrede utbetalingsperioder som er ufullstendig utfylt. Disse må enten fylles ut eller slettes før du kan gå videre.",
            )
        }
    }

    fun validerAtEndringerErTilknyttetAndelTilkjentYtelse(endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>) {
        if (endretUtbetalingAndeler.any { it.andelerTilkjentYtelse.isEmpty() }) {
            throw FunksjonellFeil(
                melding = "Det er opprettet instanser av EndretUtbetalingandel som ikke er tilknyttet noen andeler. De må enten lagres eller slettes av SB.",
                frontendFeilmelding = "Du har endrede utbetalingsperioder. Bekreft, slett eller oppdater periodene i listen.",
            )
        }
    }
}

fun validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer(
    endretUtbetalingAndelerMedÅrsakDeltBosted: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>,
) {
    val endredeUtvidetUtbetalingerAndeler =
        endretUtbetalingAndelerMedÅrsakDeltBosted
            .filter { endretUtbetaling ->
                endretUtbetaling.andelerTilkjentYtelse.any { it.erUtvidet() }
            }

    endredeUtvidetUtbetalingerAndeler.forEach { endretPåUtvidetUtbetalinger ->
        val endretUtbetalingAndelInneholderBarn = endretPåUtvidetUtbetalinger.personer.any { it.type == PersonType.BARN }

        val deltBostedEndringerISammePeriode =
            endretUtbetalingAndelerMedÅrsakDeltBosted.filter {
                it.årsak == Årsak.DELT_BOSTED &&
                    it.fom!!.isSameOrBefore(endretPåUtvidetUtbetalinger.fom!!) &&
                    it.tom!!.isSameOrAfter(endretPåUtvidetUtbetalinger.tom!!) &&
                    it.id != endretPåUtvidetUtbetalinger.id
            }

        if (!endretUtbetalingAndelInneholderBarn && deltBostedEndringerISammePeriode.isEmpty()) {
            val feilmelding =
                "Det kan ikke være en endring på en utvidet ytelse uten en endring på en delt bosted ytelse. " +
                    "Legg til en delt bosted endring i perioden ${endretPåUtvidetUtbetalinger.fom} til " +
                    "${endretPåUtvidetUtbetalinger.tom} eller fjern endringen på den utvidede ytelsen."
            throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
        }
    }
}

fun validerUtbetalingMotÅrsak(
    årsak: Årsak?,
    skalUtbetales: Boolean,
) {
    if (skalUtbetales && (årsak == Årsak.ENDRE_MOTTAKER || årsak == Årsak.ALLEREDE_UTBETALT)) {
        val feilmelding = "Du kan ikke velge denne årsaken og si at barnetrygden skal utbetales."
        throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
    }
}

fun validerTomDato(
    tomDato: YearMonth?,
    gyldigTomEtterDagensDato: YearMonth?,
    årsak: Årsak?,
) {
    if (årsak != Årsak.ENDRE_MOTTAKER && tomDato == null) {
        throw FunksjonellFeil(melding = "Til og med-dato kan ikke være tom for årsak '${årsak?.visningsnavn}'")
    }
    val dagensDato = YearMonth.now()
    if (årsak == Årsak.ALLEREDE_UTBETALT && tomDato?.isAfter(dagensDato) == true) {
        val feilmelding =
            "For årsak '${årsak.visningsnavn}' kan du ikke legge inn til og med dato som er i neste måned eller senere."
        throw FunksjonellFeil(
            frontendFeilmelding = feilmelding,
            melding = feilmelding,
        )
    }
    if (tomDato?.isAfter(dagensDato) == true && tomDato != gyldigTomEtterDagensDato) {
        val feilmelding =
            "Du kan ikke legge inn til og med dato som er i neste måned eller senere. Om det gjelder en løpende periode vil systemet legge inn riktig dato for deg."
        throw FunksjonellFeil(
            frontendFeilmelding = feilmelding,
            melding = feilmelding,
        )
    }
}

private fun slåSammenDeltBostedPerioderSomHengerSammen(
    perioder: MutableList<Periode>,
): MutableList<Periode> {
    if (perioder.isEmpty()) return mutableListOf()
    val sortertePerioder = perioder.sortedBy { it.fom }.toMutableList()
    var periodenViSerPå: Periode = sortertePerioder.first()
    val oppdatertListeMedPerioder = mutableListOf<Periode>()

    for (index in 0 until sortertePerioder.size) {
        val periode = sortertePerioder[index]
        val nestePeriode = if (index == sortertePerioder.size - 1) null else sortertePerioder[index + 1]

        periodenViSerPå =
            if (nestePeriode != null) {
                val andelerSkalSlåsSammen =
                    periode.tom.sisteDagIMåned().erDagenFør(nestePeriode.fom.førsteDagIInneværendeMåned())

                if (andelerSkalSlåsSammen) {
                    val nyPeriode = periodenViSerPå.copy(tom = nestePeriode.tom)
                    nyPeriode
                } else {
                    oppdatertListeMedPerioder.add(periodenViSerPå)
                    sortertePerioder[index + 1]
                }
            } else {
                oppdatertListeMedPerioder.add(periodenViSerPå)
                break
            }
    }
    return oppdatertListeMedPerioder
}

private fun VilkårResultat.tilPeriode(
    vilkår: List<VilkårResultat>,
): Periode? {
    if (this.periodeFom == null) return null
    val fraOgMedDato = this.periodeFom!!.førsteDagINesteMåned()
    val tilOgMedDato = finnTilOgMedDato(tilOgMed = this.periodeTom, vilkårResultater = vilkår)
    if (fraOgMedDato.toYearMonth().isAfter(tilOgMedDato.toYearMonth())) return null
    return Periode(
        fom = fraOgMedDato,
        tom = tilOgMedDato,
    )
}

fun finnDeltBostedPerioder(
    person: Person?,
    vilkårsvurdering: Vilkårsvurdering?,
): List<Periode> {
    if (vilkårsvurdering == null || person == null) return emptyList()
    val deltBostedPerioder =
        if (person.type == PersonType.SØKER) {
            val deltBostedVilkårResultater =
                vilkårsvurdering.personResultater.flatMap { personResultat ->
                    personResultat.vilkårResultater.filter {
                        it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) && it.resultat == Resultat.OPPFYLT
                    }
                }

            val deltBostedPerioder =
                deltBostedVilkårResultater
                    .groupBy { it.personResultat?.aktør }
                    .flatMap { (_, vilkårResultater) -> vilkårResultater.mapNotNull { it.tilPeriode(vilkår = vilkårResultater) } }

            slåSammenOverlappendePerioder(
                deltBostedPerioder.map {
                    DatoIntervallEntitet(
                        fom = it.fom,
                        tom = it.tom,
                    )
                },
            ).filter { it.fom != null && it.tom != null }.map {
                Periode(
                    fom = it.fom!!,
                    tom = it.tom!!,
                )
            }
        } else {
            val personensVilkår = vilkårsvurdering.personResultater.singleOrNull { it.aktør == person.aktør } ?: return emptyList()

            val deltBostedVilkårResultater =
                personensVilkår.vilkårResultater.filter {
                    it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) && it.resultat == Resultat.OPPFYLT
                }

            deltBostedVilkårResultater.mapNotNull { it.tilPeriode(vilkår = deltBostedVilkårResultater) }
        }
    return slåSammenDeltBostedPerioderSomHengerSammen(
        perioder = deltBostedPerioder.toMutableList(),
    )
}

fun validerBarnasVilkår(
    barna: List<PersonEnkel>,
    vilkårsvurdering: Vilkårsvurdering,
) {
    val listeAvFeil = mutableListOf<String>()

    barna.map { barn ->
        vilkårsvurdering.personResultater
            .flatMap { it.vilkårResultater }
            .filter { it.personResultat?.aktør == barn.aktør }
            .forEach { vilkårResultat ->
                if (vilkårResultat.resultat == Resultat.OPPFYLT && vilkårResultat.periodeFom == null) {
                    listeAvFeil.add("Vilkår '${vilkårResultat.vilkårType}' for barn med fødselsdato ${barn.fødselsdato.tilDagMånedÅr()} mangler fom dato.")
                }
                if (vilkårResultat.periodeFom != null && vilkårResultat.toPeriode().fom.isBefore(barn.fødselsdato)) {
                    listeAvFeil.add("Vilkår '${vilkårResultat.vilkårType}' for barn med fødselsdato ${barn.fødselsdato.tilDagMånedÅr()} har fra-og-med dato før barnets fødselsdato.")
                }
                if (vilkårResultat.periodeFom != null &&
                    vilkårResultat.toPeriode().fom.isAfter(barn.fødselsdato.plusYears(18)) &&
                    vilkårResultat.vilkårType == Vilkår.UNDER_18_ÅR &&
                    vilkårResultat.erEksplisittAvslagPåSøknad != true
                ) {
                    listeAvFeil.add("Vilkår '${vilkårResultat.vilkårType}' for barn med fødselsdato ${barn.fødselsdato.tilDagMånedÅr()} har fra-og-med dato etter barnet har fylt 18.")
                }
            }
    }

    if (listeAvFeil.isNotEmpty()) {
        throw VilkårFeil(listeAvFeil.joinToString(separator = "\n"))
    }
}

fun finnTilOgMedDato(
    tilOgMed: LocalDate?,
    vilkårResultater: List<VilkårResultat>,
): LocalDate {
    // LocalDateTimeline krasjer i isTimelineOutsideInterval funksjonen dersom vi sender med TIDENES_ENDE,
    // så bruker tidenes ende minus én dag.
    if (tilOgMed == null) return TIDENES_ENDE.minusDays(1)
    val skalVidereføresEnMndEkstra =
        vilkårResultater.any { vilkårResultat ->
            erBack2BackIMånedsskifte(
                tilOgMed = tilOgMed,
                fraOgMed = vilkårResultat.periodeFom,
            )
        }

    return if (skalVidereføresEnMndEkstra) {
        tilOgMed.plusMonths(1).sisteDagIMåned()
    } else {
        tilOgMed.sisteDagIMåned()
    }
}
