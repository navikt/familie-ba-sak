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
                  forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>): List<Krav> {
        val krav: MutableSet<Krav> =
                søknadDTO?.barnaMedOpplysninger?.filter { it.inkludertISøknaden }?.map {
                    Krav(personIdent = it.ident,
                         ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                         erSøknadskrav = true)
                }?.toMutableSet() ?: mutableSetOf()

        forrigeAndelerTilkjentYtelse.forEach {
            val nyttKrav = Krav(
                    personIdent = it.personIdent,
                    ytelseType = it.type,
                    erSøknadskrav = false
            )

            if (!krav.contains(nyttKrav)) {
                krav.add(nyttKrav)
            }
        }

        return krav.toList()
    }

    fun utledKravMedResultat(krav: List<Krav>,
                             forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
                             andelerTilkjentYtelse: List<AndelTilkjentYtelse>): List<Krav> {
        return krav.map { enkeltKrav: Krav ->
            val andeler = andelerTilkjentYtelse.filter { andel -> andel.personIdent == enkeltKrav.personIdent }
            val forrigeAndeler =
                    forrigeAndelerTilkjentYtelse.filter { andel -> andel.personIdent == enkeltKrav.personIdent }

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
            if (erAvslagPåSøknad(enkeltKrav = enkeltKrav, segmenterLagtTil = segmenterLagtTil)) {
                resultatTyper.add(BehandlingResultatType.AVSLÅTT)
            }

            if (erInnvilgetSøknad(enkeltKrav = enkeltKrav, segmenterLagtTil = segmenterLagtTil)) {
                resultatTyper.add(BehandlingResultatType.INNVILGET)
            }

            if (erYtelsenOpphørt(andeler = andeler, segmenterLagtTil = segmenterLagtTil, segmenterFjernet = segmenterFjernet)) {
                resultatTyper.add(BehandlingResultatType.OPPHØRT)
            }

            if (erYtelsenEndretTilbakeITid(enkeltKrav = enkeltKrav,
                                           segmenterLagtTil = segmenterLagtTil,
                                           segmenterFjernet = segmenterFjernet)) {
                resultatTyper.add(BehandlingResultatType.ENDRING)
            }

            if (erYtelsenFortsattInnvilget(forrigeAndeler = forrigeAndeler, andeler = andeler)) {
                resultatTyper.add(BehandlingResultatType.FORTSATT_INNVILGET)
            }

            enkeltKrav.copy(
                    resultatTyper = resultatTyper.toList()
            )
        }
    }

    private fun erAvslagPåSøknad(enkeltKrav: Krav,
                                 segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>) = enkeltKrav.erSøknadskrav && segmenterLagtTil.isEmpty

    private fun erInnvilgetSøknad(enkeltKrav: Krav,
                                  segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>) = enkeltKrav.erSøknadskrav && !segmenterLagtTil.isEmpty

    private fun erYtelsenOpphørt(andeler: List<AndelTilkjentYtelse>,
                                 segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>,
                                 segmenterFjernet: LocalDateTimeline<AndelTilkjentYtelse>) = (!segmenterLagtTil.isEmpty || !segmenterFjernet.isEmpty) && (andeler.isNotEmpty() && andeler.none { it.erLøpende() })

    private fun erYtelsenFortsattInnvilget(forrigeAndeler: List<AndelTilkjentYtelse>,
                                           andeler: List<AndelTilkjentYtelse>) = forrigeAndeler.isNotEmpty() && forrigeAndeler.any { it.erLøpende() } && andeler.any { it.erLøpende() }

    private fun erYtelsenEndretTilbakeITid(enkeltKrav: Krav,
                                           segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>,
                                           segmenterFjernet: LocalDateTimeline<AndelTilkjentYtelse>) = !enkeltKrav.erSøknadskrav && (erEndringerTilbakeITid(
            segmenterLagtTil) || erEndringerTilbakeITid(segmenterFjernet))

    private fun erEndringerTilbakeITid(andeler: LocalDateTimeline<AndelTilkjentYtelse>) = !andeler.isEmpty && andeler.any { !it.erLøpende() }
}