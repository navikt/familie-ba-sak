package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.Satskjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.task.OpprettTaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.YearMonth

@Service
class StartSatsendring(
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val opprettTaskService: OpprettTaskService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val satskjøringRepository: SatskjøringRepository,
    private val featureToggleService: FeatureToggleService,
    private val personidentService: PersonidentService

) {

    @Transactional
    fun startSatsendring(
        antallFagsaker: Int,
        satsTidspunkt: YearMonth = YearMonth.of(2023, 3)
    ) {
        val gyldigeSatstyper = hentGyldigeSatstyper()
        if (gyldigeSatstyper.isEmpty()) {
            logger.info("Skipper satsendring da ingen av bryterne for de ulike satstypene er påskrudd.")
            return
        }
        var antallSatsendringerStartet = 0
        var startSide = 0
        while (antallSatsendringerStartet < antallFagsaker) {
            val page =
                fagsakRepository.finnLøpendeFagsakerForSatsendring(Pageable.ofSize(PAGE_STØRRELSE))

            val fagsakerForSatsendring = page.toList()
            logger.info("Fant ${fagsakerForSatsendring.size} personer for satsendring på side $startSide")
            if (fagsakerForSatsendring.isNotEmpty()) {
                antallSatsendringerStartet =
                    oppretteEllerSkipSatsendring(
                        fagsakerForSatsendring,
                        antallSatsendringerStartet,
                        antallFagsaker,
                        satsTidspunkt,
                        gyldigeSatstyper
                    )
            }

            if (++startSide == page.totalPages) break
        }
    }

    private fun harYtelsetype(
        ytelseType: YtelseType,
        andelerTilkjentYtelseMedEndreteUtbetalinger: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        tidspunkt: YearMonth,
        sats: Int? = null
    ): Boolean {
        return if (sats == null) {
            andelerTilkjentYtelseMedEndreteUtbetalinger.any {
                it.type == ytelseType && it.stønadFom.isBefore(tidspunkt) && it.stønadTom.isSameOrAfter(
                    tidspunkt
                ) && it.prosent != BigDecimal(50) // Ignorerer delt bosted i fase 1
            }
        } else {
            andelerTilkjentYtelseMedEndreteUtbetalinger.any {
                it.type == ytelseType && it.sats == sats && it.stønadFom.isBefore(tidspunkt) && it.stønadTom.isSameOrAfter(
                    tidspunkt
                ) && it.prosent != BigDecimal(50) // Ignorerer delt bosted i fase 1
            }
        }
    }

    private fun sjekkOgTriggSatsendring(
        satstyper: List<SatsType>,
        fagsak: Fagsak,
        gyldigeSatstyper: List<SatsType>,
        satsTidspunkt: YearMonth
    ): Boolean {
        if (satstyper.isNotEmpty() && gyldigeSatstyper.containsAll(satstyper)) {
            return if (featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_OPPRETT_TASKER)) {
                logger.info("Oppretter satsendringtask for fagsak=${fagsak.id}")
                opprettTaskService.opprettSatsendringTask(fagsak.id, satsTidspunkt)
                satskjøringRepository.save(Satskjøring(fagsakId = fagsak.id))
                true
            } else {
                logger.info("Oppretter ikke satsendringtask for fagsak=${fagsak.id}. Toggle SATSENDRING_OPPRETT_TASKER avskrudd.")
                true // fordi vi vil at den skal telles selv om opprett task er skrudd av
            }
        }
        logger.info(
            "Oppretter ikke satsendringtask for fagsak=${fagsak.id}. Mangler ytelse, eller har ytelsestype(r) det ikke" +
                " skal kjøres for: ${satstyper.filter { it !in gyldigeSatstyper }}"
        )
        return false
    }

    private fun hentGyldigeSatstyper(): List<SatsType> {
        val gyldigeSatstyper = mutableListOf<SatsType>()
        if (featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_TILLEGG_ORBA, false)) {
            gyldigeSatstyper.add(SatsType.TILLEGG_ORBA)
        }

        if (featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_ORBA, true)) {
            gyldigeSatstyper.add(SatsType.ORBA)
        }

        if (featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_UTVIDET, false)) {
            gyldigeSatstyper.add(SatsType.UTVIDET_BARNETRYGD)
        }

        if (featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_SMA, false)) {
            gyldigeSatstyper.add(SatsType.SMA)
        }

        logger.info("Påskrudde satstyper for satskjøring $gyldigeSatstyper")
        return gyldigeSatstyper
    }

    private fun oppretteEllerSkipSatsendring(
        fagsakForSatsendring: List<Fagsak>,
        antallAlleredeTriggetSatsendring: Int,
        antallFagsakerTilSatsendring: Int,
        satsTidspunkt: YearMonth,
        gyldigeSatstyper: List<SatsType>
    ): Int {
        var antallFagsakerSatsendring = antallAlleredeTriggetSatsendring
        for (fagsak in fagsakForSatsendring) {
            if (skalTriggeFagsak(fagsak, satsTidspunkt, gyldigeSatstyper)) {
                antallFagsakerSatsendring++
            }

            if (antallFagsakerSatsendring == antallFagsakerTilSatsendring) {
                return antallFagsakerSatsendring
            }
        }
        return antallFagsakerSatsendring
    }

    private fun skalTriggeFagsak(fagsak: Fagsak, satsTidspunkt: YearMonth, gyldigeSatstyper: List<SatsType>): Boolean {
        val aktivOgÅpenBehandling = behandlingRepository.findByFagsakAndAktivAndOpen(fagsakId = fagsak.id)
        if (aktivOgÅpenBehandling != null) {
            logger.info("Oppretter ikke satsendringtask for fagsak=${fagsak.id}. Har åpen behandling ${aktivOgÅpenBehandling.id}")
            return false
        }

        val sisteIverksatteBehandling = behandlingRepository.finnSisteIverksatteBehandling(fagsak.id)
        if (sisteIverksatteBehandling != null) {
            val andelerTilkjentYtelseMedEndreteUtbetalinger =
                andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                    sisteIverksatteBehandling.id
                )

            if (AutovedtakSatsendringService.harAlleredeSisteSats(
                    andelerTilkjentYtelseMedEndreteUtbetalinger,
                    satsTidspunkt
                )
            ) {
                satskjøringRepository.save(
                    Satskjøring(
                        fagsakId = fagsak.id,
                        ferdigTidspunkt = sisteIverksatteBehandling.endretTidspunkt
                    )
                )
                logger.info("Fagsak=${fagsak.id} har alt siste satser")
                return true
            }

            val satstyper = mutableListOf<SatsType>()
            if (harYtelsetype(
                    YtelseType.SMÅBARNSTILLEGG,
                    andelerTilkjentYtelseMedEndreteUtbetalinger,
                    satsTidspunkt
                )
            ) {
                satstyper.add(SatsType.SMA)
            }

            if (harYtelsetype(
                    YtelseType.UTVIDET_BARNETRYGD,
                    andelerTilkjentYtelseMedEndreteUtbetalinger,
                    satsTidspunkt
                )
            ) {
                satstyper.add(SatsType.UTVIDET_BARNETRYGD)
            }

            if (harYtelsetype(
                    YtelseType.ORDINÆR_BARNETRYGD,
                    andelerTilkjentYtelseMedEndreteUtbetalinger,
                    satsTidspunkt,
                    1054
                )

            ) {
                satstyper.add(SatsType.ORBA)
            }

            if (harYtelsetype(
                    YtelseType.ORDINÆR_BARNETRYGD,
                    andelerTilkjentYtelseMedEndreteUtbetalinger,
                    satsTidspunkt,
                    1676
                )

            ) {
                satstyper.add(SatsType.TILLEGG_ORBA)
            }

            return sjekkOgTriggSatsendring(satstyper, fagsak, gyldigeSatstyper, satsTidspunkt)
        } else {
            logger.info("Satsendring utføres ikke på fagsak=${fagsak.id} fordi fagsaken mangler en iverksatt behandling")
            return false
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprettSatsendringForIdent(ident: String) {
        val aktør = personidentService.hentAktør(ident)
        val løpendeFagsakerForAktør = fagsakRepository.finnFagsakerForAktør(aktør)
            .filter { !it.arkivert && it.status == FagsakStatus.LØPENDE }

        løpendeFagsakerForAktør.forEach { fagsak ->
            val sisteIverksatteBehandling = behandlingRepository.finnSisteIverksatteBehandling(fagsak.id)
            if (sisteIverksatteBehandling != null) {
                val andelerTilkjentYtelseMedEndreteUtbetalinger =
                    andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                        sisteIverksatteBehandling.id
                    )

                if (!AutovedtakSatsendringService.harAlleredeSisteSats(
                        andelerTilkjentYtelseMedEndreteUtbetalinger,
                        SATSENDRINGMÅNED_2023
                    )
                ) {
                    secureLogger.info("Oppretter satsendringtask for $ident og fagsakID=${fagsak.id}")
                    opprettSatsendringForFagsak(fagsakId = fagsak.id)
                }
            }
        }
    }

    fun opprettSatsendringForFagsak(fagsakId: Long) {
        satskjøringRepository.save(Satskjøring(fagsakId = fagsakId))
        opprettTaskService.opprettSatsendringTask(fagsakId, SATSENDRINGMÅNED_2023)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(StartSatsendring::class.java)
        const val PAGE_STØRRELSE = 1000
        val SATSENDRINGMÅNED_2023: YearMonth = YearMonth.of(2023, 3)
    }
}
