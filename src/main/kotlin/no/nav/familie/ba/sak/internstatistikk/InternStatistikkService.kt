package no.nav.familie.ba.sak.internstatistikk

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRepository
import org.springframework.stereotype.Service

@Service
class InternStatistikkService(private val behandlingRepository: BehandlingRepository,
                              private val fagsakRepository: FagsakRepository) {
    fun finnAntallFagsakerTotalt() = fagsakRepository.finnAntallFagsakerTotalt()
    fun finnAntallFagsakerLøpende() = fagsakRepository.finnAntallFagsakerLøpende()
    fun finnAntallBehandlingerIkkeErAvsluttet() = behandlingRepository.finnAntallBehandlingerIkkeAvsluttet()
}