package no.nav.familie.ba.sak.integrasjoner.infotrygd

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InfotrygdControllerTest {
    @MockK
    lateinit var personopplysningerService: PersonopplysningerService

    @MockK
    lateinit var mockFamilieIntegrasjonerTilgangskontrollClient: FamilieIntegrasjonerTilgangskontrollClient

    @MockK
    lateinit var infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient

    @MockK
    lateinit var personidentService: PersonidentService

    @InjectMockKs
    lateinit var infotrygdService: InfotrygdService

    lateinit var infotrygdController: InfotrygdController

    @BeforeAll
    fun init() {
        infotrygdController = InfotrygdController(infotrygdBarnetrygdClient, personidentService, infotrygdService)
    }

    @Test
    fun `hentInfotrygdsakerForSøker skal returnere ok dersom saksbehandler har tilgang`() {
        val fnr = "12345678910"

        every { personidentService.hentAktør(fnr) } returns tilAktør(fnr)
        every { mockFamilieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(any()) } answers {
            firstArg<List<Tilgang>>().map { Tilgang(it.personIdent, true) }
        }
        every {
            infotrygdBarnetrygdClient.hentSaker(
                any(),
                any(),
            )
        } returns InfotrygdSøkResponse(listOf(Sak(status = "IP")), emptyList())
        val respons = infotrygdController.hentInfotrygdsakerForSøker(Personident(fnr))

        Assertions.assertEquals(HttpStatus.OK, respons.statusCode)
        Assertions.assertEquals(true, respons.body?.data?.harTilgang)
        Assertions.assertEquals("IP", respons.body?.data?.saker!![0].status)
    }

    @Test
    fun `hentInfotrygdsakerForSøker skal returnere ok, men ha gradering satt, dersom saksbehandler ikke har tilgang`() {
        val fnr = "12345678910"

        every { personidentService.hentAktør(fnr) } returns tilAktør(fnr)
        every { mockFamilieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(any()) } answers {
            firstArg<List<Tilgang>>().map { Tilgang(it.personIdent, false) }
        }
        every { personopplysningerService.hentAdressebeskyttelseSomSystembruker(any()) } returns ADRESSEBESKYTTELSEGRADERING.FORTROLIG

        val respons = infotrygdController.hentInfotrygdsakerForSøker(Personident(fnr))

        Assertions.assertEquals(HttpStatus.OK, respons.statusCode)
        Assertions.assertEquals(false, respons.body?.data?.harTilgang)
        Assertions.assertEquals(ADRESSEBESKYTTELSEGRADERING.FORTROLIG, respons.body?.data?.adressebeskyttelsegradering)
    }
}
