package no.nav.familie.ba.sak.arbeidsfordeling

import no.nav.familie.ba.sak.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.client.Norg2RestClient
import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FastsettBehandlendeEnhetMigrering(
        private val behandlingRepository: BehandlingRepository,
        private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
        private val norg2RestClient: Norg2RestClient,
        private val vedtakService: VedtakService,
        private val arbeidsfordelingService: ArbeidsfordelingService
) {

    @Scheduled(initialDelay = 1000, fixedDelay = Long.MAX_VALUE)
    private fun migrer() {
        logger.info("Migrerer enhet fra vedtak til arbeidsfordeling på behandling")
        if (LeaderClient.isLeader() == true) {
            val behandlinger = behandlingRepository.findAll()

            var vellykkedeMigreringer = 0
            var mislykkedeMigreringer = 0
            behandlinger.forEach { behandling ->

                val arbeidsfordelingPåBehandling =
                        arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandlingId = behandling.id)

                if (arbeidsfordelingPåBehandling == null) {
                    kotlin.runCatching {
                        val aktivVedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)

                        val enhetNummerFraVedtak = aktivVedtak?.ansvarligEnhet
                        val arbeidsfordelingsenhet = arbeidsfordelingService.hentArbeidsfordelingsenhet(behandling)

                        arbeidsfordelingPåBehandlingRepository.save(
                                ArbeidsfordelingPåBehandling(
                                        behandlingId = behandling.id,
                                        behandlendeEnhetId = enhetNummerFraVedtak ?: arbeidsfordelingsenhet.enhetId,
                                        behandlendeEnhetNavn = if (enhetNummerFraVedtak != null) norg2RestClient.hentEnhet(
                                                enhetNummerFraVedtak).navn else arbeidsfordelingsenhet.enhetNavn
                                )
                        )
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
                } else {
                    vellykkedeMigreringer++
                }
            }

            logger.info("Migrering av enhet fra vedtak til arbeidsfordeling på behandling ferdig.\n" +
                        "Antall behandlinger=${behandlinger.size}\n" +
                        "Vellykede migreringer=$vellykkedeMigreringer\n" +
                        "Mislykkede migreringer=$mislykkedeMigreringer\n")
        } else {
            logger.info("Leader election er ikke satt opp, skipper migrering.")
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(this::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}