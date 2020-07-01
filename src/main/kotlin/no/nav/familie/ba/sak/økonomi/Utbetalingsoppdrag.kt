package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.senesteDatoAv
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring.NY
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring.UEND
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode.SatsType.MND
import java.math.BigDecimal
import java.time.LocalDate

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
                UtbetalingsperiodeMal(vedtak, andelerTilkjentYtelse, true, vedtak.forrigeVedtakId!!)
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

    // TODO: Dette må gjøres annerledes for revurderinger. Her må kjeden for hver person starte med en offset som er lik
    //  den siste periodeId-en for personen i forrige behandling, eller starte på 0 hvis personen ikke hadde andeler i
    //  forrige behandling. 
    var offset = 0
    val utbetalingsperioder: List<Utbetalingsperiode> = samledeAndeler
            .flatMap { andelerForPerson: List<AndelTilkjentYtelse> ->
                andelerForPerson.sortedBy { it.stønadFom }.mapIndexed { index, andel ->
                    val forrigeOffset = if (index == 0) null else offset - 1
                    utbetalingsperiodeMal.lagPeriodeFraAndel(andel, offset, forrigeOffset).also { offset++ }
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

data class UtbetalingsperiodeMal(
        val vedtak: Vedtak,
        val andeler: List<AndelTilkjentYtelse>? = null,
        val erEndringPåEksisterendePeriode: Boolean = false,
        val periodeIdStart: Long = vedtak.id
) {

    private val MAX_PERIODEID_OFFSET = 1_000

    fun lagPeriodeFraAndel(andel: AndelTilkjentYtelse, periodeIdOffset: Int, forrigePeriodeIdOffset: Int?): Utbetalingsperiode {

        // Vedtak-id øker med 50, så vi kan ikke risikere overflow
        if (periodeIdOffset >= MAX_PERIODEID_OFFSET) {
            throw IllegalArgumentException("periodeIdOffset forsøkt satt høyere enn ${MAX_PERIODEID_OFFSET}. " +
                                           "Det ville ført til duplisert periodeId")
        }

        // Skaper "plass" til offset
        val utvidetPeriodeIdStart = periodeIdStart * MAX_PERIODEID_OFFSET

        return Utbetalingsperiode(
                erEndringPåEksisterendePeriode = erEndringPåEksisterendePeriode,
                opphør = vedtak.opphørsdato?.let { Opphør(senesteDatoAv(vedtak.opphørsdato, tidligsteFomDatoIKjede(andel))) },
                forrigePeriodeId = forrigePeriodeIdOffset?.let { utvidetPeriodeIdStart + forrigePeriodeIdOffset.toLong() },
                periodeId = utvidetPeriodeIdStart + periodeIdOffset,
                datoForVedtak = vedtak.vedtaksdato,
                klassifisering = andel.type.klassifisering,
                vedtakdatoFom = andel.stønadFom,
                vedtakdatoTom = andel.stønadTom,
                sats = BigDecimal(andel.beløp),
                satsType = MND,
                utbetalesTil = vedtak.behandling.fagsak.hentAktivIdent().ident,
                behandlingId = vedtak.behandling.id
        )
    }

    fun tidligsteFomDatoIKjede(andel: AndelTilkjentYtelse): LocalDate {
        return andeler!!.filter { it.type == andel.type && it.personIdent == andel.personIdent }
                .sortedBy { it.stønadFom }
                .first().stønadFom
    }
}
