package no.nav.familie.ba.sak.behandling.fagsak

import io.mockk.every
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.steg.RegistrerPersongrunnlagDTO
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.util.*

@SpringBootTest
@ActiveProfiles("dev")
@Tag("integration")
class FagsakServiceTest(
        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val behandlingService: BehandlingService,

        @Autowired
        private val stegService: StegService,

        @Autowired
        private val integrasjonClient: IntegrasjonClient
) {

    @Test
    fun `test å søke fagsak med fnr`() {

        val søker1Fnr = UUID.randomUUID().toString()
        val søker2Fnr = UUID.randomUUID().toString()
        val barn1Fnr = UUID.randomUUID().toString()
        val barn2Fnr = UUID.randomUUID().toString()

        every {
            integrasjonClient.hentPersoninfoFor(eq(barn1Fnr))
        } returns Personinfo(fødselsdato = LocalDate.of(2018, 5, 1), kjønn = Kjønn.KVINNE, navn = "Jenta Barnesen")

        every {
            integrasjonClient.hentPersoninfoFor(eq(barn2Fnr))
        } returns Personinfo(fødselsdato = LocalDate.of(2019, 5, 1), kjønn = Kjønn.MANN, navn = "Gutten Barnesen")

        every {
            integrasjonClient.hentPersoninfoFor(eq(søker1Fnr))
        } returns Personinfo(fødselsdato = LocalDate.of(1990, 2, 19), kjønn = Kjønn.KVINNE, navn = "Mor Moresen")

        every {
            integrasjonClient.hentPersoninfoFor(eq(søker2Fnr))
        } returns Personinfo(fødselsdato = LocalDate.of(1991, 2, 20), kjønn = Kjønn.MANN, navn = "Far Faresen")

        val fagsak0 = fagsakService.nyFagsak(NyFagsak(
                søker1Fnr
        ))

        val fagsak1 = fagsakService.nyFagsak(NyFagsak(
                søker2Fnr
        ))

        val førsteBehandling = stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                søker1Fnr,
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))
        stegService.håndterPersongrunnlag(førsteBehandling,
                                          RegistrerPersongrunnlagDTO(ident = søker1Fnr, barnasIdenter = listOf(barn1Fnr)))

        behandlingService.oppdaterStatusPåBehandling(førsteBehandling.id, BehandlingStatus.FERDIGSTILT)

        val andreBehandling = stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                søker1Fnr,
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))
        stegService.håndterPersongrunnlag(andreBehandling,
                                          RegistrerPersongrunnlagDTO(ident = søker1Fnr, barnasIdenter = listOf(barn2Fnr)))



        val tredjeBehandling = stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                søker2Fnr,
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))
        stegService.håndterPersongrunnlag(tredjeBehandling,
                                          RegistrerPersongrunnlagDTO(ident = søker2Fnr, barnasIdenter = listOf(barn1Fnr)))

        val søkeresultat1 = fagsakService.hentFagsaker(søker1Fnr)
        Assertions.assertEquals(søker1Fnr, søkeresultat1.personIdent)
        Assertions.assertEquals(1, søkeresultat1.fagsaker.size)
        Assertions.assertEquals(Kjønn.KVINNE, søkeresultat1.kjønn)
        Assertions.assertEquals(fagsak0.data!!.id, søkeresultat1.fagsaker[0].fagsakId)

        val søkeresultat2 = fagsakService.hentFagsaker(barn1Fnr)
        Assertions.assertEquals(barn1Fnr, søkeresultat2.personIdent)
        Assertions.assertEquals(2, søkeresultat2.fagsaker.size)
        var matching = 0
        søkeresultat2.fagsaker.forEach {
            matching += if (it.fagsakId == fagsak0.data!!.id) 1 else if (it.fagsakId == fagsak1.data!!.id) 10 else 0
        }
        Assertions.assertEquals(11, matching)
        Assertions.assertEquals(Kjønn.KVINNE, søkeresultat2.kjønn)

        val søkeresultat3 = fagsakService.hentFagsaker(barn2Fnr)
        Assertions.assertEquals(barn2Fnr, søkeresultat3.personIdent)
        Assertions.assertEquals(1, søkeresultat3.fagsaker.size)
        Assertions.assertEquals(fagsak0.data!!.id, søkeresultat3.fagsaker[0].fagsakId)
        Assertions.assertEquals(Kjønn.MANN, søkeresultat3.kjønn)
    }
}