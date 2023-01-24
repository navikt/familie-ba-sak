package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.Satskjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.task.OpprettTaskService
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
    private val featureToggleService: FeatureToggleService

) {

    @Transactional
    fun startSatsendring(satsTidspunkt: YearMonth = YearMonth.of(2023, 3)) {
        val gyldigeSatser = hentGyldigeSatser()
        fagsakRepository.finnLøpendeFagsakerSatsendring(Pageable.ofSize(BOLK_STØRRELSE_SATSENDRING)).forEach {
            val sisteIverksatteBehandling = behandlingRepository.finnSisteIverksatteBehandling(it.id)

            if (sisteIverksatteBehandling != null) {
                val andelerTilkjentYtelseMedEndreteUtbetalinger =
                    andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                        sisteIverksatteBehandling.id
                    )

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

                sjekkOgTriggSatsendring(satstyper, sisteIverksatteBehandling, gyldigeSatser)
            } else {
                logger.info("Satsendring utføres ikke på fagsak=${it.id} fordi fagsaken mangler en iverksatt behandling")
            }
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
                )
            }
        } else {
            andelerTilkjentYtelseMedEndreteUtbetalinger.any {
                it.type == ytelseType && it.sats == sats && it.stønadFom.isBefore(tidspunkt) && it.stønadTom.isSameOrAfter(
                    tidspunkt
                )
            }
        }
    }

    private fun sjekkOgTriggSatsendring(
        satstyper: List<SatsType>,
        behandling: Behandling,
        gyldigeSatser: List<SatsType>
    ) {
        if (gyldigeSatser.containsAll(satstyper)) {
            if (featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_OPPRETT_TASKER)) {
                logger.info("Oppretter satsendringtask for fagsak=${behandling.id}")
                opprettTaskService.opprettSatsendringTask(behandling.id)
                satskjøringRepository.save(Satskjøring(fagsakId = behandling.fagsak.id))
            } else {
                logger.info("Oppretter ikke satsendringtask for fagsak=${behandling.id}. Toggle avskrudd.")
            }
        }
    }

    private fun hentGyldigeSatser(): MutableList<SatsType> {
        val gyldigeSatser = mutableListOf<SatsType>()
        if (featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_TILLEGG_ORBA, true)) {
            gyldigeSatser.add(SatsType.TILLEGG_ORBA)
        }

        if (featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_ORBA, false)) {
            gyldigeSatser.add(SatsType.ORBA)
        }

        if (featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_UTVIDET, false)) {
            gyldigeSatser.add(SatsType.UTVIDET_BARNETRYGD)
        }

        if (featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_SMA, false)) {
            gyldigeSatser.add(SatsType.SMA)
        }

        logger.info("Påskrudde satstyper for satskjøring $gyldigeSatser")
        return gyldigeSatser
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(StartSatsendring::class.java)
        const val BOLK_STØRRELSE_SATSENDRING = 100
    }
}
