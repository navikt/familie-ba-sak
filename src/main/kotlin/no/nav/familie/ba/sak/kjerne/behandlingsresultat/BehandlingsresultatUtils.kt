package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

object BehandlingsresultatUtils {

    private val ikkeStøttetFeil =
            Feil(frontendFeilmelding = "Behandlingsresultatet du har fått på behandlingen er ikke støttet i løsningen enda. Ta kontakt med Team familie om du er uenig i resultatet.",
                 message = "Behandlingsresultatet er ikke støttet i løsningen, se securelogger for resultatene som ble utledet.")

    fun utledBehandlingsresultatDataForPerson(person: Person,
                                              personerFremstiltKravFor: List<String>,
                                              forrigeTilkjentYtelse: TilkjentYtelse?,
                                              tilkjentYtelse: TilkjentYtelse): BehandlingsresultatPerson {

        val personIdent = person.personIdent.ident
        return BehandlingsresultatPerson(
                personIdent = personIdent,
                personType = person.type,
                søktForPerson = personerFremstiltKravFor.contains(personIdent),
                forrigeAndeler = forrigeTilkjentYtelse?.andelerTilkjentYtelse?.filter { it.personIdent == personIdent }
                                         ?.map { andelTilkjentYtelse ->
                                             BehandlingsresultatAndelTilkjentYtelse(
                                                     stønadFom = andelTilkjentYtelse.stønadFom,
                                                     stønadTom = andelTilkjentYtelse.stønadTom,
                                                     kalkulertUtbetalingsbeløp = andelTilkjentYtelse.kalkulertUtbetalingsbeløp
                                             )
                                         } ?: emptyList(),
                andeler = tilkjentYtelse.andelerTilkjentYtelse.filter { it.personIdent == personIdent }
                        .map { andelTilkjentYtelse ->
                            BehandlingsresultatAndelTilkjentYtelse(
                                    stønadFom = andelTilkjentYtelse.stønadFom,
                                    stønadTom = andelTilkjentYtelse.stønadTom,
                                    kalkulertUtbetalingsbeløp = andelTilkjentYtelse.kalkulertUtbetalingsbeløp
                            )
                        }
        )
    }


    fun utledBehandlingsresultatBasertPåYtelsePersoner(ytelsePersoner: List<YtelsePerson>): BehandlingResultat {
        validerYtelsePersoner(ytelsePersoner)

        val samledeResultater = ytelsePersoner.flatMap { it.resultater }.toMutableSet()
        val erKunFremstilKravIDenneBehandling =
                ytelsePersoner.all { it.kravOpprinnelse == listOf(KravOpprinnelse.INNEVÆRENDE) }

        val altOpphører = ytelsePersoner.all { it.ytelseSlutt!!.isSameOrBefore(inneværendeMåned()) }
        val erAvslått = ytelsePersoner.all { it.resultater == setOf(YtelsePersonResultat.AVSLÅTT) }
        val opphørPåSammeTid = altOpphører &&
                               (ytelsePersoner.filter { it.resultater != setOf(YtelsePersonResultat.AVSLÅTT) }
                                        .groupBy { it.ytelseSlutt }.size == 1 || erAvslått)
        val noeOpphørerPåTidligereBarn = ytelsePersoner.any {
            it.resultater.contains(YtelsePersonResultat.OPPHØRT) && !it.kravOpprinnelse.contains(KravOpprinnelse.INNEVÆRENDE)
        }

        if (noeOpphørerPåTidligereBarn && !altOpphører) {
            samledeResultater.add(YtelsePersonResultat.ENDRET)
        }

        val opphørSomFørerTilEndring = altOpphører && !opphørPåSammeTid && !erKunFremstilKravIDenneBehandling
        if (opphørSomFørerTilEndring) {
            samledeResultater.add(YtelsePersonResultat.ENDRET)
        }

        if (!altOpphører) {
            samledeResultater.remove(YtelsePersonResultat.OPPHØRT)
        }

        return when {
            samledeResultater.isEmpty() -> BehandlingResultat.FORTSATT_INNVILGET
            samledeResultater == setOf(YtelsePersonResultat.ENDRET) -> BehandlingResultat.ENDRET
            samledeResultater == setOf(YtelsePersonResultat.ENDRET,
                                       YtelsePersonResultat.OPPHØRT) -> BehandlingResultat.ENDRET_OG_OPPHØRT
            samledeResultater == setOf(YtelsePersonResultat.OPPHØRT) -> BehandlingResultat.OPPHØRT
            samledeResultater == setOf(YtelsePersonResultat.INNVILGET) -> BehandlingResultat.INNVILGET
            samledeResultater == setOf(YtelsePersonResultat.INNVILGET,
                                       YtelsePersonResultat.OPPHØRT) -> BehandlingResultat.INNVILGET_OG_OPPHØRT
            samledeResultater == setOf(YtelsePersonResultat.INNVILGET,
                                       YtelsePersonResultat.ENDRET) -> BehandlingResultat.INNVILGET_OG_ENDRET
            samledeResultater == setOf(YtelsePersonResultat.INNVILGET,
                                       YtelsePersonResultat.ENDRET,
                                       YtelsePersonResultat.OPPHØRT) -> BehandlingResultat.INNVILGET_ENDRET_OG_OPPHØRT
            samledeResultater == setOf(YtelsePersonResultat.INNVILGET,
                                       YtelsePersonResultat.AVSLÅTT) -> BehandlingResultat.DELVIS_INNVILGET
            samledeResultater == setOf(YtelsePersonResultat.INNVILGET,
                                       YtelsePersonResultat.AVSLÅTT,
                                       YtelsePersonResultat.OPPHØRT) -> BehandlingResultat.DELVIS_INNVILGET_OG_OPPHØRT
            samledeResultater == setOf(YtelsePersonResultat.INNVILGET,
                                       YtelsePersonResultat.AVSLÅTT,
                                       YtelsePersonResultat.ENDRET) -> BehandlingResultat.DELVIS_INNVILGET_OG_ENDRET
            samledeResultater == setOf(YtelsePersonResultat.INNVILGET,
                                       YtelsePersonResultat.AVSLÅTT,
                                       YtelsePersonResultat.ENDRET,
                                       YtelsePersonResultat.OPPHØRT) -> BehandlingResultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT
            samledeResultater == setOf(YtelsePersonResultat.AVSLÅTT) -> BehandlingResultat.AVSLÅTT
            samledeResultater == setOf(YtelsePersonResultat.AVSLÅTT,
                                       YtelsePersonResultat.OPPHØRT) -> BehandlingResultat.AVSLÅTT_OG_OPPHØRT
            samledeResultater == setOf(YtelsePersonResultat.AVSLÅTT,
                                       YtelsePersonResultat.ENDRET) -> BehandlingResultat.AVSLÅTT_OG_ENDRET
            samledeResultater == setOf(YtelsePersonResultat.AVSLÅTT,
                                       YtelsePersonResultat.ENDRET,
                                       YtelsePersonResultat.OPPHØRT) -> BehandlingResultat.AVSLÅTT_ENDRET_OG_OPPHØRT
            else -> throw ikkeStøttetFeil
        }
    }

    fun validerBehandlingsresultat(behandling: Behandling, resultat: BehandlingResultat) {
        if ((behandling.type == BehandlingType.FØRSTEGANGSBEHANDLING && setOf(
                        BehandlingResultat.AVSLÅTT_OG_OPPHØRT,
                        BehandlingResultat.ENDRET,
                        BehandlingResultat.ENDRET_OG_OPPHØRT,
                        BehandlingResultat.OPPHØRT,
                        BehandlingResultat.FORTSATT_INNVILGET,
                        BehandlingResultat.IKKE_VURDERT).contains(resultat)) ||
            (behandling.type == BehandlingType.REVURDERING && resultat == BehandlingResultat.IKKE_VURDERT)) {

            val feilmelding = "Behandlingsresultatet ${resultat.displayName.lowercase()} " +
                              "er ugyldig i kombinasjon med behandlingstype '${behandling.type.visningsnavn}'."
            throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
        }
        if (behandling.opprettetÅrsak == BehandlingÅrsak.KLAGE && setOf(
                        BehandlingResultat.AVSLÅTT_OG_OPPHØRT,
                        BehandlingResultat.AVSLÅTT_ENDRET_OG_OPPHØRT,
                        BehandlingResultat.AVSLÅTT_OG_ENDRET,
                        BehandlingResultat.AVSLÅTT).contains(resultat)) {
            val feilmelding = "Behandlingsårsak ${behandling.opprettetÅrsak.visningsnavn.lowercase()} " +
                              "er ugyldig i kombinasjon med resultat '${resultat.displayName.lowercase()}'."
            throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
        }
    }
}

private fun validerYtelsePersoner(ytelsePersoner: List<YtelsePerson>) {
    if (ytelsePersoner.flatMap { it.resultater }.any { it == YtelsePersonResultat.IKKE_VURDERT })
        throw Feil(message = "Minst én ytelseperson er ikke vurdert")

    if (ytelsePersoner.any { it.ytelseSlutt == null })
        throw Feil(message = "YtelseSlutt ikke satt ved utledning av BehandlingResultat")

    if (ytelsePersoner.any { it.resultater.contains(YtelsePersonResultat.OPPHØRT) && it.ytelseSlutt?.isAfter(inneværendeMåned()) == true })
        throw Feil(message = "Minst én ytelseperson har fått opphør som resultat og ytelseSlutt etter inneværende måned")
}