package no.nav.familie.ba.sak.behandling

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.mockk.*
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.fagsak.FagsakPersonRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRequest
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.*
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultat
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatRepository
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.task.OpphørVedtakTask
import no.nav.familie.ba.sak.task.OpphørVedtakTask.Companion.opprettOpphørVedtakTask
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personinfo.Bostedsadresse
import no.nav.familie.kontrakter.felles.personinfo.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personinfo.UkjentBosted
import no.nav.familie.kontrakter.felles.personinfo.Vegadresse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.nare.core.evaluations.Resultat
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
import javax.transaction.Transactional


@SpringBootTest(properties = ["FAMILIE_INTEGRASJONER_API_URL=http://localhost:28085/api"])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen", "mock-oauth")
@Tag("integration")
@AutoConfigureWireMock(port = 28085)
class BehandlingIntegrationTest {

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var personRepository: PersonRepository

    @Autowired
    lateinit var vedtakRepository: VedtakRepository

    @Autowired
    lateinit var vedtakService: VedtakService

    @Autowired
    lateinit var persongrunnlagService: PersongrunnlagService

    @Autowired
    lateinit var beregningService: BeregningService

    @Autowired
    lateinit var behandlingResultatRepository: BehandlingResultatRepository

    @Autowired
    lateinit var behandlingResultatService: BehandlingResultatService

    @Autowired
    lateinit var fagsakPersonRepository: FagsakPersonRepository

    @Autowired
    lateinit var totrinnskontrollService: TotrinnskontrollService

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var integrasjonClient: IntegrasjonClient

    @Autowired
    lateinit var loggService: LoggService

    lateinit var behandlingService: BehandlingService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        behandlingService = BehandlingService(
                behandlingRepository,
                fagsakPersonRepository,
                persongrunnlagService,
                beregningService,
                fagsakService,
                loggService)

        stubFor(get(urlEqualTo("/api/aktoer/v1"))
                        .willReturn(aResponse()
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(objectMapper.writeValueAsString(Ressurs.success(mapOf("aktørId" to "1"))))))
        stubFor(get(urlEqualTo("/api/personopplysning/v1/info"))
                        .willReturn(aResponse()
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(objectMapper.writeValueAsString(Ressurs.success(Personinfo(LocalDate.of(2019,
                                                                                                                              1,
                                                                                                                              1)))))))
        stubFor(get(urlEqualTo("/api/personopplysning/v1/info/BAR"))
                        .willReturn(aResponse()
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(objectMapper.writeValueAsString(Ressurs.success(Personinfo(LocalDate.of(2019,
                                                                                                                              1,
                                                                                                                              1)))))))
    }

    @Test
    fun `Kjør flyway migreringer og sjekk at behandlingslagerservice klarer å lese å skrive til postgresql`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        behandlingService.opprettBehandling(nyOrdinærBehandling(
                fnr))
        Assertions.assertEquals(1, behandlingService.hentBehandlinger(fagsak.id).size)
    }

    @Test
    fun `Test at opprettEllerOppdaterBehandling kjører uten feil`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        behandlingService.opprettBehandling(nyOrdinærBehandling(
                fnr))
        Assertions.assertEquals(1,
                                behandlingService.hentBehandlinger(fagsak.id).size)
    }

    @Test
    @Transactional
    fun `Opprett behandling`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        Assertions.assertEquals(fagsak.id, behandling.fagsak.id)
    }

    @Test
    fun `Kast feil om man lager ny behandling på fagsak som har behandling som skal godkjennes`() {
        val morId = randomFnr()

        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = morId))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(morId))
        behandling.steg = StegType.BESLUTTE_VEDTAK
        behandlingRepository.saveAndFlush(behandling)

        Assertions.assertThrows(Exception::class.java) {
            behandlingService.opprettBehandling(NyBehandling(
                    BehandlingKategori.NASJONAL,
                    BehandlingUnderkategori.ORDINÆR,
                    morId,
                    BehandlingType.REVURDERING,
                    null))
        }
    }

    @Test
    fun `Bruk samme behandling hvis nytt barn kommer på fagsak med aktiv behandling`() {
        val morId = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(morId)
        behandlingService.opprettBehandling(nyOrdinærBehandling(morId))

        Assertions.assertEquals(1, behandlingService.hentBehandlinger(fagsakId = fagsak.id).size)

        behandlingService.opprettBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                morId,
                BehandlingType.REVURDERING,
                null))

        val behandlinger = behandlingService.hentBehandlinger(fagsakId = fagsak.id)
        Assertions.assertEquals(1, behandlinger.size)
    }

    @Test
    fun `Opphør migrert vedtak via task`() {

        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()
        val stønadFom = LocalDate.of(2020, 1, 1)
        val stønadTom = stønadFom.plusYears(17)

        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerFnr))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(søkerFnr))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr, barn2Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val behandlingResultat =
                BehandlingResultat(behandling = behandling)
        behandlingResultat.personResultater =
                lagPersonResultaterForSøkerOgToBarn(behandlingResultat, søkerFnr, barn1Fnr, barn2Fnr, stønadFom, stønadTom)
        behandlingResultatRepository.save(behandlingResultat)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag)

        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
        Assertions.assertNotNull(vedtak)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        val task = opprettOpphørVedtakTask(
                behandling,
                vedtak!!,
                "saksbehandler",
                BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT,
                LocalDate.now()
        )

        val taskRepository: TaskRepository = mockk()
        val slot = slot<Task>()

        every { taskRepository.save(capture(slot)) } answers { slot.captured }

        OpphørVedtakTask(
                vedtakService,
                totrinnskontrollService,
                taskRepository
        ).doTask(task)

        verify(exactly = 1) {
            taskRepository.save(any())
            Assertions.assertEquals("iverksettMotOppdrag", slot.captured.taskStepType)
        }

        val aktivBehandling = behandlingService.hentAktivForFagsak(behandling.fagsak.id)

        Assertions.assertEquals(BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT, aktivBehandling!!.type)
        Assertions.assertNotEquals(behandling.id, aktivBehandling.id)
    }

    @Test
    fun `Opprett barnas beregning på vedtak`() {

        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()

        val dato_2020_01_01 = LocalDate.of(2020, 1, 1)
        val dato_2020_10_01 = LocalDate.of(2020, 10, 1)
        val stønadTom = dato_2020_01_01.plusYears(17)

        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerFnr))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(søkerFnr))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr, barn2Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag)

        val behandlingResultat =
                BehandlingResultat(behandling = behandling)
        behandlingResultat.personResultater = setOf(
                lagPersonResultat(behandlingResultat = behandlingResultat,
                                  fnr = søkerFnr,
                                  resultat = Resultat.JA,
                                  periodeFom = dato_2020_01_01.minusMonths(1),
                                  periodeTom = stønadTom,
                                  lagFullstendigVilkårResultat = true,
                                  personType = PersonType.SØKER
                ),
                lagPersonResultat(behandlingResultat = behandlingResultat,
                                  fnr = barn1Fnr,
                                  resultat = Resultat.JA,
                                  periodeFom = dato_2020_01_01.minusMonths(1),
                                  periodeTom = stønadTom,
                                  lagFullstendigVilkårResultat = true,
                                  personType = PersonType.BARN
                ),
                lagPersonResultat(behandlingResultat = behandlingResultat,
                                  fnr = barn2Fnr,
                                  resultat = Resultat.JA,
                                  periodeFom = dato_2020_10_01.minusMonths(1),
                                  periodeTom = stønadTom,
                                  lagFullstendigVilkårResultat = true,
                                  personType = PersonType.BARN
                )
        )
        behandlingResultatRepository.save(behandlingResultat)

        val restVedtakBarnMap = beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
                .data!!.behandlinger
                .flatMap { it.vedtakForBehandling }
                .flatMap { it!!.personBeregninger }
                .associateBy({ it.personIdent }, { it.ytelsePerioder[0] })

        Assertions.assertEquals(2, restVedtakBarnMap.size)
        Assertions.assertEquals(1054, restVedtakBarnMap[barn1Fnr]!!.beløp)
        Assertions.assertEquals(dato_2020_01_01, restVedtakBarnMap[barn1Fnr]!!.stønadFom)
        Assertions.assertTrue(dato_2020_01_01 < restVedtakBarnMap[barn1Fnr]!!.stønadTom)
        Assertions.assertEquals(YtelseType.ORDINÆR_BARNETRYGD, restVedtakBarnMap[barn1Fnr]!!.ytelseType)

        Assertions.assertEquals(1054, restVedtakBarnMap[barn2Fnr]!!.beløp)
        Assertions.assertEquals(dato_2020_10_01, restVedtakBarnMap[barn2Fnr]!!.stønadFom)
        Assertions.assertTrue(dato_2020_10_01 < restVedtakBarnMap[barn2Fnr]!!.stønadTom)
        Assertions.assertEquals(YtelseType.ORDINÆR_BARNETRYGD, restVedtakBarnMap[barn2Fnr]!!.ytelseType)
    }

    @Test
    fun `Endre barnas beregning på vedtak`() {

        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()
        val barn3Fnr = randomFnr()

        val dato_2020_01_01 = LocalDate.of(2020, 1, 1)
        val dato_2021_01_01 = LocalDate.of(2021, 1, 1)
        val stønadTom = dato_2020_01_01.plusYears(17)

        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerFnr))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(søkerFnr))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr, barn2Fnr, barn3Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        Assertions.assertNotNull(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag)

        val behandlingResultat1 =
                BehandlingResultat(behandling = behandling)
        behandlingResultat1.personResultater = lagPersonResultaterForSøkerOgToBarn(behandlingResultat1,
                                                                                   søkerFnr,
                                                                                   barn1Fnr,
                                                                                   barn2Fnr,
                                                                                   dato_2020_01_01.minusMonths(1),
                                                                                   stønadTom)
        behandlingResultatRepository.save(behandlingResultat1)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)


        val behandlingResultat2 =
                BehandlingResultat(behandling = behandling)
        behandlingResultat2.personResultater = lagPersonResultaterForSøkerOgToBarn(behandlingResultat2,
                                                                                   søkerFnr,
                                                                                   barn1Fnr,
                                                                                   barn3Fnr,
                                                                                   dato_2021_01_01.minusMonths(1),
                                                                                   stønadTom)
        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat = behandlingResultat2, loggHendelse = true)

        val restVedtakBarnMap = beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
                .data!!.behandlinger
                .flatMap { it.vedtakForBehandling }
                .flatMap { it!!.personBeregninger }
                .associateBy({ it.personIdent }, { it.ytelsePerioder[0] })

        Assertions.assertEquals(2, restVedtakBarnMap.size)
        Assertions.assertEquals(1054, restVedtakBarnMap[barn1Fnr]!!.beløp)
        Assertions.assertEquals(dato_2021_01_01, restVedtakBarnMap[barn1Fnr]!!.stønadFom)
        Assertions.assertTrue(dato_2021_01_01 < restVedtakBarnMap[barn1Fnr]!!.stønadTom)

        Assertions.assertEquals(1054, restVedtakBarnMap[barn3Fnr]!!.beløp)
        Assertions.assertEquals(dato_2021_01_01, restVedtakBarnMap[barn3Fnr]!!.stønadFom)
        Assertions.assertTrue(dato_2021_01_01 < restVedtakBarnMap[barn3Fnr]!!.stønadTom)

        Assertions.assertNull(restVedtakBarnMap[barn2Fnr])
    }

    @Test
    fun `Hent en persons bostedsadresse fra PDL og lagre den i database`() {
        val søkerFnr = ClientMocks.søkerFnr[0]
        val barn1Fnr = ClientMocks.barnFnr[0]
        val barn2Fnr = ClientMocks.barnFnr[1]
        val barn3Fnr = ClientMocks.søkerFnr[1]

        val matrikkelId = 123456L
        val søkerHusnummer = "12"
        val søkerHusbokstav = "A"
        val søkerBruksenhetsnummer = "H012"
        val søkerAdressnavn = "Sannergate"
        val søkerKommunenummer = "1234"
        val søkerTilleggsnavn = "whatever"
        val søkerPostnummer = "2222"

        val barn1Bruksenhetsnummer = "H201"
        val barn1Tilleggsnavn = "whoknows"
        val barn1Postnummer = "3333"
        val barn1Kommunenummer = "3233"

        val barn2BostedKommune = "Oslo"

        every { integrasjonClient.hentPersoninfoFor(søkerFnr) } returns Personinfo(
                fødselsdato = LocalDate.of(1990, 1, 1),
                adressebeskyttelseGradering = null,
                navn = "Mor",
                kjønn = Kjønn.KVINNE,
                familierelasjoner = emptySet(),
                bostedsadresse = Bostedsadresse(vegadresse = Vegadresse(matrikkelId,
                                                                        søkerHusnummer,
                                                                        søkerHusbokstav,
                                                                        søkerBruksenhetsnummer,
                                                                        søkerAdressnavn,
                                                                        søkerKommunenummer,
                                                                        søkerTilleggsnavn,
                                                                        søkerPostnummer)),
                sivilstand = null
        )

        every { integrasjonClient.hentPersoninfoFor(barn1Fnr) } returns Personinfo(
                fødselsdato = LocalDate.of(2009, 1, 1),
                adressebeskyttelseGradering = null,
                navn = "Gutt",
                kjønn = Kjønn.MANN,
                familierelasjoner = emptySet(),
                bostedsadresse = Bostedsadresse(matrikkeladresse = Matrikkeladresse(matrikkelId, barn1Bruksenhetsnummer, barn1Tilleggsnavn,
                                                                                    barn1Postnummer, barn1Kommunenummer)),
                sivilstand = null
        )

        every { integrasjonClient.hentPersoninfoFor(barn2Fnr) } returns Personinfo(
                fødselsdato = LocalDate.of(2012, 1, 1),
                adressebeskyttelseGradering = null,
                navn = "Jente",
                kjønn = Kjønn.KVINNE,
                familierelasjoner = emptySet(),
                bostedsadresse = Bostedsadresse(ukjentBosted = UkjentBosted(barn2BostedKommune)),
                sivilstand = null
        )

        every { integrasjonClient.hentPersoninfoFor(barn3Fnr) } returns Personinfo(
                fødselsdato = LocalDate.of(2013, 1, 1),
                adressebeskyttelseGradering = null,
                navn = "Jente2",
                kjønn = Kjønn.KVINNE,
                familierelasjoner = emptySet(),
                bostedsadresse = Bostedsadresse(),
                sivilstand = null
        )

        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerFnr))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(søkerFnr))

        persongrunnlagService.lagreSøkerOgBarnIPersonopplysningsgrunnlaget(søkerFnr,
                                                                           listOf(barn1Fnr, barn2Fnr, barn3Fnr),
                                                                           behandling)

        val søker = personRepository.findByPersonIdent(PersonIdent(søkerFnr)).first()
        val vegadresse = søker.bostedsadresse as GrVegadresse
        Assertions.assertEquals(søkerAdressnavn, vegadresse.adressenavn)
        Assertions.assertEquals(matrikkelId, vegadresse.matrikkelId)
        Assertions.assertEquals(søkerBruksenhetsnummer, vegadresse.bruksenhetsnummer)
        Assertions.assertEquals(søkerHusbokstav, vegadresse.husbokstav)
        Assertions.assertEquals(søkerHusnummer, vegadresse.husnummer)
        Assertions.assertEquals(søkerKommunenummer, vegadresse.kommunenummer)
        Assertions.assertEquals(søkerPostnummer, vegadresse.postnummer)
        Assertions.assertEquals(søkerTilleggsnavn, vegadresse.tilleggsnavn)

        Assertions.assertEquals(4, søker.personopplysningGrunnlag.personer.size)

        søker.personopplysningGrunnlag.barna.forEach {
            if (it.personIdent.ident == barn1Fnr) {
                val matrikkeladresse = it.bostedsadresse as GrMatrikkeladresse
                Assertions.assertEquals(barn1Bruksenhetsnummer, matrikkeladresse.bruksenhetsnummer)
                Assertions.assertEquals(barn1Kommunenummer, matrikkeladresse.kommunenummer)
                Assertions.assertEquals(barn1Postnummer, matrikkeladresse.postnummer)
                Assertions.assertEquals(barn1Tilleggsnavn, matrikkeladresse.tilleggsnavn)
            } else if (it.personIdent.ident == barn2Fnr) {
                val ukjentBosted = it.bostedsadresse as GrUkjentBosted
                Assertions.assertEquals(barn2BostedKommune, ukjentBosted.bostedskommune)
            } else if (it.personIdent.ident == barn3Fnr) {
                Assertions.assertNull(it.bostedsadresse)
            } else {
                throw RuntimeException("Ujent barn fnr")
            }
        }
    }
}
