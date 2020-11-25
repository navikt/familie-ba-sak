package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRequest
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.steg.RegistrerPersongrunnlagDTO
import no.nav.familie.ba.sak.behandling.steg.StegService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
@Profile("mock-pdl-test-søk")
class MockFagsakConfig (
        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val behandlingService: BehandlingService,

        @Autowired
        private val stegService: StegService
){
    @PostConstruct
    fun createMockFagsak(){
        fagsakService.hentEllerOpprettFagsak(FagsakRequest(
                "12345678910"
        ))

        val førsteBehandling = stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                "12345678910",
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))
        stegService.håndterPersongrunnlag(førsteBehandling,
                RegistrerPersongrunnlagDTO(ident = "12345678910", barnasIdenter = listOf("31245678910")))

        behandlingService.oppdaterStatusPåBehandling(førsteBehandling.id, BehandlingStatus.AVSLUTTET)
    }
}