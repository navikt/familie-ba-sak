package no.nav.familie.ba.sak.behandling

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.mockk.*
import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.behandling.domene.tilstand.BehandlingStegTilstandRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakPersonRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRequest
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.*
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultat
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatRepository
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.SatsService
import no.nav.familie.ba.sak.beregning.domene.SatsType
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
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
@ActiveProfiles("postgres", "mock-dokgen", "mock-oauth", "mock-pdl", "mock-arbeidsfordeling")
@Tag("integration")
@AutoConfigureWireMock(port = 28085)
class BehandlingIntegrationTest(
        @Autowired
        private val behandlingRepository: BehandlingRepository,

        @Autowired
        private val personRepository: PersonRepository,

        @Autowired
        private val vedtakService: VedtakService,

        @Autowired
        private val persongrunnlagService: PersongrunnlagService,

        @Autowired
        private val beregningService: BeregningService,

        @Autowired
        private val behandlingResultatRepository: BehandlingResultatRepository,

        @Autowired
        private val behandlingResultatService: BehandlingResultatService,

        @Autowired
        private val fagsakPersonRepository: FagsakPersonRepository,

        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val personopplysningerService: PersonopplysningerService,

        @Autowired
        private val databaseCleanupService: DatabaseCleanupService,

        @Autowired
        private val behandlingMetrikker: BehandlingMetrikker,

        @Autowired
        private val loggService: LoggService,

        @Autowired
        private val arbeidsfordelingService: ArbeidsfordelingService,

        @Autowired
        private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,

        @Autowired
        private val behandlingStegTilstandRepository: BehandlingStegTilstandRepository
) {

    lateinit var behandlingService: BehandlingService

    @BeforeEach
    fun setup() {
        databaseCleanupService.truncate()

        MockKAnnotations.init(this)
        behandlingService = BehandlingService(
                behandlingRepository,
                behandlingMetrikker,
                fagsakPersonRepository,
                persongrunnlagService,
                beregningService,
                fagsakService,
                loggService,
                arbeidsfordelingService,
                saksstatistikkEventPublisher,
                behandlingStegTilstandRepository
        )

        stubFor(get(urlEqualTo("/api/aktoer/v1"))
                        .willReturn(aResponse()
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(objectMapper.writeValueAsString(Ressurs.success(mapOf("aktørId" to "1"))))))

        stubFor(get(urlEqualTo("/api/personopplysning/v1/info/BAR"))
                        .willReturn(aResponse()
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(objectMapper.writeValueAsString(Ressurs.success(PersonInfo(LocalDate.of(2019,
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
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
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
                                  resultat = Resultat.OPPFYLT,
                                  periodeFom = dato_2020_01_01.minusMonths(1),
                                  periodeTom = stønadTom,
                                  lagFullstendigVilkårResultat = true,
                                  personType = PersonType.SØKER
                ),
                lagPersonResultat(behandlingResultat = behandlingResultat,
                                  fnr = barn1Fnr,
                                  resultat = Resultat.OPPFYLT,
                                  periodeFom = dato_2020_01_01.minusMonths(1),
                                  periodeTom = stønadTom,
                                  lagFullstendigVilkårResultat = true,
                                  personType = PersonType.BARN
                ),
                lagPersonResultat(behandlingResultat = behandlingResultat,
                                  fnr = barn2Fnr,
                                  resultat = Resultat.OPPFYLT,
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
                .flatMap { it.personBeregninger }
                .associateBy({ it.personIdent }, { it.ytelsePerioder.sortedBy { it.stønadFom } })

        val satsEndringDato = SatsService.hentDatoForSatsendring(SatsType.TILLEGG_ORBA, 1354)
        Assertions.assertEquals(2, restVedtakBarnMap.size)

        // Barn 1
        Assertions.assertEquals(1054, restVedtakBarnMap[barn1Fnr]!![0].beløp)
        Assertions.assertEquals(dato_2020_01_01, restVedtakBarnMap[barn1Fnr]!![0].stønadFom)
        Assertions.assertTrue(dato_2020_01_01 < restVedtakBarnMap[barn1Fnr]!![0].stønadTom)
        Assertions.assertEquals(YtelseType.ORDINÆR_BARNETRYGD, restVedtakBarnMap[barn1Fnr]!![0].ytelseType)
        Assertions.assertEquals(1354, restVedtakBarnMap[barn1Fnr]!![1].beløp)
        Assertions.assertEquals(satsEndringDato, restVedtakBarnMap[barn1Fnr]!![1].stønadFom)
        Assertions.assertTrue(dato_2020_01_01 < restVedtakBarnMap[barn1Fnr]!![1].stønadTom)
        Assertions.assertEquals(YtelseType.ORDINÆR_BARNETRYGD, restVedtakBarnMap[barn1Fnr]!![1].ytelseType)
        Assertions.assertEquals(1054, restVedtakBarnMap[barn1Fnr]!![2].beløp)
        Assertions.assertEquals(dato_2020_01_01.plusYears(5).førsteDagIInneværendeMåned(),
                                restVedtakBarnMap[barn1Fnr]!![2].stønadFom)
        Assertions.assertTrue(dato_2020_01_01 < restVedtakBarnMap[barn1Fnr]!![2].stønadTom)
        Assertions.assertEquals(YtelseType.ORDINÆR_BARNETRYGD, restVedtakBarnMap[barn1Fnr]!![2].ytelseType)

        // Barn 2
        Assertions.assertEquals(1354, restVedtakBarnMap[barn2Fnr]!![0].beløp)
        Assertions.assertEquals(dato_2020_10_01, restVedtakBarnMap[barn2Fnr]!![0].stønadFom)
        Assertions.assertTrue(dato_2020_10_01 < restVedtakBarnMap[barn2Fnr]!![0].stønadTom)
        Assertions.assertEquals(YtelseType.ORDINÆR_BARNETRYGD, restVedtakBarnMap[barn2Fnr]!![0].ytelseType)
        Assertions.assertEquals(1054, restVedtakBarnMap[barn2Fnr]!![1].beløp)
        Assertions.assertEquals(dato_2020_01_01.plusYears(5).førsteDagIInneværendeMåned(),
                                restVedtakBarnMap[barn2Fnr]!![1].stønadFom)
        Assertions.assertTrue(dato_2020_01_01 < restVedtakBarnMap[barn2Fnr]!![1].stønadTom)
        Assertions.assertEquals(YtelseType.ORDINÆR_BARNETRYGD, restVedtakBarnMap[barn2Fnr]!![1].ytelseType)
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
        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat = behandlingResultat2)

        val restVedtakBarnMap = beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
                .data!!.behandlinger
                .flatMap { it.vedtakForBehandling }
                .flatMap { it.personBeregninger }
                .associateBy({ it.personIdent }, { it.ytelsePerioder.sortedBy { it.stønadFom } })

        Assertions.assertEquals(2, restVedtakBarnMap.size)

        Assertions.assertEquals(1354, restVedtakBarnMap[barn1Fnr]!![0].beløp)
        Assertions.assertEquals(dato_2021_01_01, restVedtakBarnMap[barn1Fnr]!![0].stønadFom)
        Assertions.assertTrue(dato_2021_01_01 < restVedtakBarnMap[barn1Fnr]!![0].stønadTom)
        Assertions.assertEquals(1054, restVedtakBarnMap[barn1Fnr]!![1].beløp)
        Assertions.assertEquals(dato_2021_01_01.plusYears(4).førsteDagIInneværendeMåned(),
                                restVedtakBarnMap[barn1Fnr]!![1].stønadFom)
        Assertions.assertTrue(dato_2021_01_01 < restVedtakBarnMap[barn1Fnr]!![1].stønadTom)

        Assertions.assertEquals(1354, restVedtakBarnMap[barn3Fnr]!![0].beløp)
        Assertions.assertEquals(dato_2021_01_01, restVedtakBarnMap[barn3Fnr]!![0].stønadFom)
        Assertions.assertTrue(dato_2021_01_01 < restVedtakBarnMap[barn3Fnr]!![0].stønadTom)
        Assertions.assertEquals(1054, restVedtakBarnMap[barn3Fnr]!![1].beløp)
        Assertions.assertEquals(dato_2021_01_01.plusYears(4).førsteDagIInneværendeMåned(),
                                restVedtakBarnMap[barn3Fnr]!![1].stønadFom)
        Assertions.assertTrue(dato_2021_01_01 < restVedtakBarnMap[barn3Fnr]!![1].stønadTom)
    }

    @Test
    fun `Hent en persons bostedsadresse fra PDL og lagre den i database`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()
        val barn3Fnr = randomFnr()

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

        every { personopplysningerService.hentPersoninfoMedRelasjoner(søkerFnr) } returns PersonInfo(
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

        every { personopplysningerService.hentPersoninfoMedRelasjoner(barn1Fnr) } returns PersonInfo(
                fødselsdato = LocalDate.of(2009, 1, 1),
                adressebeskyttelseGradering = null,
                navn = "Gutt",
                kjønn = Kjønn.MANN,
                familierelasjoner = emptySet(),
                bostedsadresse = Bostedsadresse(matrikkeladresse = Matrikkeladresse(matrikkelId,
                                                                                    barn1Bruksenhetsnummer,
                                                                                    barn1Tilleggsnavn,
                                                                                    barn1Postnummer,
                                                                                    barn1Kommunenummer)),
                sivilstand = null
        )

        every { personopplysningerService.hentPersoninfoMedRelasjoner(barn2Fnr) } returns PersonInfo(
                fødselsdato = LocalDate.of(2012, 1, 1),
                adressebeskyttelseGradering = null,
                navn = "Jente",
                kjønn = Kjønn.KVINNE,
                familierelasjoner = emptySet(),
                bostedsadresse = Bostedsadresse(ukjentBosted = UkjentBosted(barn2BostedKommune)),
                sivilstand = null
        )

        every { personopplysningerService.hentPersoninfoMedRelasjoner(barn3Fnr) } returns PersonInfo(
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
                                                                           behandling,
                                                                           Målform.NB)

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
