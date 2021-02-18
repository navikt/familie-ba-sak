package no.nav.familie.ba.sak.infotrygd

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OppgaveControllerTest {
    @MockK
    lateinit var personopplysningerService: PersonopplysningerService

    @MockK
    lateinit var integrasjonClient: IntegrasjonClient

    @MockK
    lateinit var infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient

    @InjectMockKs
    lateinit var infotrygdService: InfotrygdService

    lateinit var infotrygdController: InfotrygdController

    @BeforeAll
    fun init() {
        infotrygdController = InfotrygdController(infotrygdService)
    }

    @Test
    fun `hentInfotrygdsakerForSøker skal returnere ok dersom saksbehandler har tilgang`() {
        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang(true))
        every { infotrygdBarnetrygdClient.hentSaker(any(), any()) } returns InfotrygdSøkResponse(listOf(Sak(status = "IP")), emptyList())

        val respons = infotrygdController.hentInfotrygdsakerForSøker(Personident("12345"))

        Assertions.assertEquals(HttpStatus.OK, respons.statusCode)
        Assertions.assertEquals(true, respons.body?.data?.harTilgang)
        Assertions.assertEquals("IP", respons.body?.data?.saker!![0].status)
    }

    @Test
    fun `hentInfotrygdsakerForSøker skal returnere ok, men ha gradering satt, dersom saksbehandler ikke har tilgang`() {
        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang(false))
        every { personopplysningerService.hentAdressebeskyttelseSomSystembruker(any()) } returns ADRESSEBESKYTTELSEGRADERING.FORTROLIG

        val respons = infotrygdController.hentInfotrygdsakerForSøker(Personident("12345"))

        Assertions.assertEquals(HttpStatus.OK, respons.statusCode)
        Assertions.assertEquals(false, respons.body?.data?.harTilgang)
        Assertions.assertEquals(ADRESSEBESKYTTELSEGRADERING.FORTROLIG, respons.body?.data?.adressebeskyttelsegradering)
    }
}