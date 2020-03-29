package no.nav.familie.ba.sak.behandling.vedtak

import com.github.tomakehurst.wiremock.client.WireMock
import io.mockk.MockKAnnotations
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.beregning.*
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@SpringBootTest(properties = ["FAMILIE_INTEGRASJONER_API_URL=http://localhost:28085/api"])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen", "mock-oauth")
@Tag("integration")
@AutoConfigureWireMock(port = 28085)
@Deprecated("Denne testen skal slettes når alt knyttet til VedtakPerson er slettet i DB og kode")
internal class AndelTilkjentYtelseMigreringTest {
    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vedtakPersonRepository: VedtakPersonRepository

    @Autowired
    lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @Autowired
    lateinit var vedtakService: VedtakService

    @Autowired
    lateinit var persongrunnlagService: PersongrunnlagService

    @Autowired
    lateinit var beregningService: BeregningService

    @Autowired
    lateinit var fagsakService: FagsakService

    lateinit var behandlingService: BehandlingService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        behandlingService = BehandlingService(
                behandlingRepository,
                persongrunnlagService,
                beregningService,
                fagsakService)

        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/aktoer/v1"))
                                 .willReturn(WireMock.aResponse()
                                                     .withHeader("Content-Type", "application/json")
                                                     .withBody(objectMapper.writeValueAsString(Ressurs.success(mapOf("aktørId" to "1"))))))
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/personopplysning/v1/info"))
                                 .willReturn(WireMock.aResponse()
                                                     .withHeader("Content-Type", "application/json")
                                                     .withBody(objectMapper.writeValueAsString(Ressurs.success(Personinfo(
                                                             LocalDate.of(2019,
                                                                          1,
                                                                          1)))))))
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/personopplysning/v1/info/BAR"))
                                 .willReturn(WireMock.aResponse()
                                                     .withHeader("Content-Type", "application/json")
                                                     .withBody(objectMapper.writeValueAsString(Ressurs.success(Personinfo(
                                                             LocalDate.of(2019,
                                                                          1,
                                                                          1)))))))
    }

    val søkerFnr = randomFnr()
    val barn1Fnr = randomFnr()
    val barn2Fnr = randomFnr()
    val barn3Fnr = randomFnr()

    val dato_2020_01_01 = LocalDate.of(2020, 1, 1)
    val dato_2020_10_01 = LocalDate.of(2020, 10, 1)
    val dato_2021_01_01 = LocalDate.of(2021, 1, 1)
    val dato_2021_10_01 = LocalDate.of(2021, 10, 1)

    @Test
    fun `test at insert, update og delete i VedtakPerson migreres til AndelTilkjentYtelse`() {


        val fagsak = fagsakService.lagre(Fagsak(0, AktørId("1"),
                                                PersonIdent(søkerFnr), FagsakStatus.OPPRETTET) )
        val behandling1 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling1.id, søkerFnr, listOf(barn1Fnr, barn2Fnr, barn3Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vedtak = vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling1,
                personopplysningGrunnlag = personopplysningGrunnlag,
                ansvarligSaksbehandler = "saksbehandler1")

        val førsteBeregning = NyBeregning(listOf(
                PersonBeregning(barn1Fnr, 1054, dato_2020_01_01, Ytelsetype.ORDINÆR_BARNETRYGD),
                PersonBeregning(barn2Fnr, 1354, dato_2020_10_01, Ytelsetype.ORDINÆR_BARNETRYGD)
        ))

        val andreBeregning = NyBeregning(listOf(
                PersonBeregning(barn1Fnr, 970, dato_2021_01_01, Ytelsetype.MANUELL_VURDERING),
                PersonBeregning(barn3Fnr, 314, dato_2021_10_01, Ytelsetype.EØS)
        ))

        val vedtakPersoner = mapNyBeregningTilVedtakPerson(vedtak.id,førsteBeregning,personopplysningGrunnlag)
        val lagredeVedtakPerson = vedtakPersonRepository.saveAll(vedtakPersoner)

        Assertions.assertEquals(2,vedtakPersonRepository.finnPersonBeregningForVedtak(vedtak.id).size)

        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val andelTilkjentYtelse = mapNyBeregningTilAndelTilkjentYtelse(behandling2.id, andreBeregning, personopplysningGrunnlag)
        andelTilkjentYtelseRepository.saveAll(andelTilkjentYtelse)

        val andelTilkjentYtelseForBeregning1 =
                andelTilkjentYtelseRepository.finnAndelTilkjentYtelseForBeregning(behandling1.id)
                        .sortedBy { it.beløp }
        Assertions.assertEquals(2, andelTilkjentYtelseForBeregning1.size)
        Assertions.assertEquals(1054, andelTilkjentYtelseForBeregning1[0].beløp)
        Assertions.assertEquals(1354, andelTilkjentYtelseForBeregning1[1].beløp)

        val andelTilkjentYtelseForBeregning2 =
                andelTilkjentYtelseRepository.finnAndelTilkjentYtelseForBeregning(behandling2.id)
                        .sortedBy { it.beløp }
        Assertions.assertEquals(2, andelTilkjentYtelseForBeregning2.size)
        Assertions.assertEquals(314, andelTilkjentYtelseForBeregning2[0].beløp)
        Assertions.assertEquals(970, andelTilkjentYtelseForBeregning2[1].beløp)

        // Update-migrering
        lagredeVedtakPerson
                .map { it.copy(
                        beløp = it.beløp+100,
                        stønadFom = it.stønadFom.plusMonths(1),
                        stønadTom = it.stønadTom.plusMonths(1),
                        type = Ytelsetype.MANUELL_VURDERING) }
                .forEach {vedtakPersonRepository.save(it) }

        val andelTilkjentYtelseForBeregning3 =
                andelTilkjentYtelseRepository.finnAndelTilkjentYtelseForBeregning(behandling1.id)
                        .sortedBy { it.beløp }
        Assertions.assertEquals(2, andelTilkjentYtelseForBeregning3.size)
        Assertions.assertEquals(1154, andelTilkjentYtelseForBeregning3[0].beløp)
        Assertions.assertEquals(dato_2020_01_01.plusMonths(1), andelTilkjentYtelseForBeregning3[0].stønadFom)
        Assertions.assertEquals(Ytelsetype.MANUELL_VURDERING, andelTilkjentYtelseForBeregning3[0].type)

        Assertions.assertEquals(1454, andelTilkjentYtelseForBeregning3[1].beløp)
        Assertions.assertEquals(dato_2020_10_01.plusMonths(1), andelTilkjentYtelseForBeregning3[1].stønadFom)
        Assertions.assertEquals(Ytelsetype.MANUELL_VURDERING, andelTilkjentYtelseForBeregning3[1].type)

        // Slett-migrering
        vedtakService.slettAlleBeregninger(vedtak)
        Assertions.assertEquals(0, andelTilkjentYtelseRepository.finnAndelTilkjentYtelseForBeregning(behandling1.id).size)

    }

}