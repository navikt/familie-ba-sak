package no.nav.familie.ba.sak.sikkerhet

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.common.clearAllCaches
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.IntegrasjonClientMock.Companion.mockSjekkTilgang
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.datagenerator.defaultFagsak
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.datagenerator.tilPersonEnkelSøkerOgBarn
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonEnkel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ba.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.log.mdc.MDCConstants
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import java.time.LocalDate

class TilgangServiceTest {
    private val mockFamilieIntegrasjonerTilgangskontrollClient: FamilieIntegrasjonerTilgangskontrollClient = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val cacheManager = ConcurrentMapCacheManager()
    private val kode6Gruppe = "kode6"
    private val kode7Gruppe = "kode7"
    private val rolleConfig = RolleConfig("", "", "", FORVALTER_ROLLE = "", KODE6 = kode6Gruppe, KODE7 = kode7Gruppe)
    private val auditLogger = AuditLogger("familie-ba-sak")
    private val familieIntegrasjonerTilgangskontrollService =
        FamilieIntegrasjonerTilgangskontrollService(
            mockFamilieIntegrasjonerTilgangskontrollClient,
            cacheManager,
            mockk(),
        )
    private val tilgangService =
        TilgangService(
            familieIntegrasjonerTilgangskontrollService = familieIntegrasjonerTilgangskontrollService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            persongrunnlagService = persongrunnlagService,
            fagsakService = fagsakService,
            rolleConfig = rolleConfig,
            auditLogger = auditLogger,
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
        every { behandlingHentOgPersisterService.hent(any()) } returns behandling
        every { persongrunnlagService.hentSøkerOgBarnPåBehandling(behandling.id) } returns
            personopplysningGrunnlag.tilPersonEnkelSøkerOgBarn()
        cacheManager.clearAllCaches()
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
    }

    @Test
    internal fun `skal kaste RolleTilgangskontrollFeil dersom saksbehandler ikke har tilgang til person eller dets barn`() {
        mockFamilieIntegrasjonerTilgangskontrollClient.mockSjekkTilgang(harTilgang = false, begrunnelse = "Fortrolig adresse")

        val rolleTilgangskontrollFeil =
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilPersoner(
                    listOf(aktør.aktivFødselsnummer()),
                    AuditLoggerEvent.ACCESS,
                )
            }

        assertThat(rolleTilgangskontrollFeil.message).isEqualTo("Saksbehandler A har ikke tilgang. Mangler tilgang til: Fortrolig adresse.")
        assertThat(rolleTilgangskontrollFeil.frontendFeilmelding).isEqualTo("Saksbehandler A har ikke tilgang. Mangler tilgang til: Fortrolig adresse.")
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til person og dets barn`() {
        mockFamilieIntegrasjonerTilgangskontrollClient.mockSjekkTilgang(true)

        tilgangService.validerTilgangTilPersoner(listOf(aktør.aktivFødselsnummer()), AuditLoggerEvent.ACCESS)
    }

    @Test
    internal fun `skal kaste RolleTilgangskontrollFeil dersom saksbehandler ikke har tilgang til behandling`() {
        mockFamilieIntegrasjonerTilgangskontrollClient.mockSjekkTilgang(harTilgang = false, begrunnelse = "NAV-ansatt")

        val rolleTilgangskontrollFeil =
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilBehandling(
                    behandling.id,
                    AuditLoggerEvent.ACCESS,
                )
            }
        assertThat(rolleTilgangskontrollFeil.message).isEqualTo("Saksbehandler A har ikke tilgang til behandling=${behandling.id}. Mangler tilgang til: NAV-ansatt.")
        assertThat(rolleTilgangskontrollFeil.frontendFeilmelding).isEqualTo("Behandlingen inneholder personer som krever ytterligere tilganger. For å kunne se behandlingen kreves tilgang til: NAV-ansatt.")
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til behandling`() {
        mockFamilieIntegrasjonerTilgangskontrollClient.mockSjekkTilgang(true)

        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
    }

    @Test
    internal fun `validerTilgangTilPersoner - hvis samme saksbehandler kaller skal den ha cachet`() {
        mockFamilieIntegrasjonerTilgangskontrollClient.mockSjekkTilgang(true)

        mockBrukerContext("A")
        tilgangService.validerTilgangTilPersoner(listOf(olaIdent), AuditLoggerEvent.ACCESS)
        tilgangService.validerTilgangTilPersoner(listOf(olaIdent), AuditLoggerEvent.ACCESS)
        verify(exactly = 1) {
            mockFamilieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(any())
        }
    }

    @Test
    internal fun `validerTilgangTilPersoner - hvis to ulike saksbehandler kaller skal den sjekke tilgang på nytt`() {
        mockFamilieIntegrasjonerTilgangskontrollClient.mockSjekkTilgang(true)

        mockBrukerContext("A")
        tilgangService.validerTilgangTilPersoner(listOf(olaIdent), AuditLoggerEvent.ACCESS)
        mockBrukerContext("B")
        tilgangService.validerTilgangTilPersoner(listOf(olaIdent), AuditLoggerEvent.ACCESS)

        verify(exactly = 2) {
            mockFamilieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(any())
        }
    }

    @Test
    internal fun `validerTilgangTilBehandling - hvis samme saksbehandler kaller skal den ha cachet`() {
        mockFamilieIntegrasjonerTilgangskontrollClient.mockSjekkTilgang(true)

        mockBrukerContext("A")

        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)

        verify(exactly = 1) {
            mockFamilieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(any())
        }
    }

    @Test
    internal fun `validerTilgangTilBehandling - hvis to ulike saksbehandler kaller skal den sjekke tilgang på nytt`() {
        mockFamilieIntegrasjonerTilgangskontrollClient.mockSjekkTilgang(true)

        mockBrukerContext("A")
        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
        mockBrukerContext("B")
        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)

        verify(exactly = 2) {
            mockFamilieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(any())
        }
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
        mockFamilieIntegrasjonerTilgangskontrollClient.mockSjekkTilgang(harTilgang = false, begrunnelse = "Strengt fortrolig adresse")
        mockBrukerContext("A")

        // Act & Assert
        val rolletilgangskontrollFeil =
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilFagsak(
                    fagsak.id,
                    AuditLoggerEvent.ACCESS,
                )
            }
        assertThat(rolletilgangskontrollFeil.message).isEqualTo("Saksbehandler A har ikke tilgang til fagsak=${fagsak.id}. Mangler tilgang til: Strengt fortrolig adresse.")
        assertThat(rolletilgangskontrollFeil.frontendFeilmelding).isEqualTo("Fagsaken inneholder personer som krever ytterligere tilganger. For å kunne se fagsaken kreves tilgang til: Strengt fortrolig adresse.")
    }

    @Test
    fun `skal feile hvis man mangler tilgang til en ident`() {
        val fnr = randomFnr()
        val fnr2 = randomFnr()
        val fnr3 = randomFnr()
        mockFamilieIntegrasjonerTilgangskontrollClient.mockSjekkTilgang(mapOf(fnr to true, fnr2 to false, fnr3 to true))
        assertThrows<RolleTilgangskontrollFeil> {
            tilgangService.validerTilgangTilPersoner(
                listOf(fnr, fnr2, fnr3),
                AuditLoggerEvent.ACCESS,
            )
        }
    }
}
