package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.Fagsak
import no.nav.familie.ba.sak.behandling.domene.FagsakRepository
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtakRepository
import no.nav.familie.ba.sak.behandling.restDomene.RestBehandling
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.toRestBehandlingVedtak
import no.nav.familie.ba.sak.behandling.restDomene.toRestFagsak
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.stereotype.Service

@Service
class FagsakService(
        private val behandlingslagerService: BehandlingslagerService,
        private val fagsakRepository: FagsakRepository,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val behandlingVedtakRepository: BehandlingVedtakRepository) {

    fun hentRestFagsak(fagsakId: Long?): Ressurs<RestFagsak> {
        val fagsak = fagsakRepository.finnFagsak(fagsakId)
                ?: return Ressurs.failure("Fant ikke fagsak med fagsakId: $fagsakId")

        val behandlinger = behandlingslagerService.hentBehandlinger(fagsak.id)

        val restBehandlinger: List<RestBehandling> = behandlinger.map {
            val personopplysningGrunnlag = it?.id?.let { it1 -> personopplysningGrunnlagRepository.findByBehandlingAndAktiv(it1) }
            val barnasFødselsnummer = personopplysningGrunnlag?.barna?.map { barn -> barn.personIdent?.ident }

            val vedtakForBehandling = behandlingslagerService.hentVedtakForBehandling(it?.id).map { behandlingVedtak ->
                val barnBeregning = behandlingslagerService.hentBarnBeregningForVedtak(behandlingVedtak?.id)
                behandlingVedtak?.toRestBehandlingVedtak(barnBeregning)
            }

            RestBehandling(
                    aktiv = it?.aktiv ?: false,
                    behandlingId = it?.id,
                    barnasFødselsnummer = barnasFødselsnummer,
                    vedtakForBehandling = vedtakForBehandling
            )
        }

        return Ressurs.success(data = fagsak.toRestFagsak(restBehandlinger))
    }

    fun hentFagsakForPersonident(personIdent: PersonIdent): Fagsak? {
        return fagsakRepository.finnFagsakForPersonIdent(personIdent)
    }

    fun lagreFagsak(fagsak: Fagsak) {
        fagsakRepository.save(fagsak)
    }
}