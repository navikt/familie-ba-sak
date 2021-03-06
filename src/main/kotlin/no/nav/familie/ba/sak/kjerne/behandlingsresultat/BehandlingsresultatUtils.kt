package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrBefore

object BehandlingsresultatUtils {

    private val ikkeStøttetFeil =
            Feil(frontendFeilmelding = "Behandlingsresultatet du har fått på behandlingen er ikke støttet i løsningen enda. Ta kontakt med Team familie om du er uenig i resultatet.",
                 message = "Behandlingsresultatet er ikke støttet i løsningen, se securelogger for resultatene som ble utledet.")

    fun utledBehandlingsresultatBasertPåYtelsePersoner(ytelsePersoner: List<YtelsePerson>): BehandlingResultat {
        if (ytelsePersoner.flatMap { it.resultater }.any { it == YtelsePersonResultat.IKKE_VURDERT })
            throw Feil(message = "Minst én ytelseperson er ikke vurdert")

        if (ytelsePersoner.any { it.ytelseSlutt == null })
            throw Feil(message = "YtelseSlutt ikke satt ved utledning av BehandlingResultat")

        if (ytelsePersoner.any { it.resultater.contains(YtelsePersonResultat.OPPHØRT) && it.ytelseSlutt?.isAfter(inneværendeMåned()) == true })
            throw Feil(message = "Minst én ytelseperson har fått opphør som resultat og ytelseSlutt etter inneværende måned")

        val (framstiltNå, framstiltTidligere) = ytelsePersoner.partition { it.erFramstiltKravForINåværendeBehandling() }

        val ytelsePersonerUtenKunAvslag =
                ytelsePersoner.filter { !it.resultater.all { resultat -> resultat == YtelsePersonResultat.AVSLÅTT } }

        val erRentOpphør =
                ytelsePersonerUtenKunAvslag.all { it.resultater.contains(YtelsePersonResultat.OPPHØRT) } &&
                ytelsePersonerUtenKunAvslag.groupBy { it.ytelseSlutt }.size == 1

        val erOpphørPåFlereDatoer = ytelsePersonerUtenKunAvslag.filter { it.resultater.contains(YtelsePersonResultat.OPPHØRT) }
                                            .groupBy { it.ytelseSlutt }.size > 1

        val erNoeSomOpphører = ytelsePersoner.flatMap { it.resultater }.any { it == YtelsePersonResultat.OPPHØRT }

        val erNoeFraTidligereBehandlingerSomOpphører =
                framstiltTidligere.flatMap { it.resultater }.any { it == YtelsePersonResultat.OPPHØRT }

        val alleOpphørt = ytelsePersoner.all { it.ytelseSlutt!!.isSameOrBefore(inneværendeMåned()) }

        val erEndring = (framstiltTidligere + framstiltNå)
                .flatMap { it.resultater }
                .any { it == YtelsePersonResultat.ENDRET }

        val erEndringEllerOpphørPåPersoner = erEndring || erNoeSomOpphører
        val kommerFraSøknad = framstiltNå.isNotEmpty()

        return if (kommerFraSøknad) {
            val alleHarNoeInnvilget = framstiltNå.all { personSøktFor ->
                personSøktFor.resultater.contains(YtelsePersonResultat.INNVILGET) &&
                !personSøktFor.resultater.contains(YtelsePersonResultat.AVSLÅTT)
            }
            val resultaterPåSøknad = framstiltNå.flatMap { it.resultater }
            val erAvslått = resultaterPåSøknad.isNotEmpty() && resultaterPåSøknad.all { it == YtelsePersonResultat.AVSLÅTT }
            val erDelvisInnvilget =
                    (resultaterPåSøknad.any { it == YtelsePersonResultat.AVSLÅTT }) && resultaterPåSøknad.any { it == YtelsePersonResultat.INNVILGET }


            when {
                alleHarNoeInnvilget && !erEndring && !erNoeFraTidligereBehandlingerSomOpphører && !alleOpphørt ->
                    BehandlingResultat.INNVILGET
                alleHarNoeInnvilget && !erEndring && erRentOpphør ->
                    BehandlingResultat.INNVILGET_OG_OPPHØRT
                alleHarNoeInnvilget && erEndringEllerOpphørPåPersoner && (!alleOpphørt || erOpphørPåFlereDatoer) ->
                    BehandlingResultat.INNVILGET_OG_ENDRET
                alleHarNoeInnvilget && erEndring && alleOpphørt ->
                    BehandlingResultat.INNVILGET_ENDRET_OG_OPPHØRT
                erDelvisInnvilget && !erEndring && !erNoeFraTidligereBehandlingerSomOpphører && !alleOpphørt ->
                    BehandlingResultat.DELVIS_INNVILGET
                erDelvisInnvilget && !erEndring && erRentOpphør ->
                    BehandlingResultat.DELVIS_INNVILGET_OG_OPPHØRT
                erDelvisInnvilget && erEndringEllerOpphørPåPersoner && !alleOpphørt ->
                    BehandlingResultat.DELVIS_INNVILGET_OG_ENDRET
                erDelvisInnvilget && erEndringEllerOpphørPåPersoner && alleOpphørt ->
                    BehandlingResultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT
                erAvslått && !erEndring && !erNoeFraTidligereBehandlingerSomOpphører ->
                    BehandlingResultat.AVSLÅTT
                erAvslått && !erEndring && erRentOpphør && alleOpphørt ->
                    BehandlingResultat.AVSLÅTT_OG_OPPHØRT
                erAvslått && erEndringEllerOpphørPåPersoner && !alleOpphørt ->
                    BehandlingResultat.AVSLÅTT_OG_ENDRET
                erAvslått && erEndring && alleOpphørt ->
                    BehandlingResultat.AVSLÅTT_ENDRET_OG_OPPHØRT
                !erEndringEllerOpphørPåPersoner && !erAvslått ->
                    BehandlingResultat.FORTSATT_INNVILGET
                else ->
                    throw ikkeStøttetFeil
            }
        } else {
            when {
                !erEndringEllerOpphørPåPersoner ->
                    BehandlingResultat.FORTSATT_INNVILGET
                !erEndring && erRentOpphør && alleOpphørt ->
                    BehandlingResultat.OPPHØRT
                erEndringEllerOpphørPåPersoner && !alleOpphørt ->
                    BehandlingResultat.ENDRET
                (erEndring || erOpphørPåFlereDatoer) && alleOpphørt ->
                    BehandlingResultat.ENDRET_OG_OPPHØRT
                else ->
                    throw ikkeStøttetFeil
            }
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
    }
}