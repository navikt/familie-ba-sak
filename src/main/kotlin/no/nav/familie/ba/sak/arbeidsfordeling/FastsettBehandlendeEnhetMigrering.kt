package no.nav.familie.ba.sak.arbeidsfordeling

import no.nav.familie.ba.sak.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.client.Norg2RestClient
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
        val behandlinger = behandlingRepository.findAll()

        behandlinger.forEach {

            val arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandlingId = it.id)

            if (arbeidsfordelingPåBehandling == null) {
                val aktivVedtak = vedtakService.hentAktivForBehandling(behandlingId = it.id)

                val enhetNummerFraVedtak = aktivVedtak?.ansvarligEnhet
                val arbeidsfordelingsenhet = arbeidsfordelingService.hentArbeidsfordelingsenhet(it)

                arbeidsfordelingPåBehandlingRepository.save(
                        ArbeidsfordelingPåBehandling(
                                behandlingId = it.id,
                                behandlendeEnhetId = enhetNummerFraVedtak ?: arbeidsfordelingsenhet.enhetId,
                                behandlendeEnhetNavn = if (enhetNummerFraVedtak != null) norg2RestClient.hentEnhet(
                                        enhetNummerFraVedtak).navn else arbeidsfordelingsenhet.enhetNavn
                        )
                )
            }
        }
    }
}