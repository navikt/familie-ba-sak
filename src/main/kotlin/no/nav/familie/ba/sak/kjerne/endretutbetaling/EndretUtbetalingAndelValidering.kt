package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.erMellom
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.hentGyldigEtterbetaling3MndFom
import no.nav.familie.ba.sak.kjerne.beregning.hentGyldigEtterbetaling3ÅrFom
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.lagForskjøvetTidslinjeForOppfylteVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.tidslinje.PRAKTISK_SENESTE_DAG
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.mapVerdi
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
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
                        finnDeltBostedPerioderForPerson(
                            person = person,
                            vilkårsvurdering = vilkårsvurdering,
                        )
                    validerDeltBosted(
                        endretUtbetalingAndel = endretUtbetalingAndel,
                        deltBostedPerioder = deltBostedPerioder,
                    )
                }
            }

            Årsak.ETTERBETALING_3MND,
            Årsak.ETTERBETALING_3ÅR,
            -> {
                validerEtterbetalingMaks3ÅrEller3MndFørSøknadstidspunkt(
                    endretUtbetalingAndel = endretUtbetalingAndel,
                    behandlingOpprettetTidspunkt = vilkårsvurdering?.behandling?.opprettetTidspunkt?.toLocalDate(),
                )
            }

            Årsak.ALLEREDE_UTBETALT -> {
                validerAlleredeUtbetalt(endretUtbetalingAndel = endretUtbetalingAndel)
            }

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

    fun validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer(
        endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>,
    ) {
        val endredeUtvidetUtbetalingerAndelerMedÅrsakDeltBosted =
            endretUtbetalingAndeler
                .filter { endretUtbetaling ->
                    endretUtbetaling.årsak == Årsak.DELT_BOSTED && endretUtbetaling.andelerTilkjentYtelse.any { it.erUtvidet() }
                }

        endredeUtvidetUtbetalingerAndelerMedÅrsakDeltBosted.forEach { endretPåUtvidetUtbetalinger ->
            val endretUtbetalingAndelInneholderBarn = endretPåUtvidetUtbetalinger.personer.any { it.type == PersonType.BARN }

            val deltBostedEndringerISammePeriode =
                endretUtbetalingAndeler.filter {
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

    fun finnDeltBostedPerioderForPerson(
        person: Person?,
        vilkårsvurdering: Vilkårsvurdering?,
    ): List<MånedPeriode> {
        if (vilkårsvurdering == null || person == null) return emptyList()
        val deltBostedPerioder =
            if (person.type == PersonType.SØKER) {
                vilkårsvurdering
                    .tilOppfyltDeltBostedTidslinjePerAktør()
                    .values
                    // Kombinerer delt bosted tidslinjer for alle barn
                    .kombiner { harDeltBostedIPeriode -> harDeltBostedIPeriode.any { it } }
                    .tilSammenhengendeDeltBostedPerioder()
            } else {
                vilkårsvurdering
                    .tilOppfyltDeltBostedTidslinjePerAktør()
                    // Kun relevant med delt bosted tidslinjen for person (barnet)
                    .getOrDefault(person.aktør, tomTidslinje())
                    .tilSammenhengendeDeltBostedPerioder()
            }
        return deltBostedPerioder
    }

    private fun Vilkårsvurdering.tilOppfyltDeltBostedTidslinjePerAktør(): Map<Aktør?, Tidslinje<Boolean>> =
        this.personResultater
            .flatMap { it.vilkårResultater }
            .groupBy { it.personResultat?.aktør }
            .mapValues { (_, vilkårResultater) ->
                vilkårResultater
                    .filter { it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) }
                    .lagForskjøvetTidslinjeForOppfylteVilkår(vilkår = Vilkår.BOR_MED_SØKER)
                    .mapVerdi { vilkårResultat -> vilkårResultat != null }
            }

    private fun Tidslinje<Boolean>.tilSammenhengendeDeltBostedPerioder(): List<MånedPeriode> =
        this
            .slåSammenLikePerioder()
            .tilPerioderIkkeNull()
            .filter { it.verdi }
            .map { MånedPeriode(fom = it.fom!!.toYearMonth(), tom = it.tom?.toYearMonth() ?: PRAKTISK_SENESTE_DAG.toYearMonth()) }
}
