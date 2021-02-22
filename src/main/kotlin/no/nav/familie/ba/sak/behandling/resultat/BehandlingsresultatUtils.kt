package no.nav.familie.ba.sak.behandling.resultat

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.restDomene.SøknadDTO
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.beregning.domene.erLøpende
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
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

        if (ytelsePersoner.any { it.periodeStartForRentOpphør?.isAfter(inneværendeMåned().plusMonths(1)) == true })
            throw Feil(message = "Minst én ytelseperson har fått opphør som resultat og periodeStartForRentOpphør etter neste måned")

        val (framstiltNå, framstiltTidligere) = ytelsePersoner.partition { it.erFramstiltKravForINåværendeBehandling }

        val ytelsePersonerUtenAvslag = ytelsePersoner.filter { it.resultater.none { it == YtelsePersonResultat.AVSLÅTT } }
        val erRentOpphør = ytelsePersonerUtenAvslag.all { it.resultater.contains(YtelsePersonResultat.OPPHØRT) && it.periodeStartForRentOpphør != null } &&
                           ytelsePersonerUtenAvslag.groupBy { it.periodeStartForRentOpphør }.size == 1

        val erNoeSomOpphører = ytelsePersoner.flatMap { it.resultater }.any { it == YtelsePersonResultat.OPPHØRT }

        val erNoeFraTidligereBehandlingerSomOpphører = framstiltTidligere.flatMap { it.resultater }.any { it == YtelsePersonResultat.OPPHØRT }
        val alleOpphørt =
                framstiltTidligere.all { it.resultater.contains(YtelsePersonResultat.OPPHØRT) } &&
                framstiltNå.all {
                    it.resultater.contains(YtelsePersonResultat.AVSLÅTT) || it.resultater.contains(YtelsePersonResultat.OPPHØRT)
                }


        val erEndring =
                framstiltTidligere.flatMap { it.resultater }
                        .any { it == YtelsePersonResultat.ENDRET }
        val erEndringEllerOpphørPåPersoner = erEndring || erNoeSomOpphører
        val kommerFraSøknad = framstiltNå.isNotEmpty()

        return if (kommerFraSøknad) {

            val erInnvilget = framstiltNå.all { it.resultater.contains(YtelsePersonResultat.INNVILGET) }
            val erAvslått = framstiltNå.flatMap { it.resultater }.all { it == YtelsePersonResultat.AVSLÅTT }

            when {
                erInnvilget && !erEndring && !erNoeFraTidligereBehandlingerSomOpphører && !alleOpphørt ->
                    BehandlingResultat.INNVILGET
                erInnvilget && !erEndring && erRentOpphør ->
                    BehandlingResultat.INNVILGET_OG_OPPHØRT
                erInnvilget && erEndringEllerOpphørPåPersoner && !alleOpphørt ->
                    BehandlingResultat.INNVILGET_OG_ENDRET
                erInnvilget && erEndring && alleOpphørt ->
                    BehandlingResultat.INNVILGET_ENDRET_OG_OPPHØRT
                // TODO delvis innvilget
                erAvslått && !erEndring && !erNoeFraTidligereBehandlingerSomOpphører ->
                    BehandlingResultat.AVSLÅTT
                erAvslått && !erEndring && erRentOpphør ->
                    BehandlingResultat.AVSLÅTT_OG_OPPHØRT
                erAvslått && erEndringEllerOpphørPåPersoner && !alleOpphørt ->
                    BehandlingResultat.AVSLÅTT_OG_ENDRET
                erAvslått && erEndring && alleOpphørt ->
                    BehandlingResultat.AVSLÅTT_ENDRET_OG_OPPHØRT
                else ->
                    throw ikkeStøttetFeil
            }
        } else {
            when {
                !erEndringEllerOpphørPåPersoner ->
                    BehandlingResultat.FORTSATT_INNVILGET
                !erEndring && erRentOpphør ->
                    BehandlingResultat.OPPHØRT
                erEndringEllerOpphørPåPersoner && !alleOpphørt ->
                    BehandlingResultat.ENDRET
                erEndring && alleOpphørt ->
                    BehandlingResultat.ENDRET_OG_OPPHØRT
                else ->
                    throw ikkeStøttetFeil
            }
        }
    }
}