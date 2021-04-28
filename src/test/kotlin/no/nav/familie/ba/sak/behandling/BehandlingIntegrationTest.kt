package no.nav.familie.ba.sak.behandling

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.mockk.MockKAnnotations
import io.mockk.every
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRequest
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.GrMatrikkeladresse
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.GrUkjentBosted
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.GrVegadresse
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.behandling.vilkår.Vilkårsvurdering
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingRepository
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.SatsService
import no.nav.familie.ba.sak.beregning.domene.SatsType
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagPersonResultaterForSøkerOgToBarn
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.saksstatistikk.domene.SaksstatistikkMellomlagringType
import no.nav.familie.ba.sak.saksstatistikk.sakstatistikkObjectMapper
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.ResultatBegrunnelseDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
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
import java.time.YearMonth
import javax.transaction.Transactional

@SpringBootTest(properties = ["FAMILIE_INTEGRASJONER_API_URL=http://localhost:28085/api"])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-brev-klient", "mock-oauth", "mock-pdl", "mock-arbeidsfordeling", "mock-infotrygd-barnetrygd")
@Tag("integration")
@AutoConfigureWireMock(port = 28085)
class BehandlingIntegrationTest(
        @Autowired
        private val behandlingRepository: BehandlingRepository,

        @Autowired
        private val behandlingService: BehandlingService,

        @Autowired
        private val personRepository: PersonRepository,

        @Autowired
        private val vedtakService: VedtakService,

        @Autowired
        private val persongrunnlagService: PersongrunnlagService,

        @Autowired
        private val beregningService: BeregningService,

        @Autowired
        private val vilkårsvurderingRepository: VilkårsvurderingRepository,

        @Autowired
        private val vilkårsvurderingService: VilkårsvurderingService,

        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val personopplysningerService: PersonopplysningerService,

        @Autowired
        private val databaseCleanupService: DatabaseCleanupService,

        @Autowired
        private val oppgaveService: OppgaveService,


        @Autowired
        private val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository

) {


    @BeforeEach
    fun setup() {
        databaseCleanupService.truncate()

        MockKAnnotations.init(this)

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
    fun `Opprett behandle sak oppgave ved opprettelse av førstegangsbehandling`() {
        val fnr = randomFnr()

        fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(fnr))

        assertNotNull(oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype = Oppgavetype.BehandleSak,
                                                                     behandling = behandling))
    }

    @Test
    fun `Opprett aktivt vedtak ved opprettelse av behandling`() {
        val fnr = randomFnr()

        fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(fnr))

        assertNotNull(vedtakService.hentAktivForBehandling(behandlingId = behandling.id))
    }

    @Test
    fun `Ikke opprett behandle sak oppgave ved opprettelse av fødselshendelsebehandling`() {
        val fnr = randomFnr()

        fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.opprettBehandling(NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                skalBehandlesAutomatisk = true,
                søkersIdent = fnr
        ))

        assertNull(oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype = Oppgavetype.BehandleSak,
                                                                  behandling = behandling))
    }

    @Test
    fun `Kast feil om man lager ny behandling på fagsak som har behandling som skal godkjennes`() {
        val morId = randomFnr()

        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = morId))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(morId))
        behandling.behandlingStegTilstand.forEach { it.behandlingStegStatus = BehandlingStegStatus.UTFØRT }
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(behandling = behandling,
                                                                     behandlingSteg = StegType.BESLUTTE_VEDTAK))
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

        val januar2020 = YearMonth.of(2020, 1)
        val oktober2020 = YearMonth.of(2020, 10)
        val stønadTom = januar2020.plusYears(17)

        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerFnr))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(søkerFnr))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr, barn2Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling = behandling)

        val vilkårsvurdering =
                Vilkårsvurdering(behandling = behandling)
        vilkårsvurdering.personResultater = setOf(
                lagPersonResultat(vilkårsvurdering = vilkårsvurdering,
                                  fnr = søkerFnr,
                                  resultat = Resultat.OPPFYLT,
                                  periodeFom = januar2020.minusMonths(1).toLocalDate(),
                                  periodeTom = stønadTom.toLocalDate(),
                                  lagFullstendigVilkårResultat = true,
                                  personType = PersonType.SØKER
                ),
                lagPersonResultat(vilkårsvurdering = vilkårsvurdering,
                                  fnr = barn1Fnr,
                                  resultat = Resultat.OPPFYLT,
                                  periodeFom = januar2020.minusMonths(1).toLocalDate(),
                                  periodeTom = stønadTom.toLocalDate(),
                                  lagFullstendigVilkårResultat = true,
                                  personType = PersonType.BARN
                ),
                lagPersonResultat(vilkårsvurdering = vilkårsvurdering,
                                  fnr = barn2Fnr,
                                  resultat = Resultat.OPPFYLT,
                                  periodeFom = oktober2020.minusMonths(1).toLocalDate(),
                                  periodeTom = stønadTom.toLocalDate(),
                                  lagFullstendigVilkårResultat = true,
                                  personType = PersonType.BARN
                )
        )
        vilkårsvurderingRepository.save(vilkårsvurdering)

        val restVedtakBarnMap = beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
                .data!!.behandlinger
                .flatMap { it.personerMedAndelerTilkjentYtelse }
                .associateBy({ it.personIdent }, { it.ytelsePerioder.sortedBy { it.stønadFom } })

        val satsEndringDato = SatsService.hentDatoForSatsendring(SatsType.TILLEGG_ORBA, 1354)
        Assertions.assertEquals(2, restVedtakBarnMap.size)

        // Barn 1
        Assertions.assertEquals(1054, restVedtakBarnMap[barn1Fnr]!![0].beløp)
        Assertions.assertEquals(januar2020, restVedtakBarnMap[barn1Fnr]!![0].stønadFom)
        Assertions.assertTrue(januar2020 < restVedtakBarnMap[barn1Fnr]!![0].stønadTom)
        Assertions.assertEquals(YtelseType.ORDINÆR_BARNETRYGD, restVedtakBarnMap[barn1Fnr]!![0].ytelseType)
        Assertions.assertEquals(1354, restVedtakBarnMap[barn1Fnr]!![1].beløp)
        Assertions.assertEquals(satsEndringDato?.toYearMonth(), restVedtakBarnMap[barn1Fnr]!![1].stønadFom)
        Assertions.assertTrue(januar2020 < restVedtakBarnMap[barn1Fnr]!![1].stønadTom)
        Assertions.assertEquals(YtelseType.ORDINÆR_BARNETRYGD, restVedtakBarnMap[barn1Fnr]!![1].ytelseType)
        Assertions.assertEquals(1054, restVedtakBarnMap[barn1Fnr]!![2].beløp)
        Assertions.assertEquals(januar2020.plusYears(5),
                                restVedtakBarnMap[barn1Fnr]!![2].stønadFom)
        Assertions.assertTrue(januar2020 < restVedtakBarnMap[barn1Fnr]!![2].stønadTom)
        Assertions.assertEquals(YtelseType.ORDINÆR_BARNETRYGD, restVedtakBarnMap[barn1Fnr]!![2].ytelseType)

        // Barn 2
        Assertions.assertEquals(1354, restVedtakBarnMap[barn2Fnr]!![0].beløp)
        Assertions.assertEquals(oktober2020, restVedtakBarnMap[barn2Fnr]!![0].stønadFom)
        Assertions.assertTrue(oktober2020 < restVedtakBarnMap[barn2Fnr]!![0].stønadTom)
        Assertions.assertEquals(YtelseType.ORDINÆR_BARNETRYGD, restVedtakBarnMap[barn2Fnr]!![0].ytelseType)
        Assertions.assertEquals(1054, restVedtakBarnMap[barn2Fnr]!![1].beløp)
        Assertions.assertEquals(januar2020.plusYears(5),
                                restVedtakBarnMap[barn2Fnr]!![1].stønadFom)
        Assertions.assertTrue(januar2020 < restVedtakBarnMap[barn2Fnr]!![1].stønadTom)
        Assertions.assertEquals(YtelseType.ORDINÆR_BARNETRYGD, restVedtakBarnMap[barn2Fnr]!![1].ytelseType)
    }

    @Test
    fun `Endre barnas beregning på vedtak`() {

        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()
        val barn3Fnr = randomFnr()

        val januar2020 = YearMonth.of(2020, 1)
        val januar2021 = YearMonth.of(2021, 1)
        val stønadTom = januar2020.plusYears(17)

        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerFnr))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(søkerFnr))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr, barn2Fnr, barn3Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        assertNotNull(personopplysningGrunnlag)

        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling = behandling)

        val behandlingResultat1 =
                Vilkårsvurdering(behandling = behandling)
        behandlingResultat1.personResultater = lagPersonResultaterForSøkerOgToBarn(behandlingResultat1,
                                                                                   søkerFnr,
                                                                                   barn1Fnr,
                                                                                   barn2Fnr,
                                                                                   januar2020.minusMonths(1).toLocalDate(),
                                                                                   stønadTom.toLocalDate())
        vilkårsvurderingRepository.save(behandlingResultat1)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)


        val behandlingResultat2 =
                Vilkårsvurdering(behandling = behandling)
        behandlingResultat2.personResultater = lagPersonResultaterForSøkerOgToBarn(behandlingResultat2,
                                                                                   søkerFnr,
                                                                                   barn1Fnr,
                                                                                   barn3Fnr,
                                                                                   januar2021.minusMonths(1).toLocalDate(),
                                                                                   stønadTom.toLocalDate())
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = behandlingResultat2)

        val restVedtakBarnMap = beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
                .data!!.behandlinger
                .flatMap { it.personerMedAndelerTilkjentYtelse }
                .associateBy({ it.personIdent }, { it.ytelsePerioder.sortedBy { it.stønadFom } })

        Assertions.assertEquals(2, restVedtakBarnMap.size)

        Assertions.assertEquals(1354, restVedtakBarnMap[barn1Fnr]!![0].beløp)
        Assertions.assertEquals(januar2021, restVedtakBarnMap[barn1Fnr]!![0].stønadFom)
        Assertions.assertTrue(januar2021 < restVedtakBarnMap[barn1Fnr]!![0].stønadTom)
        Assertions.assertEquals(1054, restVedtakBarnMap[barn1Fnr]!![1].beløp)
        Assertions.assertEquals(januar2021.plusYears(4),
                                restVedtakBarnMap[barn1Fnr]!![1].stønadFom)
        Assertions.assertTrue(januar2021 < restVedtakBarnMap[barn1Fnr]!![1].stønadTom)

        Assertions.assertEquals(1354, restVedtakBarnMap[barn3Fnr]!![0].beløp)
        Assertions.assertEquals(januar2021, restVedtakBarnMap[barn3Fnr]!![0].stønadFom)
        Assertions.assertTrue(januar2021 < restVedtakBarnMap[barn3Fnr]!![0].stønadTom)
        Assertions.assertEquals(1054, restVedtakBarnMap[barn3Fnr]!![1].beløp)
        Assertions.assertEquals(januar2021.plusYears(4),
                                restVedtakBarnMap[barn3Fnr]!![1].stønadFom)
        Assertions.assertTrue(januar2021 < restVedtakBarnMap[barn3Fnr]!![1].stønadTom)
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

    @Test
    fun `Skal lagre og sende korrekt sakstatistikk for behandlingresultat og begrunnelser`() {
        val fnr = "12345678910"
        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(fnr))
        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling = behandling)
        val vedtak = vedtakService.hentAktivForBehandling(behandling.id)
        val fom = LocalDate.now().minusMonths(5)
        val tom = LocalDate.now().plusMonths(5)
        val vedtakBegrunnelser = setOf(VedtakBegrunnelse(vedtak = vedtak!!,
                                                         fom = fom,
                                                         tom = tom,
                                                         begrunnelse = VedtakBegrunnelseSpesifikasjon.AVSLAG_FRITEKST),
                                       VedtakBegrunnelse(vedtak = vedtak,
                                                         fom = fom,
                                                         tom = tom,
                                                         begrunnelse = VedtakBegrunnelseSpesifikasjon.AVSLAG_BOR_HOS_SØKER))

        vedtak.settBegrunnelser(vedtakBegrunnelser)
        vedtakService.oppdater(vedtak)

        behandlingService.lagreEllerOppdater(behandling.also { it.resultat = BehandlingResultat.AVSLÅTT })

        val behandlingDvhMeldinger = saksstatistikkMellomlagringRepository.finnMeldingerKlarForSending()
                .filter { it.type == SaksstatistikkMellomlagringType.BEHANDLING }
                .map { it.jsonToBehandlingDVH() }

        assertEquals(2, behandlingDvhMeldinger.size)
        assertEquals("AVSLÅTT", behandlingDvhMeldinger.last().resultat)
        assertThat(behandlingDvhMeldinger.last().resultat).isEqualTo("AVSLÅTT")
        assertThat(behandlingDvhMeldinger.last().resultatBegrunnelser).containsExactlyInAnyOrder(
                ResultatBegrunnelseDVH(fom, tom, "AVSLAG", VedtakBegrunnelseSpesifikasjon.AVSLAG_FRITEKST.name),
                ResultatBegrunnelseDVH(fom, tom, "AVSLAG", VedtakBegrunnelseSpesifikasjon.AVSLAG_BOR_HOS_SØKER.name),
        )
    }
}
