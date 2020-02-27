package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.Fagsak
import no.nav.familie.ba.sak.behandling.domene.FagsakRepository
import no.nav.familie.ba.sak.behandling.domene.FagsakStatus
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakPersonRepository
import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakRepository
import no.nav.familie.ba.sak.behandling.domene.vilkår.SamletVilkårResultatRepository
import no.nav.familie.ba.sak.behandling.restDomene.*
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FagsakService(
        private val vedtakPersonRepository: VedtakPersonRepository,
        private val fagsakRepository: FagsakRepository,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val samletVilkårResultatRepository: SamletVilkårResultatRepository,
        private val behandlingRepository: BehandlingRepository,
        private val vedtakRepository: VedtakRepository,
        private val integrasjonTjeneste: IntegrasjonTjeneste) {

    @Transactional
    fun nyFagsak(nyFagsak: NyFagsak): Ressurs<RestFagsak> {
        return when (val hentetFagsak = fagsakRepository.finnFagsakForPersonIdent(personIdent = PersonIdent(nyFagsak.personIdent))) {
            null -> {
                val fagsak = Fagsak(
                        aktørId = integrasjonTjeneste.hentAktørId(nyFagsak.personIdent),
                        personIdent = PersonIdent(nyFagsak.personIdent)
                )
                lagreFagsak(fagsak)

                hentRestFagsak(fagsakId = fagsak.id)
            }
            else -> Ressurs.failure(
                    "Kan ikke opprette fagsak på person som allerede finnes. Gå til fagsak ${hentetFagsak.id} for å se på saken")
        }
    }

    @Transactional
    fun hentRestFagsak(fagsakId: Long?): Ressurs<RestFagsak> {
        val fagsak = fagsakRepository.finnFagsak(fagsakId)
                     ?: return Ressurs.failure("Fant ikke fagsak med fagsakId: $fagsakId")

        val behandlinger = behandlingRepository.finnBehandlinger(fagsak.id)

        val restBehandlinger: List<RestBehandling> = behandlinger.map { it ->
            val personopplysningGrunnlag = it.id.let { it1 -> personopplysningGrunnlagRepository.findByBehandlingAndAktiv(it1) }
                                           ?: return Ressurs.failure("Fant ikke personopplysningsgrunnlag på behandling")

            val vedtakForBehandling = vedtakRepository.finnVedtakForBehandling(it.id).map { vedtak ->
                val personBeregning = vedtakPersonRepository.finnPersonBeregningForVedtak(vedtak.id)
                vedtak.toRestVedtak(personBeregning)
            }

            RestBehandling(
                    aktiv = it.aktiv,
                    behandlingId = it.id,
                    vedtakForBehandling = vedtakForBehandling,
                    personer = personopplysningGrunnlag.personer.map { it.toRestPerson() },
                    type = it.type,
                    status = it.status,
                    samletVilkårResultat = samletVilkårResultatRepository.finnSamletVilkårResultatPåBehandlingOgAktiv(it.id)?.toRestSamletVilkårResultat(),
                    opprettetTidspunkt = it.opprettetTidspunkt,
                    kategori = it.kategori,
                    underkategori = it.underkategori
            )
        }

        return Ressurs.success(data = fagsak.toRestFagsak(restBehandlinger))
    }

    fun oppdaterStatus(fagsak: Fagsak, nyStatus: FagsakStatus) {
        LOG.info("Endrer status på fagsak ${fagsak.id} fra ${fagsak.status} til $nyStatus")
        fagsak.status = nyStatus

        lagreFagsak(fagsak)
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

    companion object {
        val LOG = LoggerFactory.getLogger(FagsakService::class.java)
    }
}
