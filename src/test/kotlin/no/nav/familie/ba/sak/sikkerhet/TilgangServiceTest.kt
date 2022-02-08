package no.nav.familie.ba.sak.sikkerhet

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.config.AuditLogger
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollClient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ba.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.log.mdc.MDCConstants
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

class TilgangServiceTest {

    private val mockFamilieIntegrasjonerTilgangskontrollClient: FamilieIntegrasjonerTilgangskontrollClient = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val cacheManager = ConcurrentMapCacheManager()
    private val kode6Gruppe = "kode6"
    private val kode7Gruppe = "kode7"
    private val rolleConfig = RolleConfig("", "", "", KODE6 = kode6Gruppe, KODE7 = kode7Gruppe)
    private val auditLogger = AuditLogger("familie-ba-sak")
    private val tilgangService =
        TilgangService(
            familieIntegrasjonerTilgangskontrollClient = mockFamilieIntegrasjonerTilgangskontrollClient,
            behandlingService = behandlingService,
            persongrunnlagService = persongrunnlagService,
            fagsakService = fagsakService,
            rolleConfig = rolleConfig,
            cacheManager = cacheManager,
            auditLogger = auditLogger
        )

    private val fagsak = defaultFagsak()
    private val behandling = lagBehandling(fagsak)
    private val aktør = fagsak.aktør
    private val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
        behandlingId = behandling.id,
        søkerPersonIdent = aktør.aktivFødselsnummer(),
        barnasIdenter = emptyList()
    )
    private val olaIdent = "4567"

    @BeforeEach
    internal fun setUp() {
        MDC.put(MDCConstants.MDC_CALL_ID, "00001111")
        mockBrukerContext()
        every { fagsakService.hentAktør(fagsak.id) } returns fagsak.aktør
        every { behandlingService.hent(any()) } returns behandling
        every { persongrunnlagService.hentAktiv(any()) } returns personopplysningGrunnlag
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
    }

    @Test
    internal fun `skal kaste RolleTilgangskontrollFeil dersom saksbehandler ikke har tilgang til person eller dets barn`() {
        every { mockFamilieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(any()) } returns Tilgang(false)

        assertThrows<RolleTilgangskontrollFeil> {
            tilgangService.validerTilgangTilPersoner(
                listOf(aktør.aktivFødselsnummer()),
                AuditLoggerEvent.ACCESS
            )
        }
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til person og dets barn`() {
        every { mockFamilieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(any()) } returns Tilgang(true)

        tilgangService.validerTilgangTilPersoner(listOf(aktør.aktivFødselsnummer()), AuditLoggerEvent.ACCESS)
    }

    @Test
    internal fun `skal kaste RolleTilgangskontrollFeil dersom saksbehandler ikke har tilgang til behandling`() {
        every { mockFamilieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(any()) } returns Tilgang(false)

        assertThrows<RolleTilgangskontrollFeil> {
            tilgangService.validerTilgangTilBehandling(
                behandling.id,
                AuditLoggerEvent.ACCESS
            )
        }
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til behandling`() {
        every { mockFamilieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(any()) } returns Tilgang(true)

        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
    }

    @Test
    internal fun `validerTilgangTilPersoner - hvis samme saksbehandler kaller skal den ha cachet`() {
        every { mockFamilieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(any()) } returns Tilgang(true)

        mockBrukerContext("A")
        tilgangService.validerTilgangTilPersoner(listOf(olaIdent), AuditLoggerEvent.ACCESS)
        tilgangService.validerTilgangTilPersoner(listOf(olaIdent), AuditLoggerEvent.ACCESS)
        verify(exactly = 1) {
            mockFamilieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(any())
        }
    }

    @Test
    internal fun `validerTilgangTilPersoner - hvis to ulike saksbehandler kaller skal den sjekke tilgang på nytt`() {
        every { mockFamilieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(any()) } returns Tilgang(true)

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
        every { mockFamilieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(any()) } returns Tilgang(true)

        mockBrukerContext("A")

        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)

        verify(exactly = 1) {
            mockFamilieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(any())
        }
    }

    @Test
    internal fun `validerTilgangTilBehandling - hvis to ulike saksbehandler kaller skal den sjekke tilgang på nytt`() {
        every { mockFamilieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(any()) } returns Tilgang(true)

        mockBrukerContext("A")
        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
        mockBrukerContext("B")
        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)

        verify(exactly = 2) {
            mockFamilieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(any())
        }
    }
}
