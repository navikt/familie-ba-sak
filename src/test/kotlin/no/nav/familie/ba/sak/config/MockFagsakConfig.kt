package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRequest
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.steg.RegistrerPersongrunnlagDTO
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.domene.FAMILIERELASJONSROLLE
import no.nav.familie.ba.sak.integrasjoner.domene.Familierelasjoner
import no.nav.familie.ba.sak.integrasjoner.domene.Personident
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDate
import javax.annotation.PostConstruct

@Component
@Profile("test-søk")
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

        behandlingService.oppdaterStatusPåBehandling(førsteBehandling.id, BehandlingStatus.FERDIGSTILT)
    }
}