package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.Satskjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.SatsendringTaskDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class StartSatsendring(
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val opprettTaskService: OpprettTaskService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val satskjøringRepository: SatskjøringRepository,
    private val featureToggleService: FeatureToggleService,
    private val personidentService: PersonidentService,
    private val autovedtakSatsendringService: AutovedtakSatsendringService

) {

    @Transactional
    fun startSatsendring(
        antallFagsaker: Int,
        satsTidspunkt: YearMonth = YearMonth.of(2023, 3)
    ) {
        if (!featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_ENABLET, false)) {
            logger.info("Skipper satsendring da toggle er skrudd av.")
            return
        }
        var antallSatsendringerStartet = 0
        var startSide = 0
        while (antallSatsendringerStartet < antallFagsaker) {
            val page =
                fagsakRepository.finnLøpendeFagsakerForSatsendring(Pageable.ofSize(antallFagsaker).withPage(startSide))

            val fagsakerForSatsendring = page.toList()
            logger.info("Fant ${fagsakerForSatsendring.size} personer for satsendring på side $startSide")
            if (fagsakerForSatsendring.isNotEmpty()) {
                antallSatsendringerStartet =
                    oppretteEllerSkipSatsendring(
                        fagsakerForSatsendring,
                        antallSatsendringerStartet,
                        antallFagsaker,
                        satsTidspunkt
                    )
            }

            if (++startSide >= page.totalPages) break
        }
    }

    private fun oppretteEllerSkipSatsendring(
        fagsakForSatsendring: List<Fagsak>,
        antallAlleredeTriggetSatsendring: Int,
        antallFagsakerTilSatsendring: Int,
        satsTidspunkt: YearMonth
    ): Int {
        var antallFagsakerSatsendring = antallAlleredeTriggetSatsendring
        for (fagsak in fagsakForSatsendring) {
            if (skalTriggeFagsak(fagsak, satsTidspunkt)) {
                antallFagsakerSatsendring++
            }

            if (antallFagsakerSatsendring == antallFagsakerTilSatsendring) {
                return antallFagsakerSatsendring
            }
        }
        return antallFagsakerSatsendring
    }

    private fun skalTriggeFagsak(fagsak: Fagsak, satsTidspunkt: YearMonth): Boolean {
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

            if (featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_OPPRETT_TASKER)) {
                logger.info("Oppretter satsendringtask for fagsak=${fagsak.id}")
                opprettTaskService.opprettSatsendringTask(fagsak.id, satsTidspunkt)
            } else {
                logger.info("Oppretter ikke satsendringtask for fagsak=${fagsak.id}. Toggle SATSENDRING_OPPRETT_TASKER avskrudd.")
            }
            return true
        } else {
            logger.info("Satsendring utføres ikke på fagsak=${fagsak.id} fordi fagsaken mangler en iverksatt behandling")
            return false
        }
    }

    fun sjekkOgOpprettSatsendringVedGammelSats(ident: String): Boolean {
        val aktør = personidentService.hentAktør(ident)
        val løpendeFagsakerForAktør = fagsakRepository.finnFagsakerForAktør(aktør)
            .filter { !it.arkivert && it.status == FagsakStatus.LØPENDE }

        var harOpprettetSatsendring = false
        løpendeFagsakerForAktør.forEach { fagsak ->
            if (opprettSatsendringTaskVedGammelSats(fagsak.id)) {
                harOpprettetSatsendring = true
            }
        }
        return harOpprettetSatsendring
    }

    fun sjekkOgOpprettSatsendringVedGammelSats(fagsakId: Long): Boolean {
        return opprettSatsendringTaskVedGammelSats(fagsakId)
    }

    private fun opprettSatsendringTaskVedGammelSats(fagsakId: Long): Boolean =
        if (kanStarteSatsendringPåFagsak(fagsakId)) {
            logger.info("Oppretter satsendringtask fagsakID=$fagsakId")
            opprettSatsendringForFagsak(fagsakId = fagsakId)
            true
        } else {
            false
        }

    fun kanStarteSatsendringPåFagsak(fagsakId: Long): Boolean {
        val sisteIverksatteBehandling = behandlingRepository.finnSisteIverksatteBehandling(fagsakId)

        return if (sisteIverksatteBehandling == null) {
            false
        } else if (satskjøringRepository.findByFagsakId(fagsakId) != null) {
            false
        } else {
            val andelerTilkjentYtelseMedEndreteUtbetalinger =
                andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                    sisteIverksatteBehandling.id
                )

            val harSisteSats = AutovedtakSatsendringService.harAlleredeSisteSats(
                aty = andelerTilkjentYtelseMedEndreteUtbetalinger,
                satstidspunkt = SATSENDRINGMÅNED_2023
            )

            !harSisteSats
        }
    }

    @Transactional
    fun opprettSatsendringSynkrontVedGammelSats(fagsakId: Long): Boolean {
        val aktivOgÅpenBehandling =
            behandlingRepository.findByFagsakAndAktivAndOpen(fagsakId = fagsakId)

        if (aktivOgÅpenBehandling != null) {
            throw FunksjonellFeil("Det finnes en åpen behandling på fagsaken som må avsluttes før satsendring kan gjennomføres.")
        }

        return if (kanStarteSatsendringPåFagsak(fagsakId)) {
            satskjøringRepository.save(Satskjøring(fagsakId = fagsakId))
            val resultattekstSatsendringBehandling = autovedtakSatsendringService.kjørBehandling(
                SatsendringTaskDto(
                    fagsakId = fagsakId,
                    satstidspunkt = SATSENDRINGMÅNED_2023
                )
            )
            if (resultattekstSatsendringBehandling == "Satsendring kjørt OK") {
                true
            } else throw Feil("Satsendring kjørte ikke OK for fagsak $fagsakId")
        } else {
            false
        }
    }

    fun opprettSatsendringForFagsak(fagsakId: Long) {
        opprettTaskService.opprettSatsendringTask(fagsakId, SATSENDRINGMÅNED_2023)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(StartSatsendring::class.java)
        val SATSENDRINGMÅNED_2023: YearMonth = YearMonth.of(2023, 3)
    }
}
