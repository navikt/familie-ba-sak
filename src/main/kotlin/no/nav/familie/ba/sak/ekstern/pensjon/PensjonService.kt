package no.nav.familie.ba.sak.ekstern.pensjon

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.ekstern.bisys.BisysService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PensjonService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val fagsakRepository: FagsakRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val personidentService: PersonidentService,
    private val vedtakService: VedtakService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {
    fun hentBarnetrygd(personIdent: String, fraDato: LocalDate): List<BarnetrygdTilPensjon> {
        val barnetrygdTilPensjon = hentBarnetrygdTilPensjon(personIdent, fraDato) ?: return emptyList()

        // Sjekk om det finnes relaterte saker, dvs om barna finnes i andre behandlinger

        val barnetrygdMedRelaterteSaker = barnetrygdTilPensjon.barnetrygdPerioder
            .map { it.personIdent }.distinct()
            .filter { it != barnetrygdTilPensjon.fagsakEiersIdent }
            .map { hentBarnetrygdForRelatertPersonTilPensjon(it, fraDato) }
            .flatten()
        return barnetrygdMedRelaterteSaker.plus(barnetrygdTilPensjon).distinct()
    }
    private fun hentBarnetrygdForRelatertPersonTilPensjon(personIdent: String, fraDato: LocalDate): List<BarnetrygdTilPensjon> {
        val aktør = personidentService.hentAktør(personIdent)
        val andeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForAktør(aktør)
        // finn alle behandlinger
        val behandlinger = andeler.map { it.behandlingId }.toSet()
        // finn alle fagsaker
        val aktørerTilEierAvFagsaker = behandlinger.map { behandlingHentOgPersisterService.hent(it).fagsak.aktør }
        // hent alle barnetrygdperiodene for de aktuelle fagsakene.
        return aktørerTilEierAvFagsaker.map { aktør -> hentBarnetrygdTilPensjon(aktør.aktivFødselsnummer(), fraDato) }.filterNotNull()
    }
    private fun hentBarnetrygdTilPensjon(personIdent: String, fraDato: LocalDate): BarnetrygdTilPensjon? {
        val aktør = personidentService.hentAktør(personIdent)
        val fagsak = fagsakRepository.finnFagsakForAktør(aktør)
        val behandling = fagsak?.let { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(it.id) }
        if (fagsak == null || behandling == null) {
            return null
        }
        logger.info("Henter perioder med barnetrygd til pensjon for fagsakId=${fagsak.id}, behandlingId=${behandling.id}")

        val perioder = hentPerioder(behandling, fraDato)

        return BarnetrygdTilPensjon(
            fagsakId = fagsak.id.toString(),
            barnetrygdPerioder = perioder,
            fagsakEiersIdent = personIdent,
        )
    }

    private fun hentPerioder(
        behandling: Behandling,
        fraDato: LocalDate,
    ): List<BarnetrygdPeriode> {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandling.id)
            ?: error("Finner ikke tilkjent ytelse for behandling=${behandling.id}")
        return tilkjentYtelse.andelerTilkjentYtelse
            .filter { it.stønadTom.isSameOrAfter(fraDato.toYearMonth()) }
            .map {
                BarnetrygdPeriode(
                    ytelseTypeEkstern = it.type.tilPensjonYtelsesType(),
                    personIdent = it.aktør.aktivFødselsnummer(),
                    stønadFom = it.stønadFom,
                    stønadTom = it.stønadTom,
                    utbetaltPerMnd = it.kalkulertUtbetalingsbeløp,
                    delingsprosentYtelse = it.prosent.toInt(),
                )
            }
    }

    fun YtelseType.tilPensjonYtelsesType(): YtelseTypeEkstern {
        return when (this) {
            YtelseType.ORDINÆR_BARNETRYGD -> YtelseTypeEkstern.ORDINÆR_BARNETRYGD
            YtelseType.SMÅBARNSTILLEGG -> YtelseTypeEkstern.SMÅBARNSTILLEGG
            YtelseType.UTVIDET_BARNETRYGD -> YtelseTypeEkstern.UTVIDET_BARNETRYGD
        }
    }
    companion object {
        private val logger = LoggerFactory.getLogger(BisysService::class.java)
    }
}
