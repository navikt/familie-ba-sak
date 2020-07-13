package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring.*
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.springframework.stereotype.Component

@Component
class UtbetalingsoppdragGenerator(
        val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
        val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        val fagsakService: FagsakService) {

    //Lager utbetalingsoppdrag direkte fra AndelTilkjentYtelse. Kobler sammen alle perioder som gjelder samme barn i en kjede,
    // bortsett ra småbarnstillegg som må ha sin egen kjede fordi det er en egen klasse i OS
    // PeriodeId = Vedtak.id * 1000 + offset
    // Beholder bare siste utbetalingsperiode hvis det er opphør.
    fun lagUtbetalingsoppdrag(saksbehandlerId: String,
                              vedtak: Vedtak,
                              behandlingResultatType: BehandlingResultatType,
                              erFørsteBehandlingPåFagsak: Boolean,
                              nyeAndeler: List<AndelTilkjentYtelse> = emptyList(),
                              opphørteAndeler: List<AndelTilkjentYtelse> = emptyList()): Utbetalingsoppdrag {

        val erFullstendigOpphør = behandlingResultatType == BehandlingResultatType.OPPHØRT
        if (erFullstendigOpphør && nyeAndeler.isNotEmpty()) {
            throw IllegalStateException("Finnes nye andeler når behandling skal opphøres")
        }

        // Hos økonomi skiller man på endring på oppdragsnivå og på linjenivå (periodenivå).
        // For å kunne behandling alle forlengelser/forkortelser av perioder likt har vi valgt å konsekvent opphøre nåværende
        // og erstatte med ny periode med oppdaterte datoer. På grunn av dette vil vi kun ha aksjonskode UEND ved fullstendig
        // opphør, selv om vi i realiteten av og til kun endrer datoer på en eksisterende linje (endring på linjenivå).
        val aksjonskodePåOppdragsnivå =
                if (erFørsteBehandlingPåFagsak) NY
                else if (erFullstendigOpphør) UEND
                else ENDR

        val nyeUtbetalingsperioder = lagUtbetalingsperioderAvAndeler(
                andelerForKjeding = delOppIKjeder(nyeAndeler),
                erFørsteBehandlingPåFagsak = erFørsteBehandlingPåFagsak,
                vedtak = vedtak,
                erOpphørtePerioder = false)
        val opphørteUtbetalingsperioder = lagUtbetalingsperioderAvAndeler(
                andelerForKjeding = delOppIKjeder(opphørteAndeler),
                erFørsteBehandlingPåFagsak = erFørsteBehandlingPåFagsak,
                vedtak = vedtak,
                erOpphørtePerioder = true)
                .kunSisteHvis(erFullstendigOpphør)

        return Utbetalingsoppdrag(
                saksbehandlerId = saksbehandlerId,
                kodeEndring = aksjonskodePåOppdragsnivå,
                fagSystem = FAGSYSTEM,
                saksnummer = vedtak.behandling.fagsak.id.toString(),
                aktoer = vedtak.behandling.fagsak.hentAktivIdent().ident,
                utbetalingsperiode = listOf(nyeUtbetalingsperioder, opphørteUtbetalingsperioder).flatten()
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
                                                erFørsteBehandlingPåFagsak: Boolean,
                                                erOpphørtePerioder: Boolean): List<Utbetalingsperiode> { // TODO: erOpphørslinje ?
        val fagsakId = vedtak.behandling.fagsak.id
        var offset = if (!erFørsteBehandlingPåFagsak) hentSisteOffsetPåFagsak(fagsakId) ?: throw IllegalStateException("Skal finnes offset?") else 0 // TODO: Avklar om man skal starte på nytt hvis gjenopptakelse

        val utbetalingsperiodeMal =
                if (erOpphørtePerioder)
                    UtbetalingsperiodeMal(vedtak, andelerForKjeding.flatten(), true)
                else
                    UtbetalingsperiodeMal(vedtak)

        val utbetalingsperioder = andelerForKjeding
                .flatMap { kjede: List<AndelTilkjentYtelse> ->
                    val ident = kjede.first().personIdent
                    val type = kjede.first().type
                    val erSøker = PersonType.SØKER == hentPersontypeForPerson(ident, kjede.first().behandlingId)
                    var forrigeOffsetPåPersonHvisFunnet: Int? = null
                    if (!erFørsteBehandlingPåFagsak) {
                        forrigeOffsetPåPersonHvisFunnet = if (erSøker) {
                            hentSisteOffsetForPerson(fagsakId = fagsakId, personIdent = ident, ytelseType = type)
                        } else {
                            hentSisteOffsetForPerson(fagsakId = fagsakId, personIdent = ident)
                        }
                    }
                    kjede.sortedBy { it.stønadFom }.mapIndexed { index, andel ->
                        val forrigeOffset = if (index == 0) forrigeOffsetPåPersonHvisFunnet else offset - 1
                        utbetalingsperiodeMal.lagPeriodeFraAndel(andel, offset, forrigeOffset).also {
                            andel.periodeOffset = offset.toLong()
                            offset++
                        }
                    }
                }
        lagreOppdaterteAndeler(andelerForKjeding.flatten())
        return utbetalingsperioder
    }

    fun hentPersontypeForPerson(personIdent: String, behandlingId: Long): PersonType {
        val personopplysningsgrunnlag =
                personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)
        return personopplysningsgrunnlag!!.personer.find { it.personIdent.ident == personIdent }!!.type
    }

    fun hentSisteOffsetForPerson(fagsakId: Long, personIdent: String, ytelseType: YtelseType? = null): Int? {
        val andelerPåFagsak = fagsakService.hentAndelerPåFagsak(fagsakId)
        val sorterteAndeler = andelerPåFagsak
                .filter { it.personIdent == personIdent }
                .sortedBy { it.periodeOffset }
        return if (ytelseType != null) {
            sorterteAndeler.filter { it.type == ytelseType }.lastOrNull()?.periodeOffset?.toInt()
        } else {
            sorterteAndeler.lastOrNull()?.periodeOffset?.toInt()
        }
    }

    fun hentSisteOffsetPåFagsak(fagsakId: Long): Int? {
        val andelerPåFagsak = fagsakService.hentAndelerPåFagsak(fagsakId)
        val sorterteAndeler = andelerPåFagsak.sortedBy { it.periodeOffset }
        return sorterteAndeler.lastOrNull()?.periodeOffset?.toInt()
    }

    fun lagreOppdaterteAndeler(andeler: List<AndelTilkjentYtelse>) {
        andelTilkjentYtelseRepository.saveAll(andeler)
    }

    fun delOppIKjeder(andelerSomSkalSplittes: List<AndelTilkjentYtelse>): List<List<AndelTilkjentYtelse>> {
        // Separereer i lister siden småbarnstillegg og utvidet barnetrygd begge vil stå på forelder, men skal kjedes separat
        val (personMedSmåbarnstilleggAndeler, personerMedAndeler) =
                andelerSomSkalSplittes.partition { it.type == YtelseType.SMÅBARNSTILLEGG }.toList().map {
                    it.groupBy { andel -> andel.personIdent } // TODO: Hva skjer dersom personidenten endrer seg? Bør gruppere på en annen måte og oppdatere lagingen av utbetalingsperioder fra andeler
                }
        val andelerForKjeding = listOf(personMedSmåbarnstilleggAndeler.values, personerMedAndeler.values).flatten()
        if (personMedSmåbarnstilleggAndeler.size > 1) {
            throw IllegalArgumentException("Finnes flere personer med småbarnstillegg")
        }
        return andelerForKjeding
    }
}