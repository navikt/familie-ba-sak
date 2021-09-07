package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.erLøpende
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import java.time.YearMonth

object YtelsePersonUtils {

    /**
     * Utleder krav for personer framstilt nå og/eller tidligere.
     * Disse populeres med behandlingens utfall for enkeltpersonene (YtelsePerson),
     * som igjen brukes for å utlede det totale BehandlingResultat.
     *
     * @param [personerMedKrav] Personer framstilt krav for i denne behandlingen
     * @param [forrigeAndelerTilkjentYtelse] Eventuelle andeler fra forrige behandling
     * @return Liste med informasjon om hvordan hver enkelt person påvirkes i behandlingen (se YtelsePerson-doc)
     */
    fun utledKrav(personerMedKrav: List<Person> = emptyList(),
                  forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>): List<YtelsePerson> {

        val framstiltKravForNå =
                personerMedKrav.map { person ->
                    YtelsePerson(
                            personIdent = person.personIdent.ident,
                            ytelseType = when (person.type) {
                                PersonType.BARN -> YtelseType.ORDINÆR_BARNETRYGD
                                PersonType.SØKER -> YtelseType.UTVIDET_BARNETRYGD
                                PersonType.ANNENPART -> throw Feil("Kan ikke utlede krav for annen part")
                            },
                            kravOpprinnelse =
                            if (forrigeAndelerTilkjentYtelse.any { it.personIdent == person.personIdent.ident }) listOf(
                                    KravOpprinnelse.TIDLIGERE,
                                    KravOpprinnelse.INNEVÆRENDE)
                            else listOf(KravOpprinnelse.INNEVÆRENDE),
                    )
                }

        val kunFramstiltKravForTidligere: List<YtelsePerson> =
                forrigeAndelerTilkjentYtelse
                        .filter { andel ->
                            framstiltKravForNå.none {
                                andel.personIdent == it.personIdent
                            }
                        }
                        .map {
                            YtelsePerson(
                                    personIdent = it.personIdent,
                                    ytelseType = it.type,
                                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
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
                        kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                )
            }

    /**
     * Utleder hvilke konsekvenser _denne_ behandlingen har for personen og populerer "resultater" med utfallet.
     *
     * @param [ytelsePersoner] Personer framstilt krav for nå og/eller tidligere
     * @param [forrigeAndelerTilkjentYtelse] Eventuelle tilstand etter forrige behandling
     * @param [andelerTilkjentYtelse] Tilstand etter nåværende behandling
     * @return Personer populert med utfall (resultater) etter denne behandlingen
     */
    fun populerYtelsePersonerMedResultat(ytelsePersoner: List<YtelsePerson>,
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

            /**
             * En temporær løsning for å håndtere use caset med delt bosted, hvor beløpet men ikke innvilget periode er
             * endret.
             */
            val beløpEndretIUforandretTidslinje = (segmenterLagtTil + segmenterFjernet).isEmpty()
                                                  && andeler.sumOf { it.beløp } != forrigeAndeler.sumOf { it.beløp }

            val resultater = ytelsePerson.resultater.toMutableSet()
            if (avslagPåNyPerson(personSomSjekkes = ytelsePerson,
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

            if (andeler.isNotEmpty()
                && (beløpEndretIUforandretTidslinje
                    || finnesEndringTilbakeITid(personSomSjekkes = ytelsePerson,
                                                segmenterLagtTil = segmenterLagtTil,
                                                segmenterFjernet = segmenterFjernet)
                    || harGåttFraOpphørtTilLøpende(forrigeTilstandForPerson = forrigeAndeler,
                                                   oppdatertTilstandForPerson = andeler))
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

    private fun avslagPåNyPerson(personSomSjekkes: YtelsePerson, segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>) =
            personSomSjekkes.kravOpprinnelse == listOf(KravOpprinnelse.INNEVÆRENDE) && segmenterLagtTil.isEmpty

    private fun finnesInnvilget(personSomSjekkes: YtelsePerson, segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>) =
            personSomSjekkes.erFramstiltKravForIInneværendeBehandling() && !segmenterLagtTil.isEmpty

    private fun erYtelsenOpphørt(andeler: List<AndelTilkjentYtelse>) = andeler.none { it.erLøpende() }

    private fun finnesEndringTilbakeITid(personSomSjekkes: YtelsePerson,
                                         segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>,
                                         segmenterFjernet: LocalDateTimeline<AndelTilkjentYtelse>): Boolean {
        fun finnesEndretSegmentTilbakeITid(segmenter: LocalDateTimeline<AndelTilkjentYtelse>) =
                !segmenter.isEmpty && segmenter.any { !it.erLøpende() }

        return personSomSjekkes.erFramstiltKravForITidligereBehandling() &&
               (finnesEndretSegmentTilbakeITid(segmenterLagtTil) || finnesEndretSegmentTilbakeITid(segmenterFjernet))

    }

    private fun harGåttFraOpphørtTilLøpende(forrigeTilstandForPerson: List<AndelTilkjentYtelse>,
                                            oppdatertTilstandForPerson: List<AndelTilkjentYtelse>) =
            forrigeTilstandForPerson.isNotEmpty() && forrigeTilstandForPerson.none { it.erLøpende() } && oppdatertTilstandForPerson.any { it.erLøpende() }

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