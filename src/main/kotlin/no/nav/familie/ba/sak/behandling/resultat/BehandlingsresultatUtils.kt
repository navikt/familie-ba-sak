package no.nav.familie.ba.sak.behandling.resultat

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.restDomene.SøknadDTO
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.beregning.domene.erLøpende
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import java.time.YearMonth

object BehandlingsresultatUtils {

    val ikkeStøttetFeil =
            Feil(frontendFeilmelding = "Behandlingsresultatet du har fått på behandlingen er ikke støttet i løsningen enda. Ta kontakt med Team familie om du er uenig i resultatet.",
                 message = "Behandlingsresultatet er ikke støttet i løsningen, se securelogger for resultatene som ble utledet.")

    fun utledBehandlingsresultatBasertPåYtelsePersoner(ytelsePersoner: List<YtelsePerson>): BehandlingResultat {
        if (ytelsePersoner.flatMap { it.resultater }.any { it == YtelsePersonResultat.IKKE_VURDERT })
            throw Feil(message = "Minst én ytelseperson er ikke vurdert")

        val (framstiltNå, framstiltTidligere) = ytelsePersoner.partition { it.erFramstiltKravForINåværendeBehandling }

        /**
         * Avklaring: Siden vi ikke differansierer mellom ENDRET_OG_FORTSATT_INNVILGET og OPPHØRT_OG_FORTSATT_INNVILGET
         * tenker jeg at dette bør være greit?
         */
        val erEndring =
                framstiltTidligere.flatMap { it.resultater }
                        .any { it == YtelsePersonResultat.ENDRET }

        // Kast feil om periodeStartForRentOpphør ikke er satt og opphør er satt
        val erRentOpphør = ytelsePersoner.all { it.periodeStartForRentOpphør != null } &&
                           ytelsePersoner.groupBy { it.periodeStartForRentOpphør }.size == 1

        val erNoeSomOpphører = ytelsePersoner.flatMap { it.resultater }.any { it == YtelsePersonResultat.OPPHØRT }

        val erNoeFraTidligereBehandlingerSomOpphører = framstiltTidligere.flatMap { it.resultater }.any { it == YtelsePersonResultat.OPPHØRT }
        val alleOpphørt =
                framstiltTidligere.all { it.resultater.contains(YtelsePersonResultat.OPPHØRT) } &&
                framstiltNå.all {
                    it.resultater.contains(YtelsePersonResultat.AVSLÅTT) || it.resultater.contains(YtelsePersonResultat.OPPHØRT)
                }


        val kommerFraSøknad = framstiltNå.isNotEmpty()

        return if (kommerFraSøknad) {

            val erInnvilget = framstiltNå.all { it.resultater.contains(YtelsePersonResultat.INNVILGET) }
            val erDelvisInnvilget = framstiltNå.flatMap { it.resultater }.any { it == YtelsePersonResultat.INNVILGET }
            val erAvslått = framstiltNå.flatMap { it.resultater }.all { it == YtelsePersonResultat.AVSLÅTT }

            when {
                erInnvilget && !erEndring && !erNoeFraTidligereBehandlingerSomOpphører && !alleOpphørt ->
                    BehandlingResultat.INNVILGET
                erInnvilget && !erEndring && erRentOpphør ->
                    BehandlingResultat.INNVILGET_OG_OPPHØRT
                erInnvilget && (erEndring || erNoeSomOpphører) && !alleOpphørt ->
                    BehandlingResultat.INNVILGET_OG_ENDRET
                erInnvilget && erEndring && alleOpphørt ->
                    BehandlingResultat.INNVILGET_ENDRET_OG_OPPHØRT
                // TODO delvis innvilget
                erAvslått && !erEndring && !erNoeFraTidligereBehandlingerSomOpphører ->
                    BehandlingResultat.AVSLÅTT
                erAvslått && !erEndring && erRentOpphør ->
                    BehandlingResultat.AVSLÅTT_OG_OPPHØRT
                erAvslått && (erEndring || erNoeSomOpphører) && !alleOpphørt ->
                    BehandlingResultat.AVSLÅTT_OG_ENDRET
                erAvslått && erEndring && alleOpphørt ->
                    BehandlingResultat.AVSLÅTT_ENDRET_OG_OPPHØRT
                else ->
                    throw ikkeStøttetFeil
            }
        } else {
            when {
                !(erEndring || erNoeSomOpphører) ->
                    BehandlingResultat.FORTSATT_INNVILGET
                !erEndring && erRentOpphør ->
                    BehandlingResultat.OPPHØRT
                (erEndring || erNoeSomOpphører) && !alleOpphørt ->
                    BehandlingResultat.ENDRET
                erEndring && alleOpphørt ->
                    BehandlingResultat.ENDRET_OG_OPPHØRT
                else ->
                    throw ikkeStøttetFeil
            }
        }
    }

    /*fun utledBehandlingsresultatBasertPåYtelsePersonerOld(ytelsePersoner: List<YtelsePerson>): BehandlingResultat {
        val (framstiltNå, framstiltTidligere) = ytelsePersoner.partition { it.erFramstiltKravForINåværendeBehandling }

        val innvilgetOgLøpendeYtelsePersoner = framstiltNå.filter { it.resultater == setOf(YtelsePersonResultat.INNVILGET) }

        val avslåttYtelsePersoner = framstiltNå.filter {
            it.resultater == setOf(YtelsePersonResultat.AVSLÅTT)
        }

        return if (framstiltNå.isNotEmpty() && framstiltTidligere.isEmpty()) { // TODO: Kun endringer fra søknad
            val innvilgetOgOpphørtYtelsePersoner = framstiltNå.filter {
                it.resultater == setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.REDUSERT)
            }

            val annet = framstiltNå.filter {
                it.resultater != setOf(YtelsePersonResultat.INNVILGET) &&
                it.resultater != setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.REDUSERT) &&
                it.resultater != setOf(YtelsePersonResultat.AVSLÅTT) &&
                it.resultater != setOf(YtelsePersonResultat.AVSLÅTT)
            }

            val erKunInnvilgetOgOpphørt = innvilgetOgOpphørtYtelsePersoner.isNotEmpty() &&
                                          innvilgetOgLøpendeYtelsePersoner.isEmpty() &&
                                          avslåttYtelsePersoner.isEmpty()

            val erInnvilget = (innvilgetOgLøpendeYtelsePersoner.isNotEmpty() || innvilgetOgOpphørtYtelsePersoner.isNotEmpty()) &&
                              avslåttYtelsePersoner.isEmpty()

            val erAvslått =
                    avslåttYtelsePersoner.isNotEmpty() && innvilgetOgLøpendeYtelsePersoner.isEmpty() && innvilgetOgOpphørtYtelsePersoner.isEmpty()

            if (annet.isNotEmpty()) throw ikkeStøttetFeil

            when {
                erKunInnvilgetOgOpphørt -> BehandlingResultat.INNVILGET_OG_OPPHØRT
                erInnvilget -> BehandlingResultat.INNVILGET
                erAvslått -> BehandlingResultat.AVSLÅTT
                else ->
                    throw ikkeStøttetFeil
            }
        } else {
            if (ytelsePersonerUtenFortsattInnvilget.any { it == YtelsePersonResultat.IKKE_VURDERT })
                throw Feil(message = "Minst én ytelseperson er ikke vurdert")

            /**
             * Avklaring: Siden vi ikke differansierer mellom ENDRET_OG_FORTSATT_INNVILGET og OPPHØRT_OG_FORTSATT_INNVILGET
             * tenker jeg at dette bør være greit?
             */
            val endringYtelsePersoner =
                    ytelsePersonerUtenFortsattInnvilget.filter { it == YtelsePersonResultat.ENDRET || it == YtelsePersonResultat.REDUSERT }


            val rentOpphør = framstiltTidligere.all { it.periodeStartForRentOpphør != null } &&
                             framstiltTidligere.groupBy { it.periodeStartForRentOpphør }.size == 1

            val erAvslått =
                    avslåttYtelsePersoner.isNotEmpty()

            val erKunEndringer = framstiltNå.isEmpty()

            return if (erKunEndringer) { // TODO: Revurdering uten søknad
                when {
                    ytelsePersonerUtenFortsattInnvilget.isEmpty() && ytelsePersonerMedFortsattInnvilget.isNotEmpty() ->
                        BehandlingResultat.FORTSATT_INNVILGET
                    endringYtelsePersoner.isNotEmpty() && ytelsePersonerMedFortsattInnvilget.isNotEmpty() ->
                        BehandlingResultat.ENDRET
                    endringYtelsePersoner.isNotEmpty() && ytelsePersonerMedFortsattInnvilget.isEmpty() && rentOpphør ->
                        BehandlingResultat.OPPHØRT
                    endringYtelsePersoner.isNotEmpty() && ytelsePersonerMedFortsattInnvilget.isEmpty() && !rentOpphør ->
                        BehandlingResultat.ENDRET_OG_OPPHØRT
                    else ->
                        throw ikkeStøttetFeil
                }
            } else { // TODO : Avslag
                val alleOpphørt = ytelsePersoner.all { it.resultater.contains(YtelsePersonResultat.REDUSERT) }

                when {
                    erAvslått && endringYtelsePersoner.isEmpty() ->
                        BehandlingResultat.AVSLÅTT
                    erAvslått && endringYtelsePersoner.isNotEmpty() && !alleOpphørt ->
                        BehandlingResultat.AVSLÅTT_OG_ENDRET
                    erAvslått && endringYtelsePersoner.isEmpty() && alleOpphørt ->
                        BehandlingResultat.AVSLÅTT_OG_OPPHØRT
                    erAvslått && endringYtelsePersoner.isNotEmpty() && alleOpphørt ->
                        BehandlingResultat.AVSLÅTT_ENDRET_OG_OPPHØRT
                    else ->
                        throw ikkeStøttetFeil
                }
            }
        }
    }*/


}