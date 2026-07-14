package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Sletter (nuller ut) gamle vedtaksbrev-pdf-er som ligger lagret i databasen på avsluttede
 * behandlinger. Brevene hentes nå fra Joark for avsluttede behandlinger, så vi trenger ikke
 * å lagre dem i vår egen database lenger. Se NAV-29936 / NAV-29382.
 */
@Component
class SlettGamleVedtaksbrevScheduler(
    private val vedtakRepository: VedtakRepository,
    private val featureToggleService: FeatureToggleService,
) {
    @Scheduled(cron = "0 0 6 1 */3 *")
    @Transactional
    fun slettGamleVedtaksbrev() {
        if (LeaderClient.isLeader() != true) return
        if (!featureToggleService.isEnabled(FeatureToggle.SKAL_SLETTE_GAMLE_VEDTAKSBREV_FRA_DB)) return

        val vedtaksdatoFør = LocalDateTime.now().minusMonths(ANTALL_MÅNEDER_FØR_SLETTING)

        // Hele jobben kjører i én transaksjon. Batchingen begrenser størrelsen på IN-lista i UPDATE-en
        // (og dermed antall parametere), ikke transaksjonsstørrelsen. Volumet er lavt siden jobben kun
        // kjører kvartalsvis, så vi holder det bevisst enkelt fremfor én transaksjon per batch.
        var totaltSlettet = 0
        for (batch in 0 until MAKS_ANTALL_BATCHER_PER_KJØRING) {
            val vedtakIder =
                vedtakRepository.finnVedtakIderMedStønadBrevPdf(
                    status = BehandlingStatus.AVSLUTTET,
                    vedtaksdatoFør = vedtaksdatoFør,
                    pageable = PageRequest.of(0, BATCH_STØRRELSE),
                )
            if (vedtakIder.isEmpty()) break

            totaltSlettet += vedtakRepository.slettStønadBrevPdfForVedtak(vedtakIder)
        }

        if (totaltSlettet > 0) {
            logger.info("Slettet $totaltSlettet gamle vedtaksbrev fra databasen")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SlettGamleVedtaksbrevScheduler::class.java)
        private const val ANTALL_MÅNEDER_FØR_SLETTING = 3L
        private const val BATCH_STØRRELSE = 500
        private const val MAKS_ANTALL_BATCHER_PER_KJØRING = 20
    }
}
