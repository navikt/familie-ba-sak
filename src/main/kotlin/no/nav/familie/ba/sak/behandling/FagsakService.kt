package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.Fagsak
import no.nav.familie.ba.sak.behandling.domene.FagsakRepository
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakBarnRepository
import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakRepository
import no.nav.familie.ba.sak.behandling.restDomene.*
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FagsakService(
        private val vedtakBarnRepository: VedtakBarnRepository,
        private val fagsakRepository: FagsakRepository,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val behandlingRepository: BehandlingRepository,
        private val vedtakRepository: VedtakRepository) {

    @Transactional
    fun hentRestFagsak(fagsakId: Long?): Ressurs<RestFagsak> {
        val fagsak = fagsakRepository.finnFagsak(fagsakId)
                     ?: return Ressurs.failure("Fant ikke fagsak med fagsakId: $fagsakId")

        val behandlinger = behandlingRepository.finnBehandlinger(fagsak.id)

        val restBehandlinger: List<RestBehandling> = behandlinger.map {
            val personopplysningGrunnlag = it.id?.let { it1 -> personopplysningGrunnlagRepository.findByBehandlingAndAktiv(it1) }
            val barnasFødselsnummer = personopplysningGrunnlag?.barna?.map { barn -> barn.personIdent.ident }

            val vedtakForBehandling = vedtakRepository.finnVedtakForBehandling(it.id).map { vedtak ->
                val barnBeregning = vedtakBarnRepository.finnBarnBeregningForVedtak(vedtak.id)
                vedtak.toRestVedtak(barnBeregning)
            }

            RestBehandling(
                    aktiv = it.aktiv,
                    behandlingId = it.id,
                    barnasFødselsnummer = barnasFødselsnummer,
                    vedtakForBehandling = vedtakForBehandling,
                    type = it.type,
                    status = it.status,
                    samletVilkårResultat = it.samletVilkårResultat?.toRestSamletVilkårResultat(),
                    kategori = it.kategori,
                    underkategori = it.underkategori
            )
        }

        return Ressurs.success(data = fagsak.toRestFagsak(restBehandlinger))
    }

    @Transactional
    fun hentFagsakForPersonident(personIdent: PersonIdent): Fagsak? {
        return fagsakRepository.finnFagsakForPersonIdent(personIdent)
    }

    @Transactional
    fun lagreFagsak(fagsak: Fagsak) {
        fagsakRepository.save(fagsak)
    }

    @Transactional
    fun hentLøpendeFagsaker(): List<Fagsak> {
        return fagsakRepository.finnLøpendeFagsaker()
    }
}
