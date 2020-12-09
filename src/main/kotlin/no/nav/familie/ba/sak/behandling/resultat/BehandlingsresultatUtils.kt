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

    fun utledBehandlingsresultatBasertPåYtelsePersoner(ytelsePersoner: List<YtelsePerson>): BehandlingResultat {
        val ytelsePersonerUtenFortsattInnvilget =
                ytelsePersoner.flatMap { it.resultater }.filter { it != YtelsePersonResultat.FORTSATT_INNVILGET }

        return when {
            ytelsePersonerUtenFortsattInnvilget.any { it == YtelsePersonResultat.IKKE_VURDERT } ->
                throw Feil(message = "Minst én ytelseperson er ikke vurdert")
            ytelsePersonerUtenFortsattInnvilget.isEmpty() ->
                BehandlingResultat.FORTSATT_INNVILGET
            ytelsePersonerUtenFortsattInnvilget.all { it == YtelsePersonResultat.INNVILGET } ->
                BehandlingResultat.INNVILGET
            ytelsePersonerUtenFortsattInnvilget.all { it == YtelsePersonResultat.AVSLÅTT } ->
                BehandlingResultat.AVSLÅTT
            ytelsePersonerUtenFortsattInnvilget.all { it == YtelsePersonResultat.OPPHØRT } ->
                BehandlingResultat.OPPHØRT
            ytelsePersonerUtenFortsattInnvilget.all { it == YtelsePersonResultat.ENDRING } ->
                BehandlingResultat.ENDRING_OG_LØPENDE
            ytelsePersonerUtenFortsattInnvilget.any { it == YtelsePersonResultat.OPPHØRT } ->
                BehandlingResultat.ENDRING_OG_OPPHØRT
            else ->
                throw Feil(message = "Klarer ikke å utlede behandlingsresultat. Resultatet er sansynligvis ikke støttet, se securelogger for resultatene som ble utledet.")
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
                                 segmenterFjernet: LocalDateTimeline<AndelTilkjentYtelse>) = (!segmenterLagtTil.isEmpty || !segmenterFjernet.isEmpty) && (andeler.isNotEmpty() && andeler.none { it.erLøpende() })

    private fun erYtelsenFortsattInnvilget(forrigeAndeler: List<AndelTilkjentYtelse>,
                                           andeler: List<AndelTilkjentYtelse>) = forrigeAndeler.isNotEmpty() && forrigeAndeler.any { it.erLøpende() } && andeler.any { it.erLøpende() }

    private fun erYtelsenEndretTilbakeITid(ytelsePerson: YtelsePerson,
                                           segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>,
                                           segmenterFjernet: LocalDateTimeline<AndelTilkjentYtelse>) = !ytelsePerson.erFramstiltKravForINåværendeBehandling && (erEndringerTilbakeITid(
            segmenterLagtTil) || erEndringerTilbakeITid(segmenterFjernet))

    private fun erEndringerTilbakeITid(segmenterLagtTilEllerFjernet: LocalDateTimeline<AndelTilkjentYtelse>) = !segmenterLagtTilEllerFjernet.isEmpty && segmenterLagtTilEllerFjernet.any { !it.erLøpende() }
}