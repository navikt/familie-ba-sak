package no.nav.familie.ba.sak.sikkerhet

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.common.clearAllCaches
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.BEGRUNNELSE_SKJERMING_FRA_TILGANGSMASKINEN
import no.nav.familie.ba.sak.datagenerator.BEGRUNNELSE_STRENGT_FORTROLIG_FRA_TILGANGSMASKINEN
import no.nav.familie.ba.sak.datagenerator.defaultFagsak
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagPersonTilgangAvvistGrunnetSkjerming
import no.nav.familie.ba.sak.datagenerator.lagPersonTilgangAvvistGrunnetStrengtFortrolig
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.datagenerator.tilPersonEnkelSøkerOgBarn
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
import no.nav.familie.ba.sak.mock.FakeTilgangsmaskinTilgangskontrollKlient
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
    private val auditLogger = AuditLogger("familie-ba-sak")
    private val fakeTilgangsmaskinTilgangskontrollKlient = FakeTilgangsmaskinTilgangskontrollKlient()
    private val fakeFamilieIntegrasjonerTilgangskontrollKlient = FakeFamilieIntegrasjonerTilgangskontrollKlient()

    private val personTilgangService =
        PersonTilgangService(
            fakeTilgangsmaskinTilgangskontrollKlient,
            fakeFamilieIntegrasjonerTilgangskontrollKlient,
            featureToggleService,
            cacheManager,
            mockk(),
        )
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository = mockk(relaxed = true)
    private val strengtFortroligService =
        StrengtFortroligService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
            personTilgangService = personTilgangService,
            featureToggleService = featureToggleService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
        )
    private val tilgangService =
        TilgangService(
            personTilgangService = personTilgangService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            persongrunnlagService = persongrunnlagService,
            fagsakService = fagsakService,
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
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN) } returns true
        cacheManager.clearAllCaches()
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
        fakeTilgangsmaskinTilgangskontrollKlient.reset()
        fakeFamilieIntegrasjonerTilgangskontrollKlient.reset()
    }

    @Test
    internal fun `skal kaste RolleTilgangskontrollFeil dersom saksbehandler ikke har tilgang til person eller dets barn`() {
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(
            listOf(
                lagPersonTilgangAvvistGrunnetStrengtFortrolig(aktør.aktivFødselsnummer()),
            ),
        )

        val rolleTilgangskontrollFeil =
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilPersoner(
                    listOf(aktør.aktivFødselsnummer()),
                    AuditLoggerEvent.ACCESS,
                )
            }

        assertThat(rolleTilgangskontrollFeil.message).isEqualTo("Saksbehandler A har ikke tilgang. $BEGRUNNELSE_STRENGT_FORTROLIG_FRA_TILGANGSMASKINEN.")
        assertThat(rolleTilgangskontrollFeil.frontendFeilmelding).isEqualTo("Saksbehandler A har ikke tilgang. $BEGRUNNELSE_STRENGT_FORTROLIG_FRA_TILGANGSMASKINEN.")
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til person og dets barn`() {
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(
            listOf(
                PersonTilgang.medTilgang(aktør.aktivFødselsnummer()),
            ),
        )

        tilgangService.validerTilgangTilPersoner(listOf(aktør.aktivFødselsnummer()), AuditLoggerEvent.ACCESS)
    }

    @Test
    internal fun `skal kaste RolleTilgangskontrollFeil dersom saksbehandler ikke har tilgang til behandling`() {
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(
            listOf(
                lagPersonTilgangAvvistGrunnetSkjerming(aktør.aktivFødselsnummer()),
            ),
        )

        val rolleTilgangskontrollFeil =
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilBehandling(
                    behandling.id,
                    AuditLoggerEvent.ACCESS,
                )
            }
        assertThat(rolleTilgangskontrollFeil.message).isEqualTo("Saksbehandler A har ikke tilgang til behandling=${behandling.id}. $BEGRUNNELSE_SKJERMING_FRA_TILGANGSMASKINEN.")
        assertThat(rolleTilgangskontrollFeil.frontendFeilmelding).isEqualTo("Behandlingen inneholder personer som krever ytterligere tilganger. $BEGRUNNELSE_SKJERMING_FRA_TILGANGSMASKINEN.")
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til behandling`() {
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(
            listOf(
                PersonTilgang.medTilgang(aktør.aktivFødselsnummer()),
            ),
        )

        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
    }

    @Test
    internal fun `validerTilgangTilPersoner - hvis samme saksbehandler kaller skal den ha cachet`() {
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(
            listOf(
                PersonTilgang.medTilgang(olaIdent),
            ),
        )

        mockBrukerContext("A")
        tilgangService.validerTilgangTilPersoner(listOf(olaIdent), AuditLoggerEvent.ACCESS)
        tilgangService.validerTilgangTilPersoner(listOf(olaIdent), AuditLoggerEvent.ACCESS)
        assertThat(fakeTilgangsmaskinTilgangskontrollKlient.antallKallTilSjekkTilgangTilPersoner()).isEqualTo(1)
    }

    @Test
    internal fun `validerTilgangTilPersoner - hvis to ulike saksbehandler kaller skal den sjekke tilgang på nytt`() {
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(
            listOf(
                PersonTilgang.medTilgang(olaIdent),
            ),
        )
        mockBrukerContext("A")
        tilgangService.validerTilgangTilPersoner(listOf(olaIdent), AuditLoggerEvent.ACCESS)
        mockBrukerContext("B")
        tilgangService.validerTilgangTilPersoner(listOf(olaIdent), AuditLoggerEvent.ACCESS)
        assertThat(fakeTilgangsmaskinTilgangskontrollKlient.antallKallTilSjekkTilgangTilPersoner()).isEqualTo(2)
    }

    @Test
    internal fun `validerTilgangTilBehandling - hvis samme saksbehandler kaller skal den ha cachet`() {
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(
            listOf(
                PersonTilgang.medTilgang(aktør.aktivFødselsnummer()),
            ),
        )

        mockBrukerContext("A")

        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
        assertThat(fakeTilgangsmaskinTilgangskontrollKlient.antallKallTilSjekkTilgangTilPersoner()).isEqualTo(1)
    }

    @Test
    internal fun `validerTilgangTilBehandling - hvis to ulike saksbehandler kaller skal den sjekke tilgang på nytt`() {
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(
            listOf(
                PersonTilgang.medTilgang(aktør.aktivFødselsnummer()),
            ),
        )

        mockBrukerContext("A")
        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
        mockBrukerContext("B")
        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
        assertThat(fakeTilgangsmaskinTilgangskontrollKlient.antallKallTilSjekkTilgangTilPersoner()).isEqualTo(2)
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
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(
            listOf(
                lagPersonTilgangAvvistGrunnetStrengtFortrolig(søkerAktør.aktivFødselsnummer()),
                PersonTilgang.medTilgang(barnAktør.aktivFødselsnummer()),
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
        assertThat(rolletilgangskontrollFeil.message).isEqualTo("Saksbehandler A har ikke tilgang til fagsak=${fagsak.id}. $BEGRUNNELSE_STRENGT_FORTROLIG_FRA_TILGANGSMASKINEN.")
        assertThat(rolletilgangskontrollFeil.frontendFeilmelding).isEqualTo("Fagsaken inneholder personer som krever ytterligere tilganger. $BEGRUNNELSE_STRENGT_FORTROLIG_FRA_TILGANGSMASKINEN.")
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

        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(
            listOf(
                lagPersonTilgangAvvistGrunnetStrengtFortrolig(søkerAktør.aktivFødselsnummer()),
                lagPersonTilgangAvvistGrunnetStrengtFortrolig(barnAktør.aktivFødselsnummer()),
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
        assertThat(rolletilgangskontrollFeil.message).isEqualTo("Saksbehandler A har ikke tilgang til fagsak=${fagsak.id}. $BEGRUNNELSE_STRENGT_FORTROLIG_FRA_TILGANGSMASKINEN.")
        assertThat(rolletilgangskontrollFeil.frontendFeilmelding).isEqualTo("Fagsaken inneholder personer som krever ytterligere tilganger. $BEGRUNNELSE_STRENGT_FORTROLIG_FRA_TILGANGSMASKINEN.")
        assertThat(fakeTilgangsmaskinTilgangskontrollKlient.hentKallMotSjekkTilgangTilPersoner())
            .hasSize(1)
            .containsOnly(setOf(barnAktør.aktivFødselsnummer(), søkerAktør.aktivFødselsnummer()))
    }

    @Test
    fun `skal feile hvis man mangler tilgang til en ident`() {
        val fnr = randomFnr()
        val fnr2 = randomFnr()
        val fnr3 = randomFnr()
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(
            listOf(
                PersonTilgang.medTilgang(fnr),
                lagPersonTilgangAvvistGrunnetSkjerming(fnr2),
                PersonTilgang.medTilgang(fnr3),
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

            fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(
                listOf(
                    lagPersonTilgangAvvistGrunnetStrengtFortrolig(barnAktør.aktivFødselsnummer()),
                    PersonTilgang.medTilgang(søkerAktør.aktivFødselsnummer()),
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

            fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(
                listOf(
                    lagPersonTilgangAvvistGrunnetStrengtFortrolig(barnAktør.aktivFødselsnummer()),
                    PersonTilgang.medTilgang(søkerAktør.aktivFødselsnummer()),
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

            fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(
                listOf(
                    lagPersonTilgangAvvistGrunnetStrengtFortrolig(barnAktør.aktivFødselsnummer()),
                    PersonTilgang.medTilgang(søkerAktør.aktivFødselsnummer()),
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

            fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(
                listOf(
                    lagPersonTilgangAvvistGrunnetStrengtFortrolig(barnAktør.aktivFødselsnummer()),
                    PersonTilgang.medTilgang(søkerAktør.aktivFødselsnummer()),
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

            fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(
                listOf(
                    lagPersonTilgangAvvistGrunnetStrengtFortrolig(barnAktør.aktivFødselsnummer()),
                    lagPersonTilgangAvvistGrunnetStrengtFortrolig(søkerAktør.aktivFødselsnummer()),
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
            fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(
                listOf(
                    lagPersonTilgangAvvistGrunnetSkjerming(barnAktør.aktivFødselsnummer()),
                    PersonTilgang.medTilgang(søkerAktør.aktivFødselsnummer()),
                ),
            )

            // Act && Assert
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilFagsak(testFagsak.id, AuditLoggerEvent.ACCESS)
            }
        }

        @Test
        fun `validerTilgangTilFagsak - skal tillate tilgang ved skjermet barn uten løpende andeler når familie-integrasjoner avviser grunnet strengt fortrolig`() {
            // Arrange
            every { featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN) } returns false
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
                    Tilgang(
                        personIdent = barnAktør.aktivFødselsnummer(),
                        harTilgang = false,
                        begrunnelse = "Bruker mangler rollen '0000-GA-Strengt_Fortrolig_Adresse'",
                    ),
                    Tilgang(personIdent = søkerAktør.aktivFødselsnummer(), harTilgang = true),
                ),
            )

            // Act && Assert
            assertDoesNotThrow {
                tilgangService.validerTilgangTilFagsak(testFagsak.id, AuditLoggerEvent.ACCESS)
            }
        }

        @Test
        fun `validerTilgangTilFagsak - skal blokkere tilgang når familie-integrasjoner avviser barn av annen grunn enn strengt fortrolig`() {
            // Arrange
            every { featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN) } returns false
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
                    Tilgang(
                        personIdent = barnAktør.aktivFødselsnummer(),
                        harTilgang = false,
                        begrunnelse = "Bruker mangler rollen '0000-GA-Egne_ansatte'",
                    ),
                    Tilgang(personIdent = søkerAktør.aktivFødselsnummer(), harTilgang = true),
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

            fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(
                listOf(
                    lagPersonTilgangAvvistGrunnetStrengtFortrolig(barnAktør.aktivFødselsnummer()),
                    PersonTilgang.medTilgang(søkerAktør.aktivFødselsnummer()),
                ),
            )

            // Act && Assert
            assertDoesNotThrow {
                tilgangService.validerTilgangTilBehandling(testBehandling.id, AuditLoggerEvent.ACCESS)
            }
        }
    }
}
