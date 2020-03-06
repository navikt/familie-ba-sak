package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.steg.initSteg
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.økonomi.OppdragId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BehandlingService(private val behandlingRepository: BehandlingRepository,
                        private val persongrunnlagService: PersongrunnlagService,
                        private val fagsakService: FagsakService) {

    @Transactional
    fun opprettBehandling(nyBehandling: NyBehandling): Behandling {
        val fagsak = fagsakService.hent(personIdent = PersonIdent(nyBehandling.søkersIdent))
                     ?: error("Kan ikke lage behandling på person uten tilknyttet fagsak")

        val aktivBehandling = hentAktivForFagsak(fagsak.id)

        if (aktivBehandling == null || aktivBehandling.status == BehandlingStatus.FERDIGSTILT) {
            val behandling =
                    Behandling(fagsak = fagsak,
                               journalpostID = nyBehandling.journalpostID,
                               type = nyBehandling.behandlingType,
                               kategori = nyBehandling.kategori,
                               underkategori = nyBehandling.underkategori,
                               steg = initSteg)
            return lagreNyOgDeaktiverGammelBehandling(behandling)
        } else {
            error("Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt.")
        }
    }

    fun settVilkårsvurdering(behandling: Behandling, resultat: BehandlingResultat, begrunnelse: String): Behandling {
        behandling.begrunnelse = begrunnelse
        behandling.resultat = resultat
        return lagre(behandling)
    }

    fun hentAktivForFagsak(fagsakId: Long): Behandling? {
        return behandlingRepository.findByFagsakAndAktiv(fagsakId)
    }

    fun hent(behandlingId: Long): Behandling {
        return behandlingRepository.finnBehandling(behandlingId)
    }

    fun hentAktiveBehandlingerForLøpendeFagsaker(): List<OppdragId> {
        return fagsakService.hentLøpendeFagsaker()
                .mapNotNull { fagsak -> hentAktivForFagsak(fagsak.id) }
                .map { behandling ->
                    OppdragId(
                            persongrunnlagService.hentSøker(behandling)!!.personIdent.ident,
                            behandling.id)
                }
    }

    fun hentBehandlinger(fagsakId: Long): List<Behandling> {
        return behandlingRepository.finnBehandlinger(fagsakId)
    }

    fun lagre(behandling: Behandling): Behandling {
        return behandlingRepository.save(behandling)
    }

    fun lagreNyOgDeaktiverGammelBehandling(behandling: Behandling): Behandling {
        val aktivBehandling = hentAktivForFagsak(behandling.fagsak.id)

        if (aktivBehandling != null) {
            behandlingRepository.saveAndFlush(aktivBehandling.also { it.aktiv = false })
        }

        LOG.info("${SikkerhetContext.hentSaksbehandler()} oppretter behandling $behandling")
        return behandlingRepository.save(behandling)
    }

    fun sendBehandlingTilBeslutter(behandling: Behandling) {
        oppdaterStatusPåBehandling(behandlingId = behandling.id, status = BehandlingStatus.SENDT_TIL_BESLUTTER)
    }

    fun oppdaterStatusPåBehandling(behandlingId: Long, status: BehandlingStatus) {
        val behandling = hent(behandlingId)
        LOG.info("${SikkerhetContext.hentSaksbehandler()} endrer status på behandling $behandlingId fra ${behandling.status} til $status")

        behandling.status = status
        behandlingRepository.save(behandling)
    }

    fun oppdaterStegPåBehandling(behandlingId: Long, steg: StegType) {
        val behandling = hent(behandlingId)
        LOG.info("${SikkerhetContext.hentSaksbehandler()} endrer steg på behandling $behandlingId fra ${behandling.steg} til $steg")

        behandling.steg = steg
        behandlingRepository.save(behandling)
    }

    companion object {
        val LOG: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
    }
}
