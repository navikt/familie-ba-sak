package no.nav.familie.ba.sak.behandling.fagsak

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.domene.vilkår.SamletVilkårResultatRepository
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakPersonRepository
import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.util.*

@SpringBootTest(properties = ["FAMILIE_INTEGRASJONER_API_URL=http://localhost:28085/api"])
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev")
@Tag("integration")
@AutoConfigureWireMock(port = 28085)
class FagsakServiceTest {
    @Autowired
    lateinit var vedtakRepository: VedtakRepository

    @Autowired
    lateinit var vedtakPersonRepository: VedtakPersonRepository

    @Autowired
    lateinit var fagsakRepository: FagsakRepository

    @Autowired
    lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    lateinit var personRepository: PersonRepository

    @Autowired
    lateinit var samletVilkårResultatRepository: SamletVilkårResultatRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @MockK
    lateinit var mockIntegrasjonClient: IntegrasjonClient

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var stegService: StegService

    @BeforeEach
    fun setUp(){
        stubFor(get(urlEqualTo("/api/personopplysning/v1/info"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(Ressurs.success(Personinfo(
                                LocalDate.of(2019,
                                        1,
                                        1)))))))
        stubFor(get(urlEqualTo("/api/personopplysning/v1/info/BAR"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(Ressurs.success(Personinfo(
                                LocalDate.of(2019,
                                        1,
                                        1)))))))
    }

    @Test
    fun `test å søke fagsak med fnr`() {
        val søker1Fnr = ClientMocks.søkerFnr[0]
        val søker2Fnr = ClientMocks.søkerFnr[1]
        val barn1Fnr = ClientMocks.barnFnr[0]
        val barn2fnr = ClientMocks.barnFnr[1]
        val fagsak0 = fagsakService.nyFagsak(NyFagsak(
                søker1Fnr
        ))

        val fagsak1 = fagsakService.nyFagsak(NyFagsak(
                søker2Fnr
        ))

        stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                søker1Fnr,
                listOf(barn1Fnr, barn2fnr),
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))

        stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                søker2Fnr,
                listOf(barn1Fnr),
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))

        val søkeresultat1 = fagsakService.søkeFagsak(PersonIdent(søker1Fnr))
        Assertions.assertEquals(søker1Fnr, søkeresultat1.personIdent.ident)
        Assertions.assertEquals(1, søkeresultat1.fagsaker.size)
        Assertions.assertEquals(Kjønn.KVINNE, søkeresultat1.kjønn)
        Assertions.assertEquals(fagsak0.data!!.id, søkeresultat1.fagsaker[0].fagsakId)

        val søkeresultat2 = fagsakService.søkeFagsak(PersonIdent(barn1Fnr))
        Assertions.assertEquals(barn1Fnr, søkeresultat2.personIdent.ident)
        Assertions.assertEquals(2, søkeresultat2.fagsaker.size)
        var matching = 0
        søkeresultat2.fagsaker.forEach {
            matching += if (it.fagsakId == fagsak0.data!!.id) 1 else if (it.fagsakId == fagsak1.data!!.id) 10 else 0
        }
        Assertions.assertEquals(11, matching)
        Assertions.assertEquals(Kjønn.KVINNE, søkeresultat2.kjønn)

        val søkeresultat3 = fagsakService.søkeFagsak(PersonIdent(barn2fnr))
        Assertions.assertEquals(barn2fnr, søkeresultat3.personIdent.ident)
        Assertions.assertEquals(1, søkeresultat3.fagsaker.size)
        Assertions.assertEquals(fagsak0.data!!.id, søkeresultat3.fagsaker[0].fagsakId)
        Assertions.assertEquals(Kjønn.MANN, søkeresultat3.kjønn)
    }
}