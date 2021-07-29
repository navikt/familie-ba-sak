package no.nav.familie.ba.sak.kjerne.fødselshendelse.gdpr

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.filtreringsregler.Fakta
import no.nav.familie.ba.sak.kjerne.fødselshendelse.gdpr.domene.FødelshendelsePreLanseringRepository
import no.nav.familie.ba.sak.kjerne.fødselshendelse.gdpr.domene.FødselshendelsePreLansering
import no.nav.familie.ba.sak.kjerne.fødselshendelse.gdpr.domene.toJson
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Evaluering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.FaktaTilVilkårsvurdering
import org.springframework.stereotype.Service

@Service
class GDPRService(
        private val fødelshendelsePreLanseringRepository: FødelshendelsePreLanseringRepository,
        private val behandlingService: BehandlingService
) {

    fun lagreResultatAvFiltreringsregler(faktaForFiltreringsregler: String,
                                         evalueringAvFiltrering: String,
                                         nyBehandling: NyBehandlingHendelse,
                                         behandlingId: Long) {
        val fødselshendelsePreLansering = FødselshendelsePreLansering(
                personIdent = nyBehandling.morsIdent,
                behandlingId = behandlingId,
                nyBehandlingHendelse = nyBehandling.toJson(),
                filtreringsreglerInput = faktaForFiltreringsregler,
                filtreringsreglerOutput = evalueringAvFiltrering,
        )
        fødelshendelsePreLanseringRepository.saveAndFlush(fødselshendelsePreLansering)
    }

    fun oppdaterFødselshendelsePreLanseringMedVilkårsvurderingForPerson(behandlingId: Long,
                                                                        faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering,
                                                                        evaluering: Evaluering) {
        val fødselshendelsePreLansering = fødelshendelsePreLanseringRepository.finnFødselshendelsePreLansering(behandlingId)
                                          ?: FødselshendelsePreLansering(
                                                  behandlingId = behandlingId,
                                                  personIdent = behandlingService.hent(behandlingId).fagsak.hentAktivIdent().ident
                                          )

        fødselshendelsePreLansering.leggTilVurderingForPerson(faktaTilVilkårsvurdering, evaluering)

        fødelshendelsePreLanseringRepository.saveAndFlush(fødselshendelsePreLansering)
    }

    fun hentFødselshendelsePreLansering(behandlingId: Long): FødselshendelsePreLansering? {
        return fødelshendelsePreLanseringRepository.finnFødselshendelsePreLansering(behandlingId)
    }
}
