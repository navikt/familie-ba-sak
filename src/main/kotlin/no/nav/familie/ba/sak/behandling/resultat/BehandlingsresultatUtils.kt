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

    fun utledBehandlingsresultatBasertPåYtelsePersonerV2(ytelsePersoner: List<YtelsePerson>): BehandlingResultat {
        val (framstiltNå, framstiltTidligere) = ytelsePersoner.partition { it.erFramstiltKravForINåværendeBehandling }

        val erEndring =
                framstiltTidligere.flatMap { it.resultater }.filter { it == YtelsePersonResultat.ENDRET || it == YtelsePersonResultat.REDUSERT }.isNotEmpty()

        val erOpphør =
        val erRentOpphør = framstiltTidligere.all { it.periodeStartForRentOpphør != null } &&
                         framstiltTidligere.groupBy { it.periodeStartForRentOpphør }.size == 1


        val kommerFraSøknad =  framstiltNå.isNotEmpty()

        if (kommerFraSøknad) {

            val erInnvilget = framstiltNå.flatMap { it.resultater }.all { it == YtelsePersonResultat.INNVILGET }
            val erDelvisInnvilget = framstiltNå.flatMap { it.resultater }.any { it == YtelsePersonResultat.INNVILGET }
            val erAvslått = framstiltNå.flatMap { it.resultater }.all { it == YtelsePersonResultat.AVSLÅTT }

            when {
                erInnvilget && !erEndring -> BehandlingResultat.INNVILGET
                erInnvilget && erEndring -> BehandlingResultat.INNVILGET_OG_ENDRET
                erInnvilget && erRentOpphør -> BehandlingResultat.INNVILGET_OG_OPPHØRT
                erInnvilget && erEndring && erRentOpphør -> BehandlingResultat.INNVILGET_ENDRET_OG_OPPHØRT
                erDelvisInnvilget && !erEndring ->
            }

        }
    }

    fun utledBehandlingsresultatBasertPåYtelsePersoner(ytelsePersoner: List<YtelsePerson>): BehandlingResultat {
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
                it.resultater != setOf(YtelsePersonResultat.FORTSATT_INNVILGET, YtelsePersonResultat.AVSLÅTT)
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
    }

    /**
     * Metode for å utlede kravene for å utlede behandlingsresultat per krav.
     * Metoden finner kravene som ble stilt i søknaden,
     * samt ytelsestypene per person fra forrige behandling.
     */
    fun utledKrav(søknadDTO: SøknadDTO?,
                  forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>): List<YtelsePerson> {
        val ytelsePersoner: MutableSet<YtelsePerson> =
                søknadDTO?.barnaMedOpplysninger?.filter { it.inkludertISøknaden }?.map {
                    YtelsePerson(personIdent = it.ident,
                                 ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                 erFramstiltKravForINåværendeBehandling = true)
                }?.toMutableSet() ?: mutableSetOf()

        forrigeAndelerTilkjentYtelse.forEach {
            val nyYtelsePerson = YtelsePerson(
                    personIdent = it.personIdent,
                    ytelseType = it.type,
                    erFramstiltKravForINåværendeBehandling = false
            )

            if (!ytelsePersoner.contains(nyYtelsePerson)) {
                ytelsePersoner.add(nyYtelsePerson)
            }
        }

        return ytelsePersoner.toList()
    }

    /**
     * Kun støttet for førstegangsbehandlinger som er fødselshendelse og ordinær barnetrygd
     */
    fun utledKravForFødselshendelseFGB(barnIdenterFraFødselshendelse: List<String>): List<YtelsePerson> =
            barnIdenterFraFødselshendelse.map {
                YtelsePerson(personIdent = it,
                             ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                             erFramstiltKravForINåværendeBehandling = true)
            }

    fun utledYtelsePersonerMedResultat(ytelsePersoner: List<YtelsePerson>,
                                       forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
                                       andelerTilkjentYtelse: List<AndelTilkjentYtelse>): List<YtelsePerson> {
        return ytelsePersoner.map { ytelsePerson: YtelsePerson ->
            val andeler = andelerTilkjentYtelse.filter { andel -> andel.personIdent == ytelsePerson.personIdent }
            val forrigeAndeler =
                    forrigeAndelerTilkjentYtelse.filter { andel -> andel.personIdent == ytelsePerson.personIdent }

            val forrigeAndelerTidslinje = LocalDateTimeline(forrigeAndeler.map {
                LocalDateSegment(
                        it.stønadFom.førsteDagIInneværendeMåned(),
                        it.stønadTom.sisteDagIInneværendeMåned(),
                        it
                )
            })
            val andelerTidslinje = LocalDateTimeline(andeler.map {
                LocalDateSegment(
                        it.stønadFom.førsteDagIInneværendeMåned(),
                        it.stønadTom.sisteDagIInneværendeMåned(),
                        it
                )
            })

            val segmenterLagtTil = andelerTidslinje.disjoint(forrigeAndelerTidslinje)
            val segmenterFjernet = forrigeAndelerTidslinje.disjoint(andelerTidslinje)

            val resultater = mutableSetOf<YtelsePersonResultat>()
            if (erAvslagPåSøknad(ytelsePerson = ytelsePerson, segmenterLagtTil = segmenterLagtTil)) {
                resultater.add(YtelsePersonResultat.AVSLÅTT)
            } else if (erYtelsenOpphørt(andeler = andeler)) {
                resultater.add(YtelsePersonResultat.REDUSERT)
            }

            if (erInnvilgetSøknad(ytelsePerson = ytelsePerson, segmenterLagtTil = segmenterLagtTil)) {
                resultater.add(YtelsePersonResultat.INNVILGET)
            } else if (erYtelsenFortsattInnvilget(andeler = andeler)) {
                resultater.add(YtelsePersonResultat.FORTSATT_INNVILGET)
            }

            if (erYtelsenEndretTilbakeITid(ytelsePerson = ytelsePerson,
                                           andeler = andeler,
                                           segmenterLagtTil = segmenterLagtTil,
                                           segmenterFjernet = segmenterFjernet)) {
                resultater.add(YtelsePersonResultat.ENDRET)
            }

            // Med "rent opphør" (ikke en fagterm) menes at tidspunkt for opphør er flyttet mot venstre i tidslinjen samtidig som
            // det ikke er gjort andre endringer (lagt til eller fjernet) som det må tas hensyn til i vedtaket.
            val periodeStartForRentOpphør: YearMonth? =
                    if (andeler.isEmpty()) {
                        // Håndtering av teknisk opphør.
                        TIDENES_MORGEN.toYearMonth()
                    } else if (resultater.contains(YtelsePersonResultat.REDUSERT) &&
                               segmenterLagtTil.isEmpty && segmenterFjernet.size() > 0) {

                        val innvilgetAndelTom = andeler.maxByOrNull { it.stønadTom }?.stønadTom
                                                ?: throw Feil("Er ytelsen opphørt skal det være satt tom-dato på alle andeler.")

                        if (segmenterFjernet.any { it.tom.toYearMonth() < innvilgetAndelTom }) {
                            null
                        } else {
                            innvilgetAndelTom.plusMonths(1)
                        }
                    } else if (resultater.contains(YtelsePersonResultat.REDUSERT)) {
                        andeler.maxByOrNull { it.stønadTom }?.stønadTom?.plusMonths(1)
                        ?: throw Feil("Er ytelsen opphørt skal det være satt tom-dato på alle andeler.")
                    } else null

            ytelsePerson.copy(
                    resultater = resultater.toSet(),
                    periodeStartForRentOpphør = periodeStartForRentOpphør
            )
        }
    }

    private fun erAvslagPåSøknad(ytelsePerson: YtelsePerson, segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>) =
            ytelsePerson.erFramstiltKravForINåværendeBehandling && segmenterLagtTil.isEmpty

    private fun erInnvilgetSøknad(ytelsePerson: YtelsePerson, segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>) =
            ytelsePerson.erFramstiltKravForINåværendeBehandling && !segmenterLagtTil.isEmpty

    private fun erYtelsenOpphørt(andeler: List<AndelTilkjentYtelse>) = andeler.none { it.erLøpende() }

    private fun erYtelsenFortsattInnvilget(andeler: List<AndelTilkjentYtelse>) = andeler.any { it.erLøpende() }

    private fun erYtelsenEndretTilbakeITid(ytelsePerson: YtelsePerson,
                                           andeler: List<AndelTilkjentYtelse>,
                                           segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>,
                                           segmenterFjernet: LocalDateTimeline<AndelTilkjentYtelse>) =
            andeler.isNotEmpty() && !ytelsePerson.erFramstiltKravForINåværendeBehandling &&
            (erEndringerTilbakeITid(segmenterLagtTil) || erEndringerTilbakeITid(segmenterFjernet))

    private fun erEndringerTilbakeITid(segmenterLagtTilEllerFjernet: LocalDateTimeline<AndelTilkjentYtelse>) =
            !segmenterLagtTilEllerFjernet.isEmpty && segmenterLagtTilEllerFjernet.any { !it.erLøpende() }
}