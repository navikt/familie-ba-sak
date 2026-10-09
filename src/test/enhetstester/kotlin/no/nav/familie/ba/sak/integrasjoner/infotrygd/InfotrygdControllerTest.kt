package no.nav.familie.ba.sak.integrasjoner.infotrygd

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import no.nav.familie.ba.sak.common.clearAllCaches
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagPersonTilgangAvvistGrunnetFortrolig
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.mock.FakeTilgangsmaskinTilgangskontrollKlient
import no.nav.familie.ba.sak.sikkerhet.PersonTilgang
import no.nav.familie.ba.sak.sikkerhet.PersonTilgangService
import no.nav.familie.ba.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ba.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Adressebeskyttelse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.http.HttpStatus

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InfotrygdControllerTest {
    private val systemOnlyPdlRestKlient = mockk<SystemOnlyPdlRestKlient>()
    private val cacheManager = spyk(ConcurrentMapCacheManager())
    private val fakeTilgangsmaskinTilgangskontrollKlient = FakeTilgangsmaskinTilgangskontrollKlient()

    private val personTilgangService =
        PersonTilgangService(
            fakeTilgangsmaskinTilgangskontrollKlient,
            mockk(),
            mockk<FeatureToggleService> { every { isEnabled(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN) } returns true },
            cacheManager,
            systemOnlyPdlRestKlient,
        )

    private val infotrygdBarnetrygdKlient = mockk<InfotrygdBarnetrygdKlient>()
    private val personidentService = mockk<PersonidentService>()
    private val infotrygdService: InfotrygdService = InfotrygdService(infotrygdBarnetrygdKlient, personTilgangService, personidentService)
    private val infotrygdController = InfotrygdController(infotrygdBarnetrygdKlient, personidentService, infotrygdService)

    @BeforeEach
    fun setUp() {
        mockBrukerContext()
        cacheManager.clearAllCaches()
    }

    @AfterEach
    fun tearDown() {
        clearBrukerContext()
        fakeTilgangsmaskinTilgangskontrollKlient.reset()
    }

    @Test
    fun `hentInfotrygdsakerForSøker skal returnere ok dersom saksbehandler har tilgang`() {
        // Arrange
        val fnr = "12345678910"

        every { personidentService.hentAktør(fnr) } returns lagAktør(fnr)
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(listOf(PersonTilgang.medTilgang(fnr)))
        every {
            infotrygdBarnetrygdKlient.hentSaker(
                any(),
                any(),
            )
        } returns InfotrygdSøkResponse(listOf(Sak(status = "IP")), emptyList())

        // Act
        val respons = infotrygdController.hentInfotrygdsakerForSøker(Personident(fnr))

        // Assert
        Assertions.assertEquals(HttpStatus.OK, respons.statusCode)
        Assertions.assertEquals(true, respons.body?.data?.harTilgang)
        Assertions.assertEquals(
            "IP",
            respons.body
                ?.data
                ?.saker!![0]
                .status,
        )
    }

    @Test
    fun `hentInfotrygdsakerForSøker skal returnere ok, men ha gradering satt, dersom saksbehandler ikke har tilgang`() {
        // Arrange
        val fnr = "12345678910"

        every { personidentService.hentAktør(fnr) } returns lagAktør(fnr)
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(listOf(lagPersonTilgangAvvistGrunnetFortrolig(fnr)))

        every { systemOnlyPdlRestKlient.hentAdressebeskyttelse(any()) } returns
            listOf(Adressebeskyttelse(ADRESSEBESKYTTELSEGRADERING.FORTROLIG))

        // Act
        val respons = infotrygdController.hentInfotrygdsakerForSøker(Personident(fnr))

        // Assert
        Assertions.assertEquals(HttpStatus.OK, respons.statusCode)
        Assertions.assertEquals(false, respons.body?.data?.harTilgang)
        Assertions.assertEquals(ADRESSEBESKYTTELSEGRADERING.FORTROLIG, respons.body?.data?.adressebeskyttelsegradering)
    }
}
