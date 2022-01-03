package no.nav.familie.ba.sak.sikkerhet

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ba.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

class TilgangServiceTest {

    private val integrasjonClient: IntegrasjonClient = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val cacheManager = ConcurrentMapCacheManager()
    private val kode6Gruppe = "kode6"
    private val kode7Gruppe = "kode7"
    private val rolleConfig = RolleConfig("", "", "", KODE6 = kode6Gruppe, KODE7 = kode7Gruppe)
    private val tilgangService =
        TilgangService(
            integrasjonClient = integrasjonClient,
            behandlingService = behandlingService,
            fagsakService = fagsakService,
            rolleConfig = rolleConfig,
            cacheManager = cacheManager
        )

    private val fagsak = defaultFagsak()
    private val behandling = lagBehandling(fagsak)
    private val aktør = fagsak.aktør
    private val olaIdent = "4567"

    @BeforeEach
    internal fun setUp() {
        mockBrukerContext("A")
        every { behandlingService.hentAktør(behandling.id) } returns fagsak.aktør
        every { fagsakService.hentAktør(fagsak.id) } returns fagsak.aktør
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
    }

    @Test
    internal fun `skal kaste RolleTilgangskontrollFeil dersom saksbehandler ikke har tilgang til person eller dets barn`() {
        every { integrasjonClient.sjekkTilgangTilPersonMedRelasjoner(any()) } returns Tilgang(false)

        assertThrows<RolleTilgangskontrollFeil> { tilgangService.validerTilgangTilPersonMedBarn(aktør.aktivFødselsnummer()) }
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til person og dets barn`() {
        every { integrasjonClient.sjekkTilgangTilPersonMedRelasjoner(any()) } returns Tilgang(true)

        tilgangService.validerTilgangTilPersonMedBarn(aktør.aktivFødselsnummer())
    }

    @Test
    internal fun `skal kaste RolleTilgangskontrollFeil dersom saksbehandler ikke har tilgang til behandling`() {
        every { integrasjonClient.sjekkTilgangTilPersonMedRelasjoner(any()) } returns Tilgang(false)

        assertThrows<RolleTilgangskontrollFeil> { tilgangService.validerTilgangTilBehandling(behandling.id) }
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til behandling`() {
        every { integrasjonClient.sjekkTilgangTilPersonMedRelasjoner(any()) } returns Tilgang(true)

        tilgangService.validerTilgangTilBehandling(behandling.id)
    }

    @Test
    internal fun `validerTilgangTilPersonMedBarn - hvis samme saksbehandler kaller skal den ha cachet`() {
        every { integrasjonClient.sjekkTilgangTilPersonMedRelasjoner(any()) } returns Tilgang(true)

        mockBrukerContext("A")
        tilgangService.validerTilgangTilPersonMedBarn(olaIdent)
        tilgangService.validerTilgangTilPersonMedBarn(olaIdent)
        verify(exactly = 1) {
            integrasjonClient.sjekkTilgangTilPersonMedRelasjoner(any())
        }
    }

    @Test
    internal fun `validerTilgangTilPersonMedBarn - hvis to ulike saksbehandler kaller skal den sjekke tilgang på nytt`() {
        every { integrasjonClient.sjekkTilgangTilPersonMedRelasjoner(any()) } returns Tilgang(true)

        mockBrukerContext("A")
        tilgangService.validerTilgangTilPersonMedBarn(olaIdent)
        mockBrukerContext("B")
        tilgangService.validerTilgangTilPersonMedBarn(olaIdent)

        verify(exactly = 2) {
            integrasjonClient.sjekkTilgangTilPersonMedRelasjoner(any())
        }
    }

    @Test
    internal fun `validerTilgangTilBehandling - hvis to ulike saksbehandler kaller skal den sjekke tilgang på nytt`() {
        every { integrasjonClient.sjekkTilgangTilPersonMedRelasjoner(any()) } returns Tilgang(true)

        mockBrukerContext("A")

        tilgangService.validerTilgangTilBehandling(behandling.id)
        tilgangService.validerTilgangTilBehandling(behandling.id)

        verify(exactly = 1) {
            behandlingService.hentAktør(behandling.id)
            integrasjonClient.sjekkTilgangTilPersonMedRelasjoner(any())
        }
    }

    @Test
    internal fun `validerTilgangTilBehandling - hvis samme saksbehandler kaller skal den ha cachet`() {
        every { integrasjonClient.sjekkTilgangTilPersonMedRelasjoner(any()) } returns Tilgang(true)

        mockBrukerContext("A")
        tilgangService.validerTilgangTilBehandling(behandling.id)
        mockBrukerContext("B")
        tilgangService.validerTilgangTilBehandling(behandling.id)

        verify(exactly = 2) {
            behandlingService.hentAktør(behandling.id)
            integrasjonClient.sjekkTilgangTilPersonMedRelasjoner(any())
        }
    }
}
