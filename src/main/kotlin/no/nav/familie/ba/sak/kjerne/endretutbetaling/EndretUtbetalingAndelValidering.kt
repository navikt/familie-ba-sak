package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.overlapperHeltEllerDelvisMed
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak

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
            throw UtbetalingsikkerhetFeil(
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
            it.personIdent == endretUtbetalingAndel.person!!.personIdent.ident
        }.minByOrNull { it.stønadFom }?.stønadFom

        val størsteDatoForTilkjentYtelse = andelTilkjentYtelser.filter {
            it.personIdent == endretUtbetalingAndel.person!!.personIdent.ident
        }.maxByOrNull { it.stønadTom }?.stønadTom

        if ((minsteDatoForTilkjentYtelse == null || størsteDatoForTilkjentYtelse == null) ||
            (
                endretUtbetalingAndel.fom!!.isBefore(minsteDatoForTilkjentYtelse) ||
                    endretUtbetalingAndel.tom!!.isAfter(størsteDatoForTilkjentYtelse)
                )
        ) {
            throw UtbetalingsikkerhetFeil(
                melding = "Det er ingen tilkjent ytelse for personen det blir forsøkt lagt til en endret periode for.",
                frontendFeilmelding = "Du har valgt en periode der det ikke finnes tilkjent ytelse for valgt person i hele eller deler av perioden."
            )
        }
    }

    fun validerDeltBosted(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        andelTilkjentYtelser: List<AndelTilkjentYtelse>
    ) {
        if (endretUtbetalingAndel.årsak != Årsak.DELT_BOSTED) return

        if (
            !andelTilkjentYtelser
                .filter { it.personIdent == endretUtbetalingAndel.person?.personIdent?.ident!! }
                .filter { it.stønadsPeriode().overlapperHeltEllerDelvisMed(endretUtbetalingAndel.periode) }
                .any { it.erDeltBosted() }
        ) {
            throw UtbetalingsikkerhetFeil(
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

fun validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer(
    endretUtbetalingAndeler: List<EndretUtbetalingAndel>
) {
    val endredeUtvidetUtbetalingerAndeler =
        endretUtbetalingAndeler.filter { endretUtbetaling ->
            endretUtbetaling.andelTilkjentYtelser.any { it.erUtvidet() }
        }

    val endredeUtvidetUtbetalingerMedOrdinæreAndeler =
        endredeUtvidetUtbetalingerAndeler
            .filter { it.andelTilkjentYtelser.any { andelTilkjentYtelse -> !andelTilkjentYtelse.erUtvidet() } }

    if (endredeUtvidetUtbetalingerMedOrdinæreAndeler.isNotEmpty()) {
        val feilmelding =
            "Endring på utvidet ytelse kan ikke være på ordinær ytelse også, men endring " +
                Utils.slåSammen(endredeUtvidetUtbetalingerMedOrdinæreAndeler.map { "fra ${it.fom} til ${it.tom}" }) +
                " gikk over både ordinær og utvidet utelse. " +
                "Del opp endringen(e) i to endringer, én på den ordinære delen og én på den utvidede delen"
        throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
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
