package no.nav.familie.ba.sak.behandling.resultat

import no.nav.familie.ba.sak.behandling.restDomene.SøknadDTO
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.beregning.domene.erLøpende
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline

object BehandlingsresultatUtils {

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
                                 erSøktOmINåværendeBehandling = true)
                }?.toMutableSet() ?: mutableSetOf()

        forrigeAndelerTilkjentYtelse.forEach {
            val nyYtelsePerson = YtelsePerson(
                    personIdent = it.personIdent,
                    ytelseType = it.type,
                    erSøktOmINåværendeBehandling = false
            )

            if (!ytelsePersoner.contains(nyYtelsePerson)) {
                ytelsePersoner.add(nyYtelsePerson)
            }
        }

        return ytelsePersoner.toList()
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

            val resultatTyper = mutableListOf<BehandlingResultatType>()
            if (erAvslagPåSøknad(ytelsePerson = ytelsePerson, segmenterLagtTil = segmenterLagtTil)) {
                resultatTyper.add(BehandlingResultatType.AVSLÅTT)
            }

            if (erInnvilgetSøknad(ytelsePerson = ytelsePerson, segmenterLagtTil = segmenterLagtTil)) {
                resultatTyper.add(BehandlingResultatType.INNVILGET)
            }

            if (erYtelsenOpphørt(andeler = andeler, segmenterLagtTil = segmenterLagtTil, segmenterFjernet = segmenterFjernet)) {
                resultatTyper.add(BehandlingResultatType.OPPHØRT)
            }

            if (erYtelsenEndretTilbakeITid(ytelsePerson = ytelsePerson,
                                           segmenterLagtTil = segmenterLagtTil,
                                           segmenterFjernet = segmenterFjernet)) {
                resultatTyper.add(BehandlingResultatType.ENDRING)
            }

            if (erYtelsenFortsattInnvilget(forrigeAndeler = forrigeAndeler, andeler = andeler)) {
                resultatTyper.add(BehandlingResultatType.FORTSATT_INNVILGET)
            }

            ytelsePerson.copy(
                    resultatTyper = resultatTyper.toList()
            )
        }
    }

    private fun erAvslagPåSøknad(ytelsePerson: YtelsePerson,
                                 segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>) = ytelsePerson.erSøktOmINåværendeBehandling && segmenterLagtTil.isEmpty

    private fun erInnvilgetSøknad(ytelsePerson: YtelsePerson,
                                  segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>) = ytelsePerson.erSøktOmINåværendeBehandling && !segmenterLagtTil.isEmpty

    private fun erYtelsenOpphørt(andeler: List<AndelTilkjentYtelse>,
                                 segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>,
                                 segmenterFjernet: LocalDateTimeline<AndelTilkjentYtelse>) = (!segmenterLagtTil.isEmpty || !segmenterFjernet.isEmpty) && (andeler.isNotEmpty() && andeler.none { it.erLøpende() })

    private fun erYtelsenFortsattInnvilget(forrigeAndeler: List<AndelTilkjentYtelse>,
                                           andeler: List<AndelTilkjentYtelse>) = forrigeAndeler.isNotEmpty() && forrigeAndeler.any { it.erLøpende() } && andeler.any { it.erLøpende() }

    private fun erYtelsenEndretTilbakeITid(ytelsePerson: YtelsePerson,
                                           segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>,
                                           segmenterFjernet: LocalDateTimeline<AndelTilkjentYtelse>) = !ytelsePerson.erSøktOmINåværendeBehandling && (erEndringerTilbakeITid(
            segmenterLagtTil) || erEndringerTilbakeITid(segmenterFjernet))

    private fun erEndringerTilbakeITid(segmenterLagtTilEllerFjernet: LocalDateTimeline<AndelTilkjentYtelse>) = !segmenterLagtTilEllerFjernet.isEmpty && segmenterLagtTilEllerFjernet.any { !it.erLøpende() }
}