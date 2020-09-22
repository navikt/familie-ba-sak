package no.nav.familie.ba.sak.gdpr

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler.Fakta
import no.nav.familie.ba.sak.behandling.vilkår.FaktaTilVilkårsvurdering
import no.nav.familie.ba.sak.behandling.vilkår.toJson
import no.nav.familie.ba.sak.gdpr.domene.FødelshendelsePreLanseringRepository
import no.nav.familie.ba.sak.gdpr.domene.FødselshendelsePreLansering
import no.nav.familie.ba.sak.gdpr.domene.toJson
import no.nav.nare.core.evaluations.Evaluering
import org.springframework.stereotype.Service

@Service
class GDPRService(
        private val fødelshendelsePreLanseringRepository: FødelshendelsePreLanseringRepository,
        private val behandlingService: BehandlingService
) {

    fun lagreResultatAvFiltreringsregler(faktaForFiltreringsregler: Fakta,
                                         evalueringAvFiltrering: Evaluering,
                                         nyBehandling: NyBehandlingHendelse,
                                         behandlingId: Long) {
        val fødselshendelsePreLansering = FødselshendelsePreLansering(
                personIdent = nyBehandling.morsIdent ?: error("Kunne ikke finne mors ident på behandling fra fødselshendelse"),
                behandlingId = behandlingId,
                nyBehandlingHendelse = nyBehandling.toJson(),
                filtreringsreglerInput = faktaForFiltreringsregler.toJson(),
                filtreringsreglerOutput = evalueringAvFiltrering.toJson(),
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
