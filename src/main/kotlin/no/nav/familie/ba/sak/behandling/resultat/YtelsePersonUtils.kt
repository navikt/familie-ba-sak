package no.nav.familie.ba.sak.behandling.resultat

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

object YtelsePersonUtils {

    /**
     * Metode for å utlede kravene for å utlede behandlingsresultat per krav.
     * Metoden finner kravene som ble stilt i søknaden,
     * samt ytelsestypene per person fra forrige behandling.
     */
    fun utledKrav(søknadDTO: SøknadDTO?,
                  forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
                  personerMedEksplisitteAvslag: List<String> = emptyList()): List<YtelsePerson> {
        val personerFramstiltKravForNå: List<YtelsePerson> =
                søknadDTO?.barnaMedOpplysninger?.filter { it.inkludertISøknaden }?.map {
                    YtelsePerson(personIdent = it.ident,
                                 ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                 erFramstiltKravForINåværendeBehandling = true)
                } ?: emptyList()

        fun mapYtelsePersonFramstiltTidligere(andel: AndelTilkjentYtelse): YtelsePerson =
                if (personerMedEksplisitteAvslag.contains(andel.personIdent))
                    YtelsePerson(
                            personIdent = andel.personIdent,
                            ytelseType = andel.type,
                            erFramstiltKravForINåværendeBehandling = true,
                            resultater = setOf(YtelsePersonResultat.AVSLÅTT)
                    )
                else
                    YtelsePerson(
                            personIdent = andel.personIdent,
                            ytelseType = andel.type,
                            erFramstiltKravForINåværendeBehandling = false
                    )

        val personerFramstiltKravForTidligere: List<YtelsePerson> =
                forrigeAndelerTilkjentYtelse.map { mapYtelsePersonFramstiltTidligere(it) }.distinct()

        return listOf(personerFramstiltKravForNå,
                      personerFramstiltKravForTidligere.filter { !personerFramstiltKravForNå.contains(it) }).flatten()
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

            val resultater = ytelsePerson.resultater.toMutableSet()
            if (finnesAvslag(personSomSjekkes = ytelsePerson,
                             segmenterLagtTil = segmenterLagtTil)) {
                resultater.add(YtelsePersonResultat.AVSLÅTT)
            } else if (erYtelsenOpphørt(andeler = andeler)) {
                resultater.add(YtelsePersonResultat.OPPHØRT)
            }

            if (finnesInnvilget(personSomSjekkes = ytelsePerson, segmenterLagtTil = segmenterLagtTil)) {
                resultater.add(YtelsePersonResultat.INNVILGET)
            }

            if (finnesEndringTilbakeITid(personSomSjekkes = ytelsePerson,
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
                    } else if (resultater.contains(YtelsePersonResultat.OPPHØRT) &&
                               segmenterLagtTil.isEmpty && segmenterFjernet.size() > 0) {

                        val innvilgetAndelTom = andeler.maxByOrNull { it.stønadTom }?.stønadTom
                                                ?: throw Feil("Er ytelsen opphørt skal det være satt tom-dato på alle andeler.")

                        if (segmenterFjernet.any { it.tom.toYearMonth() < innvilgetAndelTom }) {
                            null
                        } else {
                            innvilgetAndelTom.plusMonths(1)
                        }
                    } else if (resultater.contains(YtelsePersonResultat.OPPHØRT)) {
                        andeler.maxByOrNull { it.stønadTom }?.stønadTom?.plusMonths(1)
                        ?: throw Feil("Er ytelsen opphørt skal det være satt tom-dato på alle andeler.")
                    } else null

            ytelsePerson.copy(
                    resultater = resultater.toSet(),
                    periodeStartForRentOpphør = periodeStartForRentOpphør
            )
        }
    }

    private fun finnesAvslag(personSomSjekkes: YtelsePerson, segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>) =
            personSomSjekkes.erFramstiltKravForINåværendeBehandling && segmenterLagtTil.isEmpty

    private fun finnesInnvilget(personSomSjekkes: YtelsePerson, segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>) =
            personSomSjekkes.erFramstiltKravForINåværendeBehandling && !segmenterLagtTil.isEmpty

    private fun erYtelsenOpphørt(andeler: List<AndelTilkjentYtelse>) = andeler.none { it.erLøpende() }

    private fun finnesEndringTilbakeITid(personSomSjekkes: YtelsePerson,
                                         segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>,
                                         segmenterFjernet: LocalDateTimeline<AndelTilkjentYtelse>): Boolean {
        fun finnesEndretSegmentTilbakeITid(segmenter: LocalDateTimeline<AndelTilkjentYtelse>) =
                !segmenter.isEmpty && segmenter.any { !it.erLøpende() }

        return !personSomSjekkes.erFramstiltKravForINåværendeBehandling &&
               (finnesEndretSegmentTilbakeITid(segmenterLagtTil) || finnesEndretSegmentTilbakeITid(segmenterFjernet))

    }
}