package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring.NY
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring.UEND
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.springframework.stereotype.Component

@Component
class UtbetalingsoppdragGenerator(
        val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository) {

    //Lager utbetalingsoppdrag direkte fra AndelTilkjentYtelse. Kobler sammen alle perioder som gjelder samme barn i en kjede,
    // bortsett ra småbarnstillegg som må ha sin egen kjede fordi det er en egen klasse i OS
    // PeriodeId = Vedtak.id * 1000 + offset
    // Beholder bare siste utbetalingsperiode hvis det er opphør.
    fun lagUtbetalingsoppdrag(saksbehandlerId: String,
                              vedtak: Vedtak,
                              behandlingResultatType: BehandlingResultatType,
                              erFørsteBehandlingPåFagsak: Boolean,
                              andelerTilkjentYtelse: List<AndelTilkjentYtelse>): Utbetalingsoppdrag {

        val erOpphør = behandlingResultatType == BehandlingResultatType.OPPHØRT

        val utbetalingsperiodeMal =
                if (erOpphør)
                    UtbetalingsperiodeMal(vedtak, true, vedtak.forrigeVedtakId!!)
                else
                    UtbetalingsperiodeMal(vedtak)

        // Må separere i lister siden småbarnstillegg og utvidet barnetrygd begge vil stå på forelder, men skal kjedes separat
        val (personMedSmåbarnstilleggAndeler, personerMedAndeler) =
                andelerTilkjentYtelse.partition { it.type == YtelseType.SMÅBARNSTILLEGG }.toList().map {
                    it.groupBy { andel -> andel.personIdent }
                }

        val alleIdenterPåFagsak = setOf( personMedSmåbarnstilleggAndeler.keys, personerMedAndeler.keys).flatten().toList()

        var offset = if (!erFørsteBehandlingPåFagsak) hentSisteOffsetPåFagsak(alleIdenterPåFagsak) ?: 0 else 0

        val utbetalingsperioder: MutableList<Utbetalingsperiode> = mutableListOf()

        if (personMedSmåbarnstilleggAndeler.size > 1) {
            throw IllegalArgumentException("Finnes flere personer med småbarnstillegg")
        } else {
            val utbetalingsperioderSmåbarn: List<Utbetalingsperiode> = personMedSmåbarnstilleggAndeler
                    .flatMap { (ident: String, andelerForPerson: List<AndelTilkjentYtelse>) ->

                        /*

                        if (!erFørsteBehandlingPåFagsak) {
                            val ident = andelerForPerson.first().personIdent
                            val type = andelerForPerson.first().type

                            forrigeOffsetHvisFunnet = if (type == YtelseType.SMÅBARNSTILLEGG) {
                                hentSisteOffsetForPerson(personIdent = ident, ytelseType = type)
                            } else {
                                hentSisteOffsetForPerson(ident)
                            }
                        }
                        */

                        andelerForPerson.sortedBy { it.stønadFom }.mapIndexed { index, andel ->
                            val forrigeOffset = if (index == 0) null else offset - 1
                            utbetalingsperiodeMal.lagPeriodeFraAndel(andel, offset, forrigeOffset).also {
                                andel.periodeOffset = offset.toLong()
                                andelTilkjentYtelseRepository.save(andel)
                                offset++
                            }
                        }.kunSisteHvis(erOpphør)
                    }
            utbetalingsperioder.addAll(utbetalingsperioderSmåbarn)
        }

        // Småbarnstillegg og utvidet barnetrygd skal ha to forskjellige kjeder. Derfor ikke lagt i map, da forelder ville hatt to identiske keys.
        //val samledeAndeler: List<List<AndelTilkjentYtelse>> = listOf(personMedSmåbarnstilleggAndeler.values.toList(), personerMedAndeler.values.toList()).flatten()


        val utbetalingsperioderResten: List<Utbetalingsperiode> = personerMedAndeler
                .flatMap { (ident: String, andelerForPerson: List<AndelTilkjentYtelse>) ->

                    /*

                    if (!erFørsteBehandlingPåFagsak) {
                        val ident = andelerForPerson.first().personIdent
                        val type = andelerForPerson.first().type

                        forrigeOffsetHvisFunnet = if (type == YtelseType.SMÅBARNSTILLEGG) {
                            hentSisteOffsetForPerson(personIdent = ident, ytelseType = type)
                        } else {
                            hentSisteOffsetForPerson(ident)
                        }
                    }
                    */

                    andelerForPerson.sortedBy { it.stønadFom }.mapIndexed { index, andel ->
                        val forrigeOffset = if (index == 0) null else offset - 1
                        utbetalingsperiodeMal.lagPeriodeFraAndel(andel, offset, forrigeOffset).also {
                            andel.periodeOffset = offset.toLong()
                            andelTilkjentYtelseRepository.save(andel)
                            offset++
                        }
                    }.kunSisteHvis(erOpphør)
                }
        utbetalingsperioder.addAll(utbetalingsperioderResten)

        return Utbetalingsoppdrag(
                saksbehandlerId = saksbehandlerId,
                kodeEndring = if (!erOpphør && erFørsteBehandlingPåFagsak) NY else UEND,
                fagSystem = FAGSYSTEM,
                saksnummer = vedtak.behandling.fagsak.id.toString(),
                aktoer = vedtak.behandling.fagsak.hentAktivIdent().ident,
                utbetalingsperiode = utbetalingsperioder
        )
    }

    // Et utbetalingsoppdrag for opphør inneholder kun én (den siste) utbetalingsperioden for hvert barn
    fun <T> List<T>.kunSisteHvis(kunSiste: Boolean): List<T> {
        return this.foldRight(mutableListOf()) { element, resultat ->
            if (resultat.size == 0 || !kunSiste) resultat.add(0, element);resultat
        }
    }

    private fun hentSisteOffsetForPerson(personIdent: String, ytelseType: YtelseType? = null): Int? {
        val sorterteAndeler =
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForPersoner(listOf(personIdent))
                        .sortedBy { it.periodeOffset }
        return if (ytelseType != null) {
            sorterteAndeler.filter { it.type == ytelseType }.lastOrNull()?.periodeOffset?.toInt()
        } else {
            sorterteAndeler.lastOrNull()?.periodeOffset?.toInt()
        }
    }

    private fun hentSisteOffsetPåFagsak(personIdenter: List<String>): Int? {
        val sorterteAndeler =
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForPersoner(personIdenter).sortedBy { it.periodeOffset }
        return sorterteAndeler.lastOrNull()?.periodeOffset?.toInt()
    }
}