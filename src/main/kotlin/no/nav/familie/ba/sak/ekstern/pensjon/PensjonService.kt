package no.nav.familie.ba.sak.ekstern.pensjon

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.ekstern.bisys.BisysService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
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
    private val kompetanseService: KompetanseService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository
) {
    fun hentBarnetrygd(personIdent: String, fraDato: LocalDate): List<BarnetrygdTilPensjon> {
        val barnetrygdTilPensjon = hentBarnetrygdTilPensjon(personIdent, fraDato)

        // Sjekk om det finnes relaterte saker, dvs om barna finnes i andre behandlinger

        val barnetrygdMedRelaterteSaker = barnetrygdTilPensjon?.barnetrygdPerioder
            ?.filter { it.personIdent != barnetrygdTilPensjon.fagsakEiersIdent }
            ?.map { barnetrygdPeriode -> hentBarnetrygdForRelatertPersonTilPensjon(barnetrygdPeriode.personIdent, fraDato) }
            ?.flatten()
        return barnetrygdMedRelaterteSaker?.plus(barnetrygdTilPensjon) ?: emptyList()
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

        val vedtak = vedtakService.hentAktivForBehandling(behandling.id)

        var datoVedtak = vedtak?.vedtaksdato

        val perioder = hentPerioder(behandling, fraDato)

        return BarnetrygdTilPensjon(
            fagsakId = fagsak.id.toString(),
            barnetrygdPerioder = perioder,
            fagsakEiersIdent = personIdent,
        )
    }

    private fun hentPerioder(
        behandling: Behandling,
        fraDato: LocalDate
    ): List<BarnetrygdPeriode> {
        val perioder =
            tilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandling.id)?.andelerTilkjentYtelse
                ?.filter {
                    it.stønadTom.isSameOrAfter(fraDato.toYearMonth())
                }
                ?.map {
                    BarnetrygdPeriode(
                        ytelseType = it.type.tilPensjonYtelsesType(),
                        personIdent = it.aktør.aktivFødselsnummer(),
                        stønadFom = it.stønadFom,
                        stønadTom = it.stønadTom,
                        utbetaltPerMnd = it.kalkulertUtbetalingsbeløp,
                        delingsprosentYtelse = it.prosent.toInt()
                    )
                } ?: emptyList()
        return perioder
    }

    fun no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.tilPensjonYtelsesType(): YtelseType {
        return when (this) {
            no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.ORDINÆR_BARNETRYGD -> YtelseType.ORDINÆR_BARNETRYGD
            no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.SMÅBARNSTILLEGG -> YtelseType.SMÅBARNSTILLEGG
            no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.UTVIDET_BARNETRYGD -> YtelseType.UTVIDET_BARNETRYGD
        }
    }
    companion object {
        private val logger = LoggerFactory.getLogger(BisysService::class.java)
    }
}
