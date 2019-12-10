package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtak
import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtakRepository
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BehandlingslagerService @Autowired constructor(
        private val fagsakRepository: FagsakRepository,
        private val behandlingRepository: BehandlingRepository,
        private val behandlingVedtakRepository: BehandlingVedtakRepository
) {
    fun nyBehandling(fødselsnummer: String,
                     barnasFødselsnummer: Array<String>,
                     behandlingType: BehandlingType,
                     journalpostID: String,
                     saksnummer: String): Behandling {
        //final var søkerAktørId = oppslagTjeneste.hentAktørId(fødselsnummer);

        val fagsak = Fagsak(null, AktørId("1"), PersonIdent(fødselsnummer))
        fagsakRepository.save(fagsak)
        val behandling = Behandling( id = null, fagsak = fagsak, journalpostID = journalpostID, type = behandlingType, saksnummer = saksnummer)
        lagreBehandling(behandling)

        return behandling
    }

    fun hentBehandlingHvisEksisterer(fagsakId: Long?): Behandling? {
        return behandlingRepository.findByFagsakAndAktiv(fagsakId)
    }

    fun hentBehandlingVedtakHvisEksisterer(behandlingId: Long?): BehandlingVedtak? {
        return behandlingVedtakRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun hentBehandlinger(fagsakId: Long?): List<Behandling?> {
        return behandlingRepository.finnBehandlinger(fagsakId)
    }

    fun hentAlleBehandlinger(): MutableList<Behandling?> {
        return this.behandlingRepository.findAll()
    }

    fun lagreBehandling(behandling: Behandling) {
        val aktivBehandling = hentBehandlingHvisEksisterer(behandling.fagsak.id)

        if (aktivBehandling != null) {
            aktivBehandling.aktiv = false
            behandlingRepository.save(aktivBehandling)
        }

        behandlingRepository.save(behandling)
    }

    fun hentVedtakForBehandling(behandlingId: Long?): List<BehandlingVedtak?> {
        return behandlingVedtakRepository.finnVedtakForBehandling(behandlingId)
    }

    fun lagreBehandlingVedtak(behandlingVedtak: BehandlingVedtak) {
        val aktivBehandlingVedtak = hentBehandlingVedtakHvisEksisterer(behandlingVedtak.behandlingId)

        if (aktivBehandlingVedtak != null) {
            aktivBehandlingVedtak.aktiv = false
            behandlingVedtakRepository.save(aktivBehandlingVedtak)
        }

        behandlingVedtakRepository.save(behandlingVedtak)
    }
}