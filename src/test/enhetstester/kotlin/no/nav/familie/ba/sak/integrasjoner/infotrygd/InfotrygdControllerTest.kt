package no.nav.familie.ba.sak.integrasjoner.infotrygd

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import no.nav.familie.ba.sak.common.clearAllCaches
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.mock.FamilieIntegrasjonerTilgangskontrollMock.Companion.mockSjekkTilgang
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Adressebeskyttelse
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
    private val familieIntegrasjonerTilgangskontrollClient = mockk<FamilieIntegrasjonerTilgangskontrollClient>()

    private val familieIntegrasjonerTilgangskontrollService = FamilieIntegrasjonerTilgangskontrollService(familieIntegrasjonerTilgangskontrollClient, cacheManager, systemOnlyPdlRestClient)

    private val infotrygdBarnetrygdClient = mockk<InfotrygdBarnetrygdClient>()
    private val personidentService = mockk<PersonidentService>()
    private val infotrygdService: InfotrygdService = InfotrygdService(infotrygdBarnetrygdClient, familieIntegrasjonerTilgangskontrollService, personidentService)
    private val infotrygdController = InfotrygdController(infotrygdBarnetrygdClient, personidentService, infotrygdService)

    @BeforeEach
    fun setUp() {
        cacheManager.clearAllCaches()
    }

    @Test
    fun `hentInfotrygdsakerForSøker skal returnere ok dersom saksbehandler har tilgang`() {
        val fnr = "12345678910"

        every { personidentService.hentAktør(fnr) } returns lagAktør(fnr)
        familieIntegrasjonerTilgangskontrollClient.mockSjekkTilgang(true)
        every {
            infotrygdBarnetrygdClient.hentSaker(
                any(),
                any(),
            )
        } returns InfotrygdSøkResponse(listOf(Sak(status = "IP")), emptyList())
        val respons = infotrygdController.hentInfotrygdsakerForSøker(Personident(fnr))

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
        val fnr = "12345678910"

        every { personidentService.hentAktør(fnr) } returns lagAktør(fnr)
        familieIntegrasjonerTilgangskontrollClient.mockSjekkTilgang(false)
        every { systemOnlyPdlRestClient.hentAdressebeskyttelse(any()) } returns
        every { systemOnlyPdlRestKlient.hentAdressebeskyttelse(any()) } returns
            listOf(Adressebeskyttelse(ADRESSEBESKYTTELSEGRADERING.FORTROLIG))

        val respons = infotrygdController.hentInfotrygdsakerForSøker(Personident(fnr))

        Assertions.assertEquals(HttpStatus.OK, respons.statusCode)
        Assertions.assertEquals(false, respons.body?.data?.harTilgang)
        Assertions.assertEquals(ADRESSEBESKYTTELSEGRADERING.FORTROLIG, respons.body?.data?.adressebeskyttelsegradering)
    }
}
