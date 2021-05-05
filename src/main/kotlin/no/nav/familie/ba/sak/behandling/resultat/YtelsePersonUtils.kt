package no.nav.familie.ba.sak.behandling.resultat

import no.nav.familie.ba.sak.behandling.restDomene.SøknadDTO
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.beregning.domene.erLøpende
import no.nav.familie.ba.sak.common.*
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

        val (tidligereAndelerMedEksplisittAvslag, tidligereAndeler)
                = forrigeAndelerTilkjentYtelse
                .distinctBy { Pair(it.personIdent, it.type) }
                .partition { personerMedEksplisitteAvslag.contains(it.personIdent) }

        val framstiltKravForNåViaSøknad =
                søknadDTO?.barnaMedOpplysninger
                        ?.filter { it.inkludertISøknaden }
                        ?.map { barn ->
                            YtelsePerson(
                                    personIdent = barn.ident,
                                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                    kravOpprinnelse =
                                    if (tidligereAndeler.any {
                                                it.personIdent == barn.ident &&
                                                it.type == YtelseType.ORDINÆR_BARNETRYGD
                                            }) KravOpprinnelse.SØKNAD_OG_TIDLIGERE
                                    else KravOpprinnelse.SØKNAD,
                            )
                        } ?: emptyList()


        val framstiltKravForNåEksplisitt =
                tidligereAndelerMedEksplisittAvslag.map {
                    YtelsePerson(
                            personIdent = it.personIdent,
                            ytelseType = it.type,
                            kravOpprinnelse = KravOpprinnelse.SØKNAD_OG_TIDLIGERE,
                    )
                }

        val framstiltKravForNå: List<YtelsePerson> = framstiltKravForNåViaSøknad + framstiltKravForNåEksplisitt

        val kunFramstiltKravForTidligere: List<YtelsePerson> =
                tidligereAndeler
                        .filter { andel ->
                            framstiltKravForNå.none {
                                andel.personIdent == it.personIdent &&
                                andel.type == it.ytelseType
                            }
                        }
                        .map {
                            YtelsePerson(
                                    personIdent = it.personIdent,
                                    ytelseType = it.type,
                                    kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                            )
                        }

        return (framstiltKravForNå + kunFramstiltKravForTidligere).distinct()
    }

    /**
     * Kun støttet for førstegangsbehandlinger som er fødselshendelse og ordinær barnetrygd
     */
    fun utledKravForFødselshendelseFGB(barnIdenterFraFødselshendelse: List<String>): List<YtelsePerson> =
            barnIdenterFraFødselshendelse.map {
                YtelsePerson(
                        personIdent = it,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = KravOpprinnelse.SØKNAD,
                )
            }

    fun populerYtelsePersonerMedResultat(ytelsePersoner: List<YtelsePerson>,
                                         forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
                                         andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
                                         personerMedEksplisitteAvslag: List<String> = emptyList()): List<YtelsePerson> {
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
            if (personerMedEksplisitteAvslag.contains(ytelsePerson.personIdent)
                || finnesAvslag(personSomSjekkes = ytelsePerson,
                                segmenterLagtTil = segmenterLagtTil)) {
                resultater.add(YtelsePersonResultat.AVSLÅTT)
            }
            if (erYtelsenOpphørt(andeler = andeler) && (segmenterFjernet + segmenterLagtTil).isNotEmpty()) {
                resultater.add(YtelsePersonResultat.OPPHØRT)
            }

            if (finnesInnvilget(personSomSjekkes = ytelsePerson, segmenterLagtTil = segmenterLagtTil)) {
                resultater.add(YtelsePersonResultat.INNVILGET)
            }

            val ytelseSlutt: YearMonth? = if (andeler.isNotEmpty())
                andeler.maxByOrNull { it.stønadTom }?.stønadTom
                ?: throw Feil("Finnes andel uten tom-dato") else TIDENES_MORGEN.toYearMonth()

            if (finnesEndringTilbakeITid(personSomSjekkes = ytelsePerson,
                                         segmenterLagtTil = segmenterLagtTil,
                                         segmenterFjernet = segmenterFjernet)
                && !enesteEndringErTidligereStønadslutt(segmenterLagtTil = segmenterLagtTil,
                                                        segmenterFjernet = segmenterFjernet,
                                                        sisteAndelPåPerson = andeler.maxByOrNull { it.stønadFom })) {
                resultater.add(YtelsePersonResultat.ENDRET)
            }

            ytelsePerson.copy(
                    resultater = resultater.toSet(),
                    ytelseSlutt = ytelseSlutt
            )
        }
    }

    private fun finnesAvslag(personSomSjekkes: YtelsePerson, segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>) =
            personSomSjekkes.erFramstiltKravForINåværendeBehandling() && segmenterLagtTil.isEmpty

    private fun finnesInnvilget(personSomSjekkes: YtelsePerson, segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>) =
            personSomSjekkes.erFramstiltKravForINåværendeBehandling() && !segmenterLagtTil.isEmpty

    private fun erYtelsenOpphørt(andeler: List<AndelTilkjentYtelse>) = andeler.none { it.erLøpende() }

    private fun finnesEndringTilbakeITid(personSomSjekkes: YtelsePerson,
                                         segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>,
                                         segmenterFjernet: LocalDateTimeline<AndelTilkjentYtelse>): Boolean {
        fun finnesEndretSegmentTilbakeITid(segmenter: LocalDateTimeline<AndelTilkjentYtelse>) =
                !segmenter.isEmpty && segmenter.any { !it.erLøpende() }

        return personSomSjekkes.erFramstiltKravForITidligereBehandling() &&
               (finnesEndretSegmentTilbakeITid(segmenterLagtTil) || finnesEndretSegmentTilbakeITid(segmenterFjernet))

    }

    private fun enesteEndringErTidligereStønadslutt(segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>,
                                                    segmenterFjernet: LocalDateTimeline<AndelTilkjentYtelse>,
                                                    sisteAndelPåPerson: AndelTilkjentYtelse?): Boolean {
        return if (sisteAndelPåPerson != null && segmenterLagtTil.isEmpty && !segmenterFjernet.isEmpty) {
            val stønadSlutt = sisteAndelPåPerson.stønadTom
            val opphører = stønadSlutt.isBefore(YearMonth.now().plusMonths(1))
            val ingenFjernetFørStønadslutt = segmenterFjernet.none { it.fom.isBefore(stønadSlutt.toLocalDate()) }
            opphører && ingenFjernetFørStønadslutt
        } else {
            false
        }
    }
}