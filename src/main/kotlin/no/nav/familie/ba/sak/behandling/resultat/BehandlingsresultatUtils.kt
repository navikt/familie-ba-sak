package no.nav.familie.ba.sak.behandling.resultat

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.restDomene.SøknadDTO
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.beregning.domene.erLøpende
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline

object BehandlingsresultatUtils {

    val ikkeStøttetFeil =
            Feil(frontendFeilmelding = "Behandlingsresultatet du har fått på behandlingen er ikke støttet i løsningen enda. Ta kontakt med Team familie om du er uenig i resultatet.",
                 message = "Behandlingsresultatet er ikke støttet i løsningen, se securelogger for resultatene som ble utledet.")

    fun utledBehandlingsresultatBasertPåYtelsePersoner(ytelsePersoner: List<YtelsePerson>): BehandlingResultat {
        val (søknad, andre) = ytelsePersoner.partition { it.erFramstiltKravForINåværendeBehandling }
        val (ytelsePersonerUtenFortsattInnvilget, ytelsePersonerMedFortsattInnvilget) =
                andre.flatMap { it.resultater }.partition { it != YtelsePersonResultat.FORTSATT_INNVILGET }

        return if (søknad.isNotEmpty() && ytelsePersonerUtenFortsattInnvilget.isEmpty()) {
            val innvilgetOgLøpende = søknad.filter {
                it.resultater.sorted() == listOf(YtelsePersonResultat.INNVILGET).sorted()
            }

            val innvilgetOgOpphørt = søknad.filter {
                it.resultater.sorted() == listOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.OPPHØRT).sorted()
            }

            val avslått = søknad.filter {
                it.resultater.sorted() == listOf(YtelsePersonResultat.AVSLÅTT).sorted()
            }

            val annet = søknad.filter {
                it.resultater.sorted() != listOf(YtelsePersonResultat.INNVILGET).sorted() &&
                it.resultater.sorted() != listOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.OPPHØRT).sorted() &&
                it.resultater.sorted() != listOf(YtelsePersonResultat.AVSLÅTT).sorted()
            }

            if (annet.isNotEmpty()) throw ikkeStøttetFeil

            when {
                innvilgetOgOpphørt.isNotEmpty() && innvilgetOgLøpende.isEmpty() && avslått.isEmpty() ->
                    BehandlingResultat.INNVILGET_OG_OPPHØR
                (innvilgetOgLøpende.isNotEmpty() || innvilgetOgOpphørt.isNotEmpty()) && avslått.isEmpty() ->
                    BehandlingResultat.INNVILGET
                avslått.isNotEmpty() && innvilgetOgLøpende.isEmpty() && innvilgetOgOpphørt.isEmpty() ->
                    BehandlingResultat.AVSLÅTT
                else ->
                    throw ikkeStøttetFeil
            }
        } else {
            val opphørteYtelsePersoner = ytelsePersonerUtenFortsattInnvilget.filter { it == YtelsePersonResultat.OPPHØRT }
            val endringYtelsePersoner = ytelsePersonerUtenFortsattInnvilget.filter { it == YtelsePersonResultat.ENDRING }

            return when {
                ytelsePersonerUtenFortsattInnvilget.any { it == YtelsePersonResultat.IKKE_VURDERT } ->
                    throw Feil(message = "Minst én ytelseperson er ikke vurdert")
                ytelsePersonerUtenFortsattInnvilget.isEmpty() && ytelsePersonerMedFortsattInnvilget.isNotEmpty() ->
                    BehandlingResultat.FORTSATT_INNVILGET
                ytelsePersonerUtenFortsattInnvilget.all { it == YtelsePersonResultat.OPPHØRT } ->
                    BehandlingResultat.OPPHØRT
                ytelsePersonerUtenFortsattInnvilget.all { it == YtelsePersonResultat.ENDRING } && ytelsePersonerMedFortsattInnvilget.isNotEmpty() ->
                    BehandlingResultat.ENDRING_OG_LØPENDE
                endringYtelsePersoner.isNotEmpty() && opphørteYtelsePersoner.isNotEmpty() ->
                    BehandlingResultat.ENDRING_OG_OPPHØRT
                else ->
                    throw ikkeStøttetFeil
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
    fun utledKravForAutomatiskFGB(barnIdenterFraFødselshendelse: List<String>): List<YtelsePerson> = barnIdenterFraFødselshendelse.map {
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

            val resultater = mutableListOf<YtelsePersonResultat>()
            if (erAvslagPåSøknad(ytelsePerson = ytelsePerson, segmenterLagtTil = segmenterLagtTil)) {
                resultater.add(YtelsePersonResultat.AVSLÅTT)
            }

            if (erInnvilgetSøknad(ytelsePerson = ytelsePerson, segmenterLagtTil = segmenterLagtTil)) {
                resultater.add(YtelsePersonResultat.INNVILGET)
            }

            if (erYtelsenOpphørt(andeler = andeler, segmenterLagtTil = segmenterLagtTil, segmenterFjernet = segmenterFjernet)) {
                resultater.add(YtelsePersonResultat.OPPHØRT)
            }

            if (erYtelsenEndretTilbakeITid(ytelsePerson = ytelsePerson,
                                           andeler = andeler,
                                           segmenterLagtTil = segmenterLagtTil,
                                           segmenterFjernet = segmenterFjernet)) {
                resultater.add(YtelsePersonResultat.ENDRING)
            }

            if (erYtelsenFortsattInnvilget(forrigeAndeler = forrigeAndeler, andeler = andeler)) {
                resultater.add(YtelsePersonResultat.FORTSATT_INNVILGET)
            }

            ytelsePerson.copy(
                    resultater = resultater.toList()
            )
        }
    }

    private fun erAvslagPåSøknad(ytelsePerson: YtelsePerson,
                                 segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>) = ytelsePerson.erFramstiltKravForINåværendeBehandling && segmenterLagtTil.isEmpty

    private fun erInnvilgetSøknad(ytelsePerson: YtelsePerson,
                                  segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>) = ytelsePerson.erFramstiltKravForINåværendeBehandling && !segmenterLagtTil.isEmpty

    private fun erYtelsenOpphørt(andeler: List<AndelTilkjentYtelse>,
                                 segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>,
                                 segmenterFjernet: LocalDateTimeline<AndelTilkjentYtelse>) = (!segmenterLagtTil.isEmpty || !segmenterFjernet.isEmpty) && andeler.none { it.erLøpende() }

    private fun erYtelsenFortsattInnvilget(forrigeAndeler: List<AndelTilkjentYtelse>,
                                           andeler: List<AndelTilkjentYtelse>) = forrigeAndeler.isNotEmpty() && forrigeAndeler.any { it.erLøpende() } && andeler.any { it.erLøpende() }

    private fun erYtelsenEndretTilbakeITid(ytelsePerson: YtelsePerson,
                                           andeler: List<AndelTilkjentYtelse>,
                                           segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>,
                                           segmenterFjernet: LocalDateTimeline<AndelTilkjentYtelse>) = andeler.isNotEmpty() && !ytelsePerson.erFramstiltKravForINåværendeBehandling && (erEndringerTilbakeITid(
            segmenterLagtTil) || erEndringerTilbakeITid(segmenterFjernet))

    private fun erEndringerTilbakeITid(segmenterLagtTilEllerFjernet: LocalDateTimeline<AndelTilkjentYtelse>) = !segmenterLagtTilEllerFjernet.isEmpty && segmenterLagtTilEllerFjernet.any { !it.erLøpende() }
}