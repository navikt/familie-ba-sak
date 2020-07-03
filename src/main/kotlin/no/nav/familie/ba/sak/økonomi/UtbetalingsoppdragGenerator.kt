package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
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
        val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
        val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository) {

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

        // Må separere i lister siden småbarnstillegg og utvidet barnetrygd begge vil stå på forelder, men skal kjedes separat
        val (personMedSmåbarnstilleggAndeler, personerMedAndeler) =
                andelerTilkjentYtelse.partition { it.type == YtelseType.SMÅBARNSTILLEGG }.toList().map {
                    it.groupBy { andel -> andel.personIdent } // TODO: Hva skjer dersom personidenten endrer seg? Bør gruppere på en annen måte og oppdatere lagingen av utbetalingsperioder fra andeler
                }

        val alleUnikeIdenterPåFagsak = setOf(personMedSmåbarnstilleggAndeler.keys, personerMedAndeler.keys).flatten().toList()
        val andelerForKjeding = listOf(personMedSmåbarnstilleggAndeler.values, personerMedAndeler.values).flatten()

        if (personMedSmåbarnstilleggAndeler.size > 1) {
            throw IllegalArgumentException("Finnes flere personer med småbarnstillegg")
        }
        val utbetalingsperioder = lagUtbetalingsperioderAvAndeler(andelerForKjeding = andelerForKjeding,
                                                                  alleIdenterPåFagsak = alleUnikeIdenterPåFagsak,
                                                                  behandlingResultatType = behandlingResultatType,
                                                                  erFørsteBehandlingPåFagsak = erFørsteBehandlingPåFagsak,
                                                                  vedtak = vedtak)

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


    private fun lagUtbetalingsperioderAvAndeler(andelerForKjeding: List<List<AndelTilkjentYtelse>>,
                                                vedtak: Vedtak,
                                                behandlingResultatType: BehandlingResultatType,
                                                erFørsteBehandlingPåFagsak: Boolean,
                                                alleIdenterPåFagsak: List<String>): List<Utbetalingsperiode> {

        var offset = if (!erFørsteBehandlingPåFagsak) hentSisteOffsetPåFagsak(alleIdenterPåFagsak) ?: 0 else 0

        val erOpphør = behandlingResultatType == BehandlingResultatType.OPPHØRT

        val utbetalingsperiodeMal =
                if (erOpphør)
                    UtbetalingsperiodeMal(vedtak, true, vedtak.forrigeVedtakId!!)
                else
                    UtbetalingsperiodeMal(vedtak)

        val vedtakBehandlingId = vedtak.behandling.id
        val utbetalingsperioder = andelerForKjeding
                .flatMap { kjede: List<AndelTilkjentYtelse> ->
                    val ident = kjede.find { it.behandlingId == vedtakBehandlingId }!!.personIdent
                    val type = kjede.find { it.behandlingId == vedtakBehandlingId }!!.type
                    val personopplysningsgrunnlag =
                            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = vedtakBehandlingId)
                    val personType = personopplysningsgrunnlag!!.personer.find { it.personIdent.ident == ident }!!.type
                    val erSøker =
                            (personType == PersonType.SØKER)
                    var forrigeOffsetPåPersonHvisFunnet: Int? = null
                    if (!erFørsteBehandlingPåFagsak) {
                        forrigeOffsetPåPersonHvisFunnet = if (erSøker) {
                            hentSisteOffsetForPerson(personIdent = ident, ytelseType = type)
                        } else {
                            hentSisteOffsetForPerson(ident)
                        }
                    }
                    kjede.sortedBy { it.stønadFom }.mapIndexed { index, andel ->
                        val forrigeOffset = if (index == 0) forrigeOffsetPåPersonHvisFunnet else offset - 1
                        utbetalingsperiodeMal.lagPeriodeFraAndel(andel, offset, forrigeOffset).also {
                            andel.periodeOffset = offset.toLong()
                            offset++
                        }
                    }.kunSisteHvis(erOpphør)
                }
        lagreOppdaterteAndeler(andelerForKjeding.flatten())
        return utbetalingsperioder
    }

    fun hentSisteOffsetForPerson(personIdent: String, ytelseType: YtelseType? = null): Int? {
        val sorterteAndeler =
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForPersoner(listOf(personIdent))
                        .sortedBy { it.periodeOffset }
        return if (ytelseType != null) {
            sorterteAndeler.filter { it.type == ytelseType }.lastOrNull()?.periodeOffset?.toInt()
        } else {
            sorterteAndeler.lastOrNull()?.periodeOffset?.toInt()
        }
    }

    fun hentSisteOffsetPåFagsak(personIdenter: List<String>): Int? {
        val sorterteAndeler =
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForPersoner(personIdenter).sortedBy { it.periodeOffset }
        return sorterteAndeler.lastOrNull()?.periodeOffset?.toInt()
    }

    fun lagreOppdaterteAndeler(andeler: List<AndelTilkjentYtelse>) {
        andelTilkjentYtelseRepository.saveAll(andeler)
    }
}