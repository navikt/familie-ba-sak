package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.erDagenFør
import no.nav.familie.ba.sak.common.erMellom
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.tilMånedPeriode
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.finnTilOgMedDato
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.YearMonth

object EndretUtbetalingAndelValidering {

    fun validerIngenOverlappendeEndring(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        eksisterendeEndringerPåBehandling: List<EndretUtbetalingAndel>
    ) {

        endretUtbetalingAndel.validerUtfyltEndring()
        if (eksisterendeEndringerPåBehandling.any
            {
                it.overlapperMed(endretUtbetalingAndel.periode) &&
                    it.person == endretUtbetalingAndel.person &&
                    it.årsak == endretUtbetalingAndel.årsak
            }
        ) {
            throw FunksjonellFeil(
                melding = "Perioden som blir forsøkt lagt til overlapper med eksisterende periode på person.",
                frontendFeilmelding = "Perioden du forsøker å legge til overlapper med eksisterende periode på personen. Om dette er ønskelig må du først endre den eksisterende perioden."
            )
        }
    }

    fun validerPeriodeInnenforTilkjentytelse(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        andelTilkjentYtelser: List<AndelTilkjentYtelse>
    ) {

        endretUtbetalingAndel.validerUtfyltEndring()
        val minsteDatoForTilkjentYtelse = andelTilkjentYtelser.filter {
            it.aktør == endretUtbetalingAndel.person!!.aktør
        }.minByOrNull { it.stønadFom }?.stønadFom

        val størsteDatoForTilkjentYtelse = andelTilkjentYtelser.filter {
            it.aktør == endretUtbetalingAndel.person!!.aktør
        }.maxByOrNull { it.stønadTom }?.stønadTom

        if ((minsteDatoForTilkjentYtelse == null || størsteDatoForTilkjentYtelse == null) ||
            (
                endretUtbetalingAndel.fom!!.isBefore(minsteDatoForTilkjentYtelse) ||
                    endretUtbetalingAndel.tom!!.isAfter(størsteDatoForTilkjentYtelse)
                )
        ) {
            throw FunksjonellFeil(
                melding = "Det er ingen tilkjent ytelse for personen det blir forsøkt lagt til en endret periode for.",
                frontendFeilmelding = "Du har valgt en periode der det ikke finnes tilkjent ytelse for valgt person i hele eller deler av perioden."
            )
        }
    }

    fun validerÅrsak(
        behandling: Behandling,
        årsak: Årsak?,
        endretUtbetalingAndel: EndretUtbetalingAndel,
        vilkårsvurdering: Vilkårsvurdering?
    ) {
        if (årsak == Årsak.DELT_BOSTED) {
            val deltBostedPerioder = finnDeltBostedPerioder(behandling = behandling, personAktør = endretUtbetalingAndel.person?.aktør, vilkårsvurdering = vilkårsvurdering).map { it.tilMånedPeriode() }
            validerDeltBosted(endretUtbetalingAndel = endretUtbetalingAndel, deltBostedPerioder = deltBostedPerioder)
        }
    }

    fun validerDeltBosted(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        deltBostedPerioder: List<MånedPeriode>
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
                frontendFeilmelding = "Du har valgt årsaken 'delt bosted', denne samstemmer ikke med vurderingene gjort på vilkårsvurderingssiden i perioden du har valgt."
            )
        }
    }

    fun validerAtAlleOpprettedeEndringerErUtfylt(endretUtbetalingAndeler: List<EndretUtbetalingAndel>) {
        runCatching {
            endretUtbetalingAndeler.forEach { it.validerUtfyltEndring() }
        }.onFailure {
            throw FunksjonellFeil(
                melding = "Det er opprettet instanser av EndretUtbetalingandel som ikke er fylt ut før navigering til neste steg.",
                frontendFeilmelding = "Du har opprettet en eller flere endrede utbetalingsperioder som er ufullstendig utfylt. Disse må enten fylles ut eller slettes før du kan gå videre."
            )
        }
    }

    fun validerAtEndringerErTilknyttetAndelTilkjentYtelse(endretUtbetalingAndeler: List<EndretUtbetalingAndel>) {
        if (endretUtbetalingAndeler.any { it.andelTilkjentYtelser.isEmpty() })
            throw FunksjonellFeil(
                melding = "Det er opprettet instanser av EndretUtbetalingandel som ikke er tilknyttet noen andeler. De må enten lagres eller slettes av SB.",
                frontendFeilmelding = "Du har endrede utbetalingsperioder. Bekreft, slett eller oppdater periodene i listen."
            )
    }
}

fun validerDeltBostedEndringerIkkeKrysserUtvidetYtelse(
    endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
) {
    fun EndretUtbetalingAndel.finnKryssendeUtvidetYtelse(
        andelTilkjentYtelser: List<AndelTilkjentYtelse>,
    ): AndelTilkjentYtelse? =
        andelTilkjentYtelser
            .filter { it.type == YtelseType.UTVIDET_BARNETRYGD }
            .find {
                it.overlapperPeriode(MånedPeriode(this.fom!!, this.tom!!)) &&
                    (this.fom!! < it.stønadFom || this.tom!! > it.stønadTom)
            }

    endretUtbetalingAndeler.forEach {
        val kryssendeTilkjentYtelse = it.finnKryssendeUtvidetYtelse(
            andelerTilkjentYtelse
        )
        if (it.årsakErDeltBosted() && kryssendeTilkjentYtelse != null) {
            val feilmelding =
                "Delt bosted endring fra ${it.fom?.tilKortString()} til ${it.tom?.tilKortString()} krysser " +
                    "starten eller slutten på den utvidede perioden fra " +
                    "${kryssendeTilkjentYtelse.stønadFom.tilKortString()} " +
                    "til ${kryssendeTilkjentYtelse.stønadTom.tilKortString()}. " +
                    "Om endringen er i riktig periode må du opprette to endringsperioder, en utenfor" +
                    " og en inni den utvidede ytelsen."
            throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
        }
    }
}

fun validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer(
    endretUtbetalingAndeler: List<EndretUtbetalingAndel>
) {
    val endredeUtvidetUtbetalingerAndeler =
        endretUtbetalingAndeler.filter { endretUtbetaling ->
            endretUtbetaling.andelTilkjentYtelser.any { it.erUtvidet() }
        }

    endredeUtvidetUtbetalingerAndeler.forEach { endretPåUtvidetUtbetalinger ->
        val deltBostedEndringerISammePeriode = endretUtbetalingAndeler.filter {
            it.årsak == Årsak.DELT_BOSTED &&
                it.fom == endretPåUtvidetUtbetalinger.fom &&
                it.tom == endretPåUtvidetUtbetalinger.tom &&
                it.id != endretPåUtvidetUtbetalinger.id
        }

        if (deltBostedEndringerISammePeriode.isEmpty()) {
            val feilmelding =
                "Det kan ikke være en endring på en utvidet ytelse uten en endring på en delt bosted ytelse. " +
                    "Legg til en delt bosted endring i perioden ${endretPåUtvidetUtbetalinger.fom} til " +
                    "${endretPåUtvidetUtbetalinger.tom} eller fjern endringen på den utvidede ytelsen."
            throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
        }

        val erForskjelligEndringPåutvidetOgDeltBostedISammePeriode =
            deltBostedEndringerISammePeriode.any { endretPåUtvidetUtbetalinger.prosent != it.prosent }

        if (erForskjelligEndringPåutvidetOgDeltBostedISammePeriode) {
            val feilmelding =
                "Endring på delt bosted ytelse og utvidet ytelse i samme periode må være lik, " +
                    "men endringene i perioden ${endretPåUtvidetUtbetalinger.fom} til ${endretPåUtvidetUtbetalinger.tom} " +
                    "er forskjellige."
            throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
        }
    }
}

fun validerUtbetalingMotÅrsak(årsak: Årsak?, skalUtbetales: Boolean) {
    if (skalUtbetales && (årsak == Årsak.ENDRE_MOTTAKER || årsak == Årsak.ALLEREDE_UTBETALT)) {
        val feilmelding = "Du kan ikke velge denne årsaken og si at barnetrygden skal utbetales."
        throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
    }
}

fun validerTomDato(tomDato: YearMonth?, gyldigTomEtterDagensDato: YearMonth?, årsak: Årsak?) {
    val dagensDato = YearMonth.now()
    if (årsak == Årsak.ALLEREDE_UTBETALT && tomDato?.isAfter(dagensDato) == true) {
        val feilmelding = "For årsak '${årsak.visningsnavn}' kan du ikke legge inn til og med dato som er i neste måned eller senere."
        throw FunksjonellFeil(
            frontendFeilmelding = feilmelding,
            melding = feilmelding
        )
    }
    if (tomDato?.isAfter(dagensDato) == true && tomDato != gyldigTomEtterDagensDato) {
        val feilmelding = "Du kan ikke legge inn til og med dato som er i neste måned eller senere. Om det gjelder en løpende periode vil systemet legge inn riktig dato for deg."
        throw FunksjonellFeil(
            frontendFeilmelding = feilmelding,
            melding = feilmelding
        )
    }
}

private fun slåSammenDeltBostedPerioderSomIkkeSkulleHaVærtSplittet(
    perioder: MutableList<Periode>,
): MutableList<Periode> {
    if (perioder.isEmpty()) return mutableListOf()
    val sortertePerioder = perioder.sortedBy { it.fom }.toMutableList()
    var periodenViSerPå: Periode = sortertePerioder.first()
    val oppdatertListeMedPerioder = mutableListOf<Periode>()

    for (index in 0 until sortertePerioder.size) {
        val periode = sortertePerioder[index]
        val nestePeriode = if (index == sortertePerioder.size - 1) null else sortertePerioder[index + 1]

        periodenViSerPå = if (nestePeriode != null) {
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
    vilkår: List<VilkårResultat>
): Periode? {
    if (this.periodeFom == null) return null
    val fraOgMedDato = this.periodeFom!!.førsteDagINesteMåned()
    val tilOgMedDato = finnTilOgMedDato(tilOgMed = this.periodeTom, vilkårResultater = vilkår)
    if (fraOgMedDato.toYearMonth().isAfter(tilOgMedDato.toYearMonth())) return null
    return Periode(
        fom = fraOgMedDato,
        tom = tilOgMedDato
    )
}

private fun finnDeltBostedPerioder(
    behandling: Behandling,
    personAktør: Aktør?,
    vilkårsvurdering: Vilkårsvurdering?
): List<Periode> {
    if (vilkårsvurdering == null) return emptyList()
    val personensVilkår = vilkårsvurdering.personResultater.single { it.aktør == personAktør }

    val deltBostedVilkårResultater = personensVilkår.vilkårResultater.filter {
        it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) && it.resultat == Resultat.OPPFYLT
    }

    val deltBostedPerioder = deltBostedVilkårResultater.mapNotNull { it.tilPeriode(vilkår = deltBostedVilkårResultater) }

    return slåSammenDeltBostedPerioderSomIkkeSkulleHaVærtSplittet(
        perioder = deltBostedPerioder.map {
            Periode(
                fom = it.fom,
                tom = it.tom
            )
        }.toMutableList()
    )
}
