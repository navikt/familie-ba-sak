package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring.*
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.springframework.stereotype.Component
import java.time.LocalDate


@Component
class UtbetalingsoppdragGenerator(
        val persongrunnlagService: PersongrunnlagService,
        val beregningService: BeregningService) {

    /**
     * Lager utbetalingsoppdrag av AndelTilkjentYtelse. Kobler sammen alle perioder som gjelder samme person i en kjede,
     * bortsett fra småbarnstillegg og utvidet barnetrygd som separeres i to kjeder for søker.
     * Sender kun siste utbetalingsperiode (med opphørsdato) hvis det er opphør.
     *
     * @param[saksbehandlerId] settes på oppdragsnivå
     * @param[vedtak] for å hente fagsakid, behandlingid, vedtaksdato, ident, og evt opphørsdato
     * @param[behandlingResultatType] for å sjekke om fullstendig opphør
     * @param[erFørsteBehandlingPåFagsak] brukes til å sette aksjonskode på oppdragsnivå og bestemme om vi skal telle fra start
     * @param[nyeAndeler] én liste per kjede, hvor andeler er helt nye, har endrede datoer eller må bygges opp igjen pga endringer før i kjeden
     * @param[opphørteAndeler] et par per kjede, hvor par består av siste andel i kjeden og
     * @return Utbetalingsoppdrag for vedtak
     */
    fun lagUtbetalingsoppdrag(saksbehandlerId: String,
                              vedtak: Vedtak,
                              behandlingResultatType: BehandlingResultatType,
                              erFørsteBehandlingPåFagsak: Boolean,
                              kjedefordelteNyeAndeler: List<List<AndelTilkjentYtelse>> = emptyList(),
                              kjedefordelteOpphørMedDato: List<Pair<AndelTilkjentYtelse, LocalDate>> = emptyList()): Utbetalingsoppdrag {

        val erFullstendigOpphør = behandlingResultatType == BehandlingResultatType.OPPHØRT
        if (erFullstendigOpphør && kjedefordelteNyeAndeler.isNotEmpty()) {// TODO: Bør også sjekke at erFullstendigOpphør inneholder akkurat antall personer på ytelse + småbarn evt
            throw IllegalStateException("Finnes nye andeler når behandling skal opphøres")
        }

        // Hos økonomi skiller man på endring på oppdragsnivå og på linjenivå (periodenivå).
        // For å kunne behandling alle forlengelser/forkortelser av perioder likt har vi valgt å konsekvent opphøre nåværende
        // og erstatte med ny periode med oppdaterte datoer. På grunn av dette vil vi på oppdragsnivå kun ha aksjonskode UEND ved
        // fullstendig opphør, selv om vi i realiteten av og til kun endrer datoer på en eksisterende linje (endring på linjenivå).
        val aksjonskodePåOppdragsnivå =
                if (erFørsteBehandlingPåFagsak) NY
                else if (erFullstendigOpphør) UEND
                else ENDR

        val nyeUtbetalingsperioder = if (kjedefordelteNyeAndeler.isNotEmpty())
            lagUtbetalingsperioderAvAndeler(
                    andelerForKjeding = kjedefordelteNyeAndeler,
                    erFørsteBehandlingPåFagsak = erFørsteBehandlingPåFagsak,
                    vedtak = vedtak) else emptyList()

        val opphørteUtbetalingsperioder = if (kjedefordelteOpphørMedDato.isNotEmpty())
            lagOpphørsperioderAvAndeler( // TODO: "Ryddelinjer" og fullt opphør
                    kjedefordelteOpphørMedDato,
                    vedtak = vedtak) else emptyList()

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

    private fun lagOpphørsperioderAvAndeler(andelerMedDato: List<Pair<AndelTilkjentYtelse, LocalDate>>,
                                            vedtak: Vedtak): List<Utbetalingsperiode> {
        val utbetalingsperiodeMal = UtbetalingsperiodeMal(vedtak, true)
        return andelerMedDato.map { (sisteAndelIKjede, opphørKjedeFom) ->
            utbetalingsperiodeMal.lagPeriodeFraAndel(andel = sisteAndelIKjede,
                                                     periodeIdOffset = sisteAndelIKjede.periodeOffset!!.toInt(),
                                                     forrigePeriodeIdOffset = null,
                                                     opphørKjedeFom = opphørKjedeFom)
        }
    }

    private fun lagUtbetalingsperioderAvAndeler(andelerForKjeding: List<List<AndelTilkjentYtelse>>,
                                                vedtak: Vedtak,
                                                erFørsteBehandlingPåFagsak: Boolean): List<Utbetalingsperiode> {
        val fagsakId = vedtak.behandling.fagsak.id
        var offset = if (!erFørsteBehandlingPåFagsak) hentSisteOffsetPåFagsak(fagsakId)
                                                      ?: throw IllegalStateException("Skal finnes offset?") else 0

        val utbetalingsperiodeMal = UtbetalingsperiodeMal(vedtak)

        // TODO: Tidligere ville man begynne med samme utgangspunkt som siste behandling og telle null, 0, 1 osv så man endte med identiske indekser som sist og så valgte den samme.
        // I realiteten genererte man alltid et identisk resultat og valgte den siste.

        // TODO: Konsept: Vi ønsker alltid å rulle framover. Det eneste vi trenger forrige indeks for er den aller siste for å begynne å telle derfra og hekte på der vi slapp.
        // Vi endrer aldri eksisterende og trenger aldri referanser bakover, kun nåværende bilde.
        // Dvs at vi for endringer alltid vil lage nye og telle oppver
        // Dvs at vi for opphør vil opphøre alle og ikke trenger å vite hva forrige referanse er hos økonomi. Dette er også mer intuitivt enn sending av gamle og telling bakover hos økonmi.

        val utbetalingsperioder =
                andelerForKjeding // TODO: Hvordan blir dette ved opphør? Skal ikke kjøre gjennom og øke indekser da, men korrigere siste linje med peker til første dato
                        .flatMap { kjede: List<AndelTilkjentYtelse> ->
                            val ident = kjede.first().personIdent
                            val type = kjede.first().type
                            val erSøker = erSøkerPåBehandling(ident, vedtak.behandling)
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
        beregningService.lagreOppdaterteAndelerTilkjentYtelse(andelerForKjeding.flatten())
        return utbetalingsperioder
    }

    fun erSøkerPåBehandling(ident: String, behandling: Behandling): Boolean =
            persongrunnlagService.hentSøker(behandling)!!.personIdent.ident == ident

    fun hentSisteOffsetForPerson(fagsakId: Long, personIdent: String, ytelseType: YtelseType? = null): Int? {
        val andelerPåFagsak = beregningService.hentAndelerTilkjentYtelseForFagsak(fagsakId)
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
        val andelerPåFagsak = beregningService.hentAndelerTilkjentYtelseForFagsak(fagsakId)
        val sorterteAndeler = andelerPåFagsak.sortedBy { it.periodeOffset }
        return sorterteAndeler.lastOrNull()?.periodeOffset?.toInt()
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