package no.nav.familie.ba.sak.kjerne.behandling

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.mockk.MockKAnnotations
import io.mockk.every
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagPersonResultaterForSøkerOgToBarn
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRequest
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse.Companion.sisteAdresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrMatrikkeladresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrUkjentBosted
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrVegadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType
import no.nav.familie.eksterne.kontrakter.saksstatistikk.ResultatBegrunnelseDVH
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
        private val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository,

        @Autowired
        private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,

        ) {


    @BeforeEach
    fun setup() {
        databaseCleanupService.truncate()

        MockKAnnotations.init(this)

        stubFor(get(urlEqualTo("/api/aktoer/v1"))
                        .willReturn(aResponse()
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(objectMapper.writeValueAsString(Ressurs.success(mapOf("aktørId" to "1"))))))
    }

    @Test
    fun `Kjør flyway migreringer og sjekk at behandlingslagerservice klarer å lese å skrive til postgresql`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        behandlingService.opprettBehandling(nyOrdinærBehandling(
                fnr))
        assertEquals(1, behandlingService.hentBehandlinger(fagsak.id).size)
    }

    @Test
    fun `Test at opprettEllerOppdaterBehandling kjører uten feil`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        behandlingService.opprettBehandling(nyOrdinærBehandling(
                fnr))
        assertEquals(1,
                     behandlingService.hentBehandlinger(fagsak.id).size)
    }

    @Test
    @Transactional
    fun `Opprett behandling`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        assertEquals(fagsak.id, behandling.fagsak.id)
    }

    @Test
    fun `Kast feil ved opprettelse av behandling for ny person med åpen sak i Infotrygd`() {
        val fnr = randomFnr()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)

        every { infotrygdBarnetrygdClient.harÅpenSakIInfotrygd(listOf(fnr)) } returns true

        assertThatThrownBy { behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak)) }
                .hasMessageContaining("sak i Infotrygd")
    }

    @Test
    fun `Kast feil ved opprettelse av behandling for ny person med løpende sak i Infotrygd, utenom migrering`() {
        val fnr = randomFnr()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)

        every { infotrygdBarnetrygdClient.harLøpendeSakIInfotrygd(listOf(fnr)) } returns true

        assertThatThrownBy { behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak)) }
                .hasMessageContaining("sak i Infotrygd")

        val behandling = behandlingService.opprettBehandling(NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                skalBehandlesAutomatisk = true,
                søkersIdent = fnr
        ))
        assertNotNull(vedtakService.hentAktivForBehandling(behandlingId = behandling.id))
        assertDoesNotThrow {
            behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak,
                                                                               behandlingType = BehandlingType.REVURDERING))
        }
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

        assertThrows(Exception::class.java) {
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

        assertEquals(1, behandlingService.hentBehandlinger(fagsakId = fagsak.id).size)

        behandlingService.opprettBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                morId,
                BehandlingType.REVURDERING,
                null))

        val behandlinger = behandlingService.hentBehandlinger(fagsakId = fagsak.id)
        assertEquals(1, behandlinger.size)
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

        val satsEndringDato2020 = SatsService.hentDatoForSatsendring(SatsType.TILLEGG_ORBA, 1354)!!.toYearMonth()
        val satsEndringDato2121 = SatsService.hentDatoForSatsendring(SatsType.TILLEGG_ORBA, 1654)!!.toYearMonth()
        assertEquals(2, restVedtakBarnMap.size)

        // Barn 1
        val barn1Perioder = restVedtakBarnMap[barn1Fnr]!!.sortedBy { it.stønadFom }
        assertEquals(1054, barn1Perioder[0].beløp)
        assertEquals(januar2020, barn1Perioder[0].stønadFom)
        assertEquals(satsEndringDato2020.minusMonths(1), barn1Perioder[0].stønadTom)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, barn1Perioder[0].ytelseType)
        assertEquals(1354, barn1Perioder[1].beløp)
        assertEquals(satsEndringDato2020, barn1Perioder[1].stønadFom)
        assertEquals(satsEndringDato2121.minusMonths(1), barn1Perioder[1].stønadTom)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, barn1Perioder[1].ytelseType)
        assertEquals(1654, barn1Perioder[2].beløp)
        assertEquals(satsEndringDato2121, barn1Perioder[2].stønadFom)
        assertTrue(januar2020 < barn1Perioder[2].stønadTom)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, barn1Perioder[2].ytelseType)
        assertEquals(1054, barn1Perioder[3].beløp)

        // Barn 2
        val barn2Perioder = restVedtakBarnMap[barn2Fnr]!!.sortedBy { it.stønadFom }
        assertEquals(1354, barn2Perioder[0].beløp)
        assertEquals(oktober2020, barn2Perioder[0].stønadFom)
        assertTrue(oktober2020 < barn2Perioder[0].stønadTom)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, barn2Perioder[0].ytelseType)
        assertEquals(1654, barn2Perioder[1].beløp)
        assertEquals(satsEndringDato2121, barn2Perioder[1].stønadFom)
        assertTrue(januar2020 < barn2Perioder[1].stønadTom)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, barn2Perioder[1].ytelseType)
        assertEquals(1054, barn2Perioder[2].beløp)
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

        val satsEndringDato = SatsService.hentDatoForSatsendring(SatsType.TILLEGG_ORBA, 1654)!!.toYearMonth()

        val restVedtakBarnMap = beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
                .data!!.behandlinger
                .flatMap { it.personerMedAndelerTilkjentYtelse }
                .associateBy({ it.personIdent }, { it.ytelsePerioder.sortedBy { it.stønadFom } })

        assertEquals(2, restVedtakBarnMap.size)

        // Barn 1
        val barn1Perioder = restVedtakBarnMap[barn1Fnr]!!.sortedBy { it.stønadFom }
        assertEquals(3, barn1Perioder.size)
        assertEquals(1354, barn1Perioder[0].beløp)
        assertEquals(januar2021, barn1Perioder[0].stønadFom)
        assertEquals(satsEndringDato.minusMonths(1), barn1Perioder[0].stønadTom)
        assertEquals(1654, barn1Perioder[1].beløp)
        assertEquals(satsEndringDato, barn1Perioder[1].stønadFom)
        assertEquals(1054, barn1Perioder[2].beløp)
        assertTrue(stønadTom >= barn1Perioder[2].stønadTom)

        // Barn 3
        val barn3perioder = restVedtakBarnMap[barn3Fnr]!!.sortedBy { it.stønadFom }
        assertEquals(3, barn3perioder.size)
        assertEquals(1354, barn3perioder[0].beløp)
        assertEquals(januar2021, barn3perioder[0].stønadFom)
        assertEquals(satsEndringDato.minusMonths(1), barn3perioder[0].stønadTom)
        assertEquals(1654, barn3perioder[1].beløp)
        assertEquals(satsEndringDato, barn3perioder[1].stønadFom)
        assertEquals(1054, barn3perioder[2].beløp)
        assertTrue(stønadTom >= barn3perioder[2].stønadTom)
    }

    @Test
    fun `Hent en persons bostedsadresse fra PDL og lagre den i database`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()

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
                forelderBarnRelasjon = emptySet(),
                bostedsadresser = mutableListOf(Bostedsadresse(vegadresse = Vegadresse(matrikkelId,
                                                                                       søkerHusnummer,
                                                                                       søkerHusbokstav,
                                                                                       søkerBruksenhetsnummer,
                                                                                       søkerAdressnavn,
                                                                                       søkerKommunenummer,
                                                                                       søkerTilleggsnavn,
                                                                                       søkerPostnummer))),
                sivilstander = listOf(Sivilstand(type = SIVILSTAND.UOPPGITT)),
        )

        every { personopplysningerService.hentPersoninfoMedRelasjoner(barn1Fnr) } returns PersonInfo(
                fødselsdato = LocalDate.of(2009, 1, 1),
                adressebeskyttelseGradering = null,
                navn = "Gutt",
                kjønn = Kjønn.MANN,
                forelderBarnRelasjon = emptySet(),
                bostedsadresser = mutableListOf(Bostedsadresse(matrikkeladresse = Matrikkeladresse(matrikkelId,
                                                                                                   barn1Bruksenhetsnummer,
                                                                                                   barn1Tilleggsnavn,
                                                                                                   barn1Postnummer,
                                                                                                   barn1Kommunenummer))),
                sivilstander = listOf(Sivilstand(type = SIVILSTAND.UOPPGITT)),
        )

        every { personopplysningerService.hentPersoninfoMedRelasjoner(barn2Fnr) } returns PersonInfo(
                fødselsdato = LocalDate.of(2012, 1, 1),
                adressebeskyttelseGradering = null,
                navn = "Jente",
                kjønn = Kjønn.KVINNE,
                forelderBarnRelasjon = emptySet(),
                bostedsadresser = mutableListOf(Bostedsadresse(ukjentBosted = UkjentBosted(barn2BostedKommune))),
                sivilstander = listOf(Sivilstand(type = SIVILSTAND.UOPPGITT)),
        )

        every { personopplysningerService.hentHistoriskPersoninfoManuell(søkerFnr) } returns PersonInfo(
                fødselsdato = LocalDate.of(1990, 1, 1),
                bostedsadresser = mutableListOf(Bostedsadresse(vegadresse = Vegadresse(matrikkelId,
                                                                                       søkerHusnummer,
                                                                                       søkerHusbokstav,
                                                                                       søkerBruksenhetsnummer,
                                                                                       søkerAdressnavn,
                                                                                       søkerKommunenummer,
                                                                                       søkerTilleggsnavn,
                                                                                       søkerPostnummer))),
        )
        every { personopplysningerService.hentHistoriskPersoninfoManuell(barn1Fnr) } returns PersonInfo(
                fødselsdato = LocalDate.of(2009, 1, 1),
                bostedsadresser = mutableListOf(Bostedsadresse(matrikkeladresse = Matrikkeladresse(matrikkelId,
                                                                                                   barn1Bruksenhetsnummer,
                                                                                                   barn1Tilleggsnavn,
                                                                                                   barn1Postnummer,
                                                                                                   barn1Kommunenummer)))
        )
        every { personopplysningerService.hentHistoriskPersoninfoManuell(barn2Fnr) } returns PersonInfo(
                fødselsdato = LocalDate.of(1990, 1, 1),
                bostedsadresser = mutableListOf(Bostedsadresse(ukjentBosted = UkjentBosted(barn2BostedKommune)))
        )

        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerFnr))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(søkerFnr))

        persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(søkerFnr,
                                                                  listOf(barn1Fnr, barn2Fnr),
                                                                  behandling,
                                                                  Målform.NB)

        val søker = personRepository.findByPersonIdent(PersonIdent(søkerFnr)).first()
        val vegadresse = søker.bostedsadresser.sisteAdresse() as GrVegadresse
        assertEquals(søkerAdressnavn, vegadresse.adressenavn)
        assertEquals(matrikkelId, vegadresse.matrikkelId)
        assertEquals(søkerBruksenhetsnummer, vegadresse.bruksenhetsnummer)
        assertEquals(søkerHusbokstav, vegadresse.husbokstav)
        assertEquals(søkerHusnummer, vegadresse.husnummer)
        assertEquals(søkerKommunenummer, vegadresse.kommunenummer)
        assertEquals(søkerPostnummer, vegadresse.postnummer)
        assertEquals(søkerTilleggsnavn, vegadresse.tilleggsnavn)

        assertEquals(3, søker.personopplysningGrunnlag.personer.size)

        søker.personopplysningGrunnlag.barna.forEach {
            when (it.personIdent.ident) {
                barn1Fnr -> {
                    val matrikkeladresse = it.bostedsadresser.sisteAdresse() as GrMatrikkeladresse
                    assertEquals(barn1Bruksenhetsnummer, matrikkeladresse.bruksenhetsnummer)
                    assertEquals(barn1Kommunenummer, matrikkeladresse.kommunenummer)
                    assertEquals(barn1Postnummer, matrikkeladresse.postnummer)
                    assertEquals(barn1Tilleggsnavn, matrikkeladresse.tilleggsnavn)
                }
                barn2Fnr -> {
                    val ukjentBosted = it.bostedsadresser.sisteAdresse() as GrUkjentBosted
                    assertEquals(barn2BostedKommune, ukjentBosted.bostedskommune)
                }
                else -> {
                    throw RuntimeException("Ujent barn fnr")
                }
            }
        }
    }

    @Test
    fun `Skal kaste feil dersom bostedsadresse uten verdier forsøkes å mappes`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()

        every { personopplysningerService.hentPersoninfoMedRelasjoner(søkerFnr) } returns PersonInfo(
                fødselsdato = LocalDate.of(1990, 1, 1),
                adressebeskyttelseGradering = null,
                navn = "Mor",
                kjønn = Kjønn.KVINNE,
                forelderBarnRelasjon = emptySet(),
                bostedsadresser = mutableListOf(Bostedsadresse()),
                sivilstander = listOf(Sivilstand(type = SIVILSTAND.UOPPGITT)),
        )

        every { personopplysningerService.hentPersoninfoMedRelasjoner(barn1Fnr) } returns PersonInfo(
                fødselsdato = LocalDate.of(2009, 1, 1),
                adressebeskyttelseGradering = null,
                navn = "Gutt",
                kjønn = Kjønn.MANN,
                forelderBarnRelasjon = emptySet(),
                bostedsadresser = mutableListOf(Bostedsadresse()),
                sivilstander = listOf(Sivilstand(type = SIVILSTAND.UOPPGITT)),
        )

        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerFnr))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(søkerFnr))

        assertThrows<Feil> {
            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(søkerFnr,
                                                                      listOf(barn1Fnr),
                                                                      behandling,
                                                                      Målform.NB)
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
        assertThat(behandlingDvhMeldinger.last().resultat).isEqualTo("AVSLÅTT")
        assertThat(behandlingDvhMeldinger.last().resultatBegrunnelser).containsExactlyInAnyOrder(
                ResultatBegrunnelseDVH(fom, tom, "AVSLAG", VedtakBegrunnelseSpesifikasjon.AVSLAG_FRITEKST.name),
                ResultatBegrunnelseDVH(fom, tom, "AVSLAG", VedtakBegrunnelseSpesifikasjon.AVSLAG_BOR_HOS_SØKER.name),
        )
    }
}
