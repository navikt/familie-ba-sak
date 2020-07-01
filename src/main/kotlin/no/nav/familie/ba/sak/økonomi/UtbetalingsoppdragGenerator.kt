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

        val (personMedSmåbarnstilleggAndeler, personerMedAndeler) =
                andelerTilkjentYtelse.partition { it.type == YtelseType.SMÅBARNSTILLEGG }.toList().map {
                    it.groupBy { andel -> andel.personIdent }
                }

        if (personMedSmåbarnstilleggAndeler.size > 1) {
            throw IllegalArgumentException("Finnes flere personer med småbarnstillegg")
        }

        val samledeAndeler: List<List<AndelTilkjentYtelse>> =
                listOf(personMedSmåbarnstilleggAndeler.values.toList(), personerMedAndeler.values.toList()).flatten()

        var offset = 0

        val utbetalingsperioder: List<Utbetalingsperiode> = samledeAndeler
                .flatMap { andelerForPerson: List<AndelTilkjentYtelse> ->

                    if (!erFørsteBehandlingPåFagsak) {
                        val ident = andelerForPerson.first().personIdent
                        val type = andelerForPerson.first().type

                        offset = if (type == YtelseType.SMÅBARNSTILLEGG) {
                            hentSisteOffsetForPerson(personIdent = ident, ytelseType = type) ?: 0
                        } else {
                            hentSisteOffsetForPerson(ident) ?: 0
                        }
                    }

                    andelerForPerson.sortedBy { it.stønadFom }.mapIndexed { index, andel ->
                        val forrigeOffset = if (index == 0) null else offset - 1
                        utbetalingsperiodeMal.lagPeriodeFraAndel(andel, offset, forrigeOffset).also {
                            andel.periodeOffset = offset.toLong()
                            andelTilkjentYtelseRepository.save(andel)
                            offset++
                        }
                    }.kunSisteHvis(erOpphør)
                }

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
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForPerson(personIdent).sortedBy { it.periodeOffset }
        return if (ytelseType != null) {
            sorterteAndeler.filter { it.type == ytelseType }.lastOrNull()?.periodeOffset?.toInt()
        } else {
            sorterteAndeler.lastOrNull()?.periodeOffset?.toInt()
        }
    }
}