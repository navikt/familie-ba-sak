package no.nav.familie.ba.sak.sikkerhet

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.common.clearAllCaches
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.defaultFagsak
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.datagenerator.tilPersonEnkelSøkerOgBarn
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonEnkel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.skjermetbarnsøker.SkjermetBarnSøker
import no.nav.familie.ba.sak.kjerne.strengtfortrolig.StrengtFortroligService
import no.nav.familie.ba.sak.mock.FakeFamilieIntegrasjonerTilgangskontrollKlient
import no.nav.familie.ba.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ba.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.log.mdc.MDCConstants
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.YearMonth

class TilgangServiceTest {
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository = mockk()
    private val cacheManager = ConcurrentMapCacheManager()
    private val kode6Gruppe = "kode6"
    private val kode7Gruppe = "kode7"
    private val rolleConfig = RolleConfig("", "", "", FORVALTER_ROLLE = "", KODE6 = kode6Gruppe, KODE7 = kode7Gruppe)
    private val auditLogger = AuditLogger("familie-ba-sak")
    private val fakeFamilieIntegrasjonerTilgangskontrollKlient = FakeFamilieIntegrasjonerTilgangskontrollKlient(RestTemplate())

    private val familieIntegrasjonerTilgangskontrollService =
        FamilieIntegrasjonerTilgangskontrollService(
            fakeFamilieIntegrasjonerTilgangskontrollKlient,
            cacheManager,
            mockk(),
        )
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository = mockk(relaxed = true)
    private val strengtFortroligService =
        StrengtFortroligService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
            familieIntegrasjonerTilgangskontrollService = familieIntegrasjonerTilgangskontrollService,
            featureToggleService = featureToggleService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
        )
    private val tilgangService =
        TilgangService(
            familieIntegrasjonerTilgangskontrollService = familieIntegrasjonerTilgangskontrollService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            persongrunnlagService = persongrunnlagService,
            fagsakService = fagsakService,
            rolleConfig = rolleConfig,
            auditLogger = auditLogger,
            strengtFortroligService = strengtFortroligService,
        )

    private val fagsak = defaultFagsak()
    private val behandling = lagBehandling(fagsak)
    private val aktør = fagsak.aktør
    private val personopplysningGrunnlag =
        lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = aktør.aktivFødselsnummer(),
            barnasIdenter = emptyList(),
        )
    private val olaIdent = "4567"

    @BeforeEach
    internal fun setUp() {
        MDC.put(MDCConstants.MDC_CALL_ID, "00001111")
        mockBrukerContext()
        every { fagsakService.hentAktør(fagsak.id) } returns fagsak.aktør
        every { fagsakService.hentPåFagsakId(fagsak.id) } returns fagsak
        every { behandlingHentOgPersisterService.hent(any()) } returns behandling
        every { persongrunnlagService.hentSøkerOgBarnPåBehandling(behandling.id) } returns
            personopplysningGrunnlag.tilPersonEnkelSøkerOgBarn()
        every { featureToggleService.isEnabled(FeatureToggle.TILLAT_TILGANG_SKJERMET_BARN_UTEN_LØPENDE_ANDELER) } returns false
        cacheManager.clearAllCaches()
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
        fakeFamilieIntegrasjonerTilgangskontrollKlient.reset()
    }

    @Test
    internal fun `skal kaste RolleTilgangskontrollFeil dersom saksbehandler ikke har tilgang til person eller dets barn`() {
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(
            listOf(
                Tilgang(
                    aktør.aktivFødselsnummer(),
                    false,
                    "Bruker mangler rollen '0000-GA-Strengt_Fortrolig_Adresse",
                ),
            ),
        )

        val rolleTilgangskontrollFeil =
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilPersoner(
                    listOf(aktør.aktivFødselsnummer()),
                    AuditLoggerEvent.ACCESS,
                )
            }

        assertThat(rolleTilgangskontrollFeil.message).isEqualTo("Saksbehandler A har ikke tilgang. Bruker mangler rollen '0000-GA-Strengt_Fortrolig_Adresse.")
        assertThat(rolleTilgangskontrollFeil.frontendFeilmelding).isEqualTo("Saksbehandler A har ikke tilgang. Bruker mangler rollen '0000-GA-Strengt_Fortrolig_Adresse.")
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til person og dets barn`() {
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(
            listOf(
                Tilgang(
                    aktør.aktivFødselsnummer(),
                    true,
                ),
            ),
        )

        tilgangService.validerTilgangTilPersoner(listOf(aktør.aktivFødselsnummer()), AuditLoggerEvent.ACCESS)
    }

    @Test
    internal fun `skal kaste RolleTilgangskontrollFeil dersom saksbehandler ikke har tilgang til behandling`() {
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(
            listOf(
                Tilgang(
                    aktør.aktivFødselsnummer(),
                    false,
                    "NAV-ansatt",
                ),
            ),
        )

        val rolleTilgangskontrollFeil =
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilBehandling(
                    behandling.id,
                    AuditLoggerEvent.ACCESS,
                )
            }
        assertThat(rolleTilgangskontrollFeil.message).isEqualTo("Saksbehandler A har ikke tilgang til behandling=${behandling.id}. NAV-ansatt.")
        assertThat(rolleTilgangskontrollFeil.frontendFeilmelding).isEqualTo("Behandlingen inneholder personer som krever ytterligere tilganger. NAV-ansatt.")
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til behandling`() {
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(
            listOf(
                Tilgang(
                    aktør.aktivFødselsnummer(),
                    true,
                ),
            ),
        )

        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
    }

    @Test
    internal fun `validerTilgangTilPersoner - hvis samme saksbehandler kaller skal den ha cachet`() {
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(
            listOf(
                Tilgang(
                    olaIdent,
                    true,
                ),
            ),
        )

        mockBrukerContext("A")
        tilgangService.validerTilgangTilPersoner(listOf(olaIdent), AuditLoggerEvent.ACCESS)
        tilgangService.validerTilgangTilPersoner(listOf(olaIdent), AuditLoggerEvent.ACCESS)
        assertThat(fakeFamilieIntegrasjonerTilgangskontrollKlient.antallKallTilSjekkTilgangTilPersoner()).isEqualTo(1)
    }

    @Test
    internal fun `validerTilgangTilPersoner - hvis to ulike saksbehandler kaller skal den sjekke tilgang på nytt`() {
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(
            listOf(
                Tilgang(
                    olaIdent,
                    true,
                ),
            ),
        )
        mockBrukerContext("A")
        tilgangService.validerTilgangTilPersoner(listOf(olaIdent), AuditLoggerEvent.ACCESS)
        mockBrukerContext("B")
        tilgangService.validerTilgangTilPersoner(listOf(olaIdent), AuditLoggerEvent.ACCESS)
        assertThat(fakeFamilieIntegrasjonerTilgangskontrollKlient.antallKallTilSjekkTilgangTilPersoner()).isEqualTo(2)
    }

    @Test
    internal fun `validerTilgangTilBehandling - hvis samme saksbehandler kaller skal den ha cachet`() {
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(
            listOf(
                Tilgang(
                    aktør.aktivFødselsnummer(),
                    true,
                ),
            ),
        )

        mockBrukerContext("A")

        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
        assertThat(fakeFamilieIntegrasjonerTilgangskontrollKlient.antallKallTilSjekkTilgangTilPersoner()).isEqualTo(1)
    }

    @Test
    internal fun `validerTilgangTilBehandling - hvis to ulike saksbehandler kaller skal den sjekke tilgang på nytt`() {
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(
            listOf(
                Tilgang(
                    aktør.aktivFødselsnummer(),
                    true,
                ),
            ),
        )

        mockBrukerContext("A")
        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
        mockBrukerContext("B")
        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
        assertThat(fakeFamilieIntegrasjonerTilgangskontrollKlient.antallKallTilSjekkTilgangTilPersoner()).isEqualTo(2)
    }

    @Test
    fun `validerTilgangTilFagsak - skal kaste feil dersom søker eller et eller flere av barna har diskresjonskode og saksbehandler mangler tilgang`() {
        // Arrange
        val søkerAktør = randomAktør("65434563721")
        val barnAktør = randomAktør("12345678910")
        every { fagsakService.hentAktør(fagsak.id) }.returns(aktør)
        every { behandlingHentOgPersisterService.hentBehandlinger(fagsak.id) }.returns(listOf(behandling))
        every { persongrunnlagService.hentSøkerOgBarnPåFagsak(fagsak.id) }.returns(
            setOf(
                PersonEnkel(
                    aktør = søkerAktør,
                    type = PersonType.SØKER,
                    fødselsdato = LocalDate.now(),
                    dødsfallDato = null,
                    målform = Målform.NB,
                ),
                PersonEnkel(
                    aktør = barnAktør,
                    type = PersonType.BARN,
                    fødselsdato = LocalDate.now(),
                    dødsfallDato = null,
                    målform = Målform.NB,
                ),
            ),
        )
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(
            listOf(
                Tilgang(
                    søkerAktør.aktivFødselsnummer(),
                    false,
                    "Bruker mangler rollen '0000-GA-Strengt_Fortrolig_Adresse'",
                ),
            ),
        )

        mockBrukerContext("A")

        // Act & Assert
        val rolletilgangskontrollFeil =
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilFagsak(
                    fagsak.id,
                    AuditLoggerEvent.ACCESS,
                )
            }
        assertThat(rolletilgangskontrollFeil.message).isEqualTo("Saksbehandler A har ikke tilgang til fagsak=${fagsak.id}. Bruker mangler rollen '0000-GA-Strengt_Fortrolig_Adresse'.")
        assertThat(rolletilgangskontrollFeil.frontendFeilmelding).isEqualTo("Fagsaken inneholder personer som krever ytterligere tilganger. Bruker mangler rollen '0000-GA-Strengt_Fortrolig_Adresse'.")
    }

    @Test
    fun `validerTilgangTilFagsak - skal kaste feil dersom søker og barn har diskresjonskode, fagsak av typen skjermet barn og ikke har behandling`() {
        // Arrange
        val søkerAktør = randomAktør("65434563721")
        val barnAktør = randomAktør("12345678910")
        val skjermetFagsak =
            defaultFagsak(aktør = barnAktør)
                .copy(skjermetBarnSøker = SkjermetBarnSøker(id = 1, søkerAktør), type = FagsakType.SKJERMET_BARN)
        every { fagsakService.hentAktør(skjermetFagsak.id) }.returns(barnAktør)
        every { fagsakService.hentPåFagsakId(skjermetFagsak.id) }.returns(skjermetFagsak)
        every { behandlingHentOgPersisterService.hentBehandlinger(fagsak.id) }.returns(emptyList())
        every { persongrunnlagService.hentSøkerOgBarnPåFagsak(fagsak.id) }.returns(
            emptyList<PersonEnkel>().toSet(),
        )

        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(
            listOf(
                Tilgang(
                    søkerAktør.aktivFødselsnummer(),
                    false,
                    "Bruker mangler rollen '0000-GA-Strengt_Fortrolig_Adresse'",
                ),
            ),
        )
        mockBrukerContext("A")

        // Act & Assert
        val rolletilgangskontrollFeil =
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilFagsak(
                    skjermetFagsak.id,
                    AuditLoggerEvent.ACCESS,
                )
            }
        assertThat(rolletilgangskontrollFeil.message).isEqualTo("Saksbehandler A har ikke tilgang til fagsak=${fagsak.id}. Bruker mangler rollen '0000-GA-Strengt_Fortrolig_Adresse'.")
        assertThat(rolletilgangskontrollFeil.frontendFeilmelding).isEqualTo("Fagsaken inneholder personer som krever ytterligere tilganger. Bruker mangler rollen '0000-GA-Strengt_Fortrolig_Adresse'.")
        assertThat(fakeFamilieIntegrasjonerTilgangskontrollKlient.hentKallMotSjekkTilgangTilPersoner())
            .hasSize(1)
            .containsOnly(listOf(barnAktør.aktivFødselsnummer(), søkerAktør.aktivFødselsnummer()))
    }

    @Test
    fun `skal feile hvis man mangler tilgang til en ident`() {
        val fnr = randomFnr()
        val fnr2 = randomFnr()
        val fnr3 = randomFnr()
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(
            listOf(
                Tilgang(fnr, true),
                Tilgang(fnr2, false),
                Tilgang(fnr3, true),
            ),
        )
        assertThrows<RolleTilgangskontrollFeil> {
            tilgangService.validerTilgangTilPersoner(
                listOf(fnr, fnr2, fnr3),
                AuditLoggerEvent.ACCESS,
            )
        }
    }

    @Nested
    inner class SkjermedePersonerUtenLøpendeAndelerTest {
        private val barnAktør = randomAktør()
        private val søkerAktør = randomAktør()
        private val testFagsak =
            lagFagsak(
                aktør = søkerAktør,
                type = FagsakType.NORMAL,
            )
        private val testBehandling = lagBehandling(testFagsak)

        @BeforeEach
        fun setUp() {
            every { fagsakService.hentAktør(testFagsak.id) } returns søkerAktør
            every { fagsakService.hentPåFagsakId(testFagsak.id) } returns testFagsak
            every { persongrunnlagService.hentSøkerOgBarnPåFagsak(testFagsak.id) } returns
                setOf(
                    PersonEnkel(aktør = søkerAktør, type = PersonType.SØKER, fødselsdato = LocalDate.now(), dødsfallDato = null, målform = Målform.NB),
                    PersonEnkel(aktør = barnAktør, type = PersonType.BARN, fødselsdato = LocalDate.now(), dødsfallDato = null, målform = Målform.NB),
                )
            every { personopplysningGrunnlagRepository.finnSøkerOgBarnAktørerTilFagsak(testFagsak.id) } returns
                setOf(
                    PersonEnkel(aktør = søkerAktør, type = PersonType.SØKER, fødselsdato = LocalDate.now(), dødsfallDato = null, målform = Målform.NB),
                    PersonEnkel(aktør = barnAktør, type = PersonType.BARN, fødselsdato = LocalDate.now(), dødsfallDato = null, målform = Målform.NB),
                )
            every { featureToggleService.isEnabled(FeatureToggle.TILLAT_TILGANG_SKJERMET_BARN_UTEN_LØPENDE_ANDELER) } returns true
            mockBrukerContext("A")
        }

        @AfterEach
        fun tearDown() {
            clearBrukerContext()
        }

        @Test
        fun `validerTilgangTilFagsak - skal tillate tilgang til saksbehandler uten strengt fortrolig tilgang ved skjermet barn uten løpende andeler`() {
            // Arrange
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(testFagsak.id) } returns testBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(testBehandling.id) } returns
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2024, 12),
                        behandling = testBehandling,
                        aktør = barnAktør,
                    ),
                )

            fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(
                listOf(
                    Tilgang(barnAktør.aktivFødselsnummer(), false, "Bruker mangler rollen '0000-GA-Strengt_Fortrolig_Adresse'"),
                    Tilgang(søkerAktør.aktivFødselsnummer(), true),
                ),
            )

            // Act && Assert
            assertDoesNotThrow {
                tilgangService.validerTilgangTilFagsak(testFagsak.id, AuditLoggerEvent.ACCESS)
            }
        }

        @Test
        fun `validerTilgangTilFagsak - skal blokkere tilgang til saksbehandler uten strengt fortrolig tilgang ved skjermet barn med løpende andeler`() {
            // Arrange
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(testFagsak.id) } returns testBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(testBehandling.id) } returns
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.now().plusYears(1),
                        behandling = testBehandling,
                        aktør = barnAktør,
                    ),
                )

            fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(
                listOf(
                    Tilgang(barnAktør.aktivFødselsnummer(), false, "Bruker mangler rollen '0000-GA-Strengt_Fortrolig_Adresse'"),
                    Tilgang(søkerAktør.aktivFødselsnummer(), true),
                ),
            )

            // Act && Assert
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilFagsak(testFagsak.id, AuditLoggerEvent.ACCESS)
            }
        }

        @Test
        fun `validerTilgangTilFagsak - skal blokkere tilgang uavhengig av årsak hvis toggle er deaktivert`() {
            // Arrange
            every { featureToggleService.isEnabled(FeatureToggle.TILLAT_TILGANG_SKJERMET_BARN_UTEN_LØPENDE_ANDELER) } returns false

            fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(
                listOf(
                    Tilgang(barnAktør.aktivFødselsnummer(), false, "Bruker mangler rollen '0000-GA-Strengt_Fortrolig_Adresse'"),
                    Tilgang(søkerAktør.aktivFødselsnummer(), true),
                ),
            )

            // Act && Assert
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilFagsak(testFagsak.id, AuditLoggerEvent.ACCESS)
            }
        }

        @Test
        fun `validerTilgangTilFagsak - skal blokkere tilgang uavhengig av årsak hvis ingen behandling er iverksatt på fagsaken`() {
            // Arrange
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(testFagsak.id) } returns null

            fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(
                listOf(
                    Tilgang(barnAktør.aktivFødselsnummer(), false, "Bruker mangler rollen '0000-GA-Strengt_Fortrolig_Adresse'"),
                    Tilgang(søkerAktør.aktivFødselsnummer(), true),
                ),
            )

            // Act && Assert
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilFagsak(testFagsak.id, AuditLoggerEvent.ACCESS)
            }
        }

        @Test
        fun `validerTilgangTilFagsak - skal blokkere tilgang når både søker og barn er skjermet, selv om barn ikke har løpende andeler`() {
            // Arrange
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(testFagsak.id) } returns testBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(testBehandling.id) } returns
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2024, 12),
                        behandling = testBehandling,
                        aktør = barnAktør,
                    ),
                )

            fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(
                listOf(
                    Tilgang(barnAktør.aktivFødselsnummer(), false, "Bruker mangler rollen '0000-GA-Strengt_Fortrolig_Adresse'"),
                    Tilgang(søkerAktør.aktivFødselsnummer(), false, "Bruker mangler rollen '0000-GA-Strengt_Fortrolig_Adresse'"),
                ),
            )

            // Act && Assert
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilFagsak(testFagsak.id, AuditLoggerEvent.ACCESS)
            }
        }

        @Test
        fun `validerTilgangTilFagsak - skal blokkere tilgang når barn er stanses av annen årsak enn strengt fortrolig`() {
            // Arrange
            fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(
                listOf(
                    Tilgang(barnAktør.aktivFødselsnummer(), false, "NAV-ansatt"),
                    Tilgang(søkerAktør.aktivFødselsnummer(), true),
                ),
            )

            // Act && Assert
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilFagsak(testFagsak.id, AuditLoggerEvent.ACCESS)
            }
        }

        @Test
        fun `validerTilgangTilBehandling - skal tillate tilgang til behandling hvis skjermet barn ikke har løpende andeler`() {
            // Arrange
            every { behandlingHentOgPersisterService.hent(testBehandling.id) } returns testBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(testFagsak.id) } returns testBehandling
            every { persongrunnlagService.hentSøkerOgBarnPåBehandling(testBehandling.id) } returns
                listOf(
                    PersonEnkel(aktør = barnAktør, type = PersonType.BARN, fødselsdato = LocalDate.now(), dødsfallDato = null, målform = Målform.NB),
                    PersonEnkel(aktør = søkerAktør, type = PersonType.SØKER, fødselsdato = LocalDate.now(), dødsfallDato = null, målform = Målform.NB),
                )
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(testBehandling.id) } returns
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2024, 12),
                        behandling = testBehandling,
                        aktør = barnAktør,
                    ),
                )

            fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(
                listOf(
                    Tilgang(barnAktør.aktivFødselsnummer(), false, "Bruker mangler rollen '0000-GA-Strengt_Fortrolig_Adresse'"),
                    Tilgang(søkerAktør.aktivFødselsnummer(), true),
                ),
            )

            // Act && Assert
            assertDoesNotThrow {
                tilgangService.validerTilgangTilBehandling(testBehandling.id, AuditLoggerEvent.ACCESS)
            }
        }
    }
}
