package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class MigrerBehandlingerUtenInitialisertVedtak(
        private val behandlingRepository: BehandlingRepository,
        private val vedtakService: VedtakService,
) {

    @Scheduled(initialDelay = 1000, fixedDelay = Long.MAX_VALUE)
    @Transactional
    fun migrer() {
        logger.info("Migrerer behandlinger opprettet før behandling initierer vedtak")
        val behandlinger = behandlingRepository.findAll()

        var vellykkedeMigreringer = 0
        var mislykkedeMigreringer = 0
        behandlinger.filter { it.aktiv && it.status != BehandlingStatus.AVSLUTTET }.forEach { behandling ->
            val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)

            if (vedtak == null) {
                kotlin.runCatching {
                    vedtakService.initierVedtakForAktivBehandling(behandling)
                }.fold(
                        onSuccess = {
                            logger.info("Vellykket migrering for behandling ${behandling.id}")
                            vellykkedeMigreringer++
                        },
                        onFailure = {
                            logger.warn("Mislykket migrering for behandling ${behandling.id}")
                            secureLogger.warn("Mislykket migrering for behandling ${behandling.id}", it)
                            mislykkedeMigreringer++
                        }
                )
            } else if (behandling.opprettetTidspunkt.isBefore(LocalDateTime.of(2021, 3, 8, 0, 0, 0))) {
                logger.warn("Aktiv behandling ${behandling.id} ble opprettet før 8.mars og ble ikke migrert")
            } else {
                logger.info("Aktiv behandling ${behandling.id} ble opprettet etter 8.mars og ble ikke migrert")
            }
        }

        logger.info("Migrering av enhet fra vedtak til arbeidsfordeling på behandling ferdig.\n" +
                    "Antall behandlinger=${behandlinger.size}\n" +
                    "Vellykede migreringer=$vellykkedeMigreringer\n" +
                    "Mislykkede migreringer=$mislykkedeMigreringer\n")
    }

    companion object {

        val logger = LoggerFactory.getLogger(MigrerBehandlingerUtenInitialisertVedtak::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
