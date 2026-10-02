package no.nav.familie.ba.sak.integrasjoner.tilgangsmaskin

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.BEGRUNNELSE_STRENGT_FORTROLIG_FRA_TILGANGSMASKINEN
import no.nav.familie.ba.sak.datagenerator.lagPersonTilgangAvvistGrunnetStrengtFortrolig
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonException
import no.nav.familie.ba.sak.sikkerhet.PersonTilgang
import no.nav.familie.ba.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ba.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.tilgangsmaskin.Avvisningskode
import no.nav.familie.tilgangsmaskin.Regeltype
import no.nav.familie.tilgangsmaskin.TilgangsmaskinException
import no.nav.familie.tilgangsmaskin.TilgangsmaskinKlient
import no.nav.familie.tilgangsmaskin.TilgangsmaskinResultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import java.net.URI

class TilgangsmaskinTilgangskontrollKlientTest {
    private val tilgangsmaskinKlient = mockk<TilgangsmaskinKlient>()
    private val tilgangsmaskinTilgangskontrollKlient =
        TilgangsmaskinTilgangskontrollKlient(URI("http://tilgangsmaskin-url"), tilgangsmaskinKlient)

    @BeforeEach
    fun setUp() {
        mockBrukerContext()
    }

    @AfterEach
    fun tearDown() {
        clearBrukerContext()
    }

    @Test
    fun `skal gi tilgang uten å kalle Tilgangsmaskinen i systemkontekst`() {
        // Arrange
        clearBrukerContext()

        // Act
        val tilganger = tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT, PERSONIDENT_2))

        // Assert
        assertThat(tilganger).containsExactly(PersonTilgang.medTilgang(PERSONIDENT), PersonTilgang.medTilgang(PERSONIDENT_2))
        verify(exactly = 0) { tilgangsmaskinKlient.sjekkTilgangTilPersoner(any(), any()) }
    }

    @Test
    fun `skal kalle Tilgangsmaskinen med identene og kjerneregeltypen`() {
        // Arrange
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT, PERSONIDENT_2), Regeltype.KJERNE_REGELTYPE) } returns
            listOf(harTilgang(PERSONIDENT), harTilgang(PERSONIDENT_2))

        // Act
        tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT, PERSONIDENT_2))

        // Assert
        verify(exactly = 1) { tilgangsmaskinKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT, PERSONIDENT_2), Regeltype.KJERNE_REGELTYPE) }
    }

    @Test
    fun `skal mappe svar fra Tilgangsmaskinen til tilganger med avvisningskode og begrunnelse`() {
        // Arrange
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT, PERSONIDENT_2), Regeltype.KJERNE_REGELTYPE) } returns
            listOf(
                harTilgang(PERSONIDENT),
                TilgangsmaskinResultat(
                    personIdent = PERSONIDENT_2,
                    harTilgang = false,
                    httpStatus = 403,
                    avvisningskode = Avvisningskode.AVVIST_STRENGT_FORTROLIG_ADRESSE,
                    begrunnelse = BEGRUNNELSE_STRENGT_FORTROLIG_FRA_TILGANGSMASKINEN,
                    traceId = TRACE_ID,
                ),
            )

        // Act
        val tilganger = tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT, PERSONIDENT_2))

        // Assert
        assertThat(tilganger).containsExactly(
            PersonTilgang.medTilgang(PERSONIDENT),
            lagPersonTilgangAvvistGrunnetStrengtFortrolig(PERSONIDENT_2),
        )
    }

    @Test
    fun `skal gi en standard begrunnelse når Tilgangsmaskinen avviser uten begrunnelse`() {
        // Arrange
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT), Regeltype.KJERNE_REGELTYPE) } returns
            listOf(
                TilgangsmaskinResultat(
                    personIdent = PERSONIDENT,
                    harTilgang = false,
                    httpStatus = 403,
                    avvisningskode = Avvisningskode.UKJENT,
                ),
            )

        // Act
        val tilgang = tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT)).single()

        // Assert
        assertThat(tilgang).isEqualTo(
            PersonTilgang.avvist(
                personIdent = PERSONIDENT,
                avvisningskode = Avvisningskode.UKJENT,
                begrunnelse = "Tilgangsmaskinen ga ikke tilgang til personen (HTTP 403)",
            ),
        )
    }

    @Test
    fun `skal kaste feil når Tilgangsmaskinen ikke svarer for en ident`() {
        // Arrange
        // Klienten fyller inn dette syntetiske avslaget for identer Tilgangsmaskinen ikke svarte for.
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT, PERSONIDENT_2), Regeltype.KJERNE_REGELTYPE) } returns
            listOf(
                harTilgang(PERSONIDENT),
                TilgangsmaskinResultat(
                    personIdent = PERSONIDENT_2,
                    harTilgang = false,
                    httpStatus = 500,
                    avvisningskode = Avvisningskode.UKJENT,
                    begrunnelse = "Fikk ikke svar fra Tilgangsmaskinen for personen",
                ),
            )

        // Act
        val feil =
            assertThrows<Feil> {
                tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT, PERSONIDENT_2))
            }

        // Assert
        assertThat(feil.message).isEqualTo("Fikk ikke gyldig svar fra Tilgangsmaskinen for 1 av 2 identer. traceIder=[]")
        assertThat(feil.httpStatus).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(feil.frontendFeilmelding).isEqualTo("Klarte ikke å sjekke tilgang til personene. Prøv igjen senere.")
    }

    @Test
    fun `skal kaste feil med traceId når Tilgangsmaskinen svarer med serverfeil for en ident`() {
        // Arrange
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT), Regeltype.KJERNE_REGELTYPE) } returns
            listOf(TilgangsmaskinResultat(personIdent = PERSONIDENT, harTilgang = false, httpStatus = 503, traceId = TRACE_ID))

        // Act
        val feil =
            assertThrows<Feil> {
                tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT))
            }

        // Assert
        assertThat(feil.message).isEqualTo("Fikk ikke gyldig svar fra Tilgangsmaskinen for 1 av 1 identer. traceIder=[$TRACE_ID]")
        assertThat(feil.message).doesNotContain(PERSONIDENT)
        assertThat(feil.httpStatus).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `skal kaste feil når svaret fra Tilgangsmaskinen mangler en ident`() {
        // Arrange
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT, PERSONIDENT_2), Regeltype.KJERNE_REGELTYPE) } returns
            listOf(harTilgang(PERSONIDENT))

        // Act
        val feil =
            assertThrows<Feil> {
                tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT, PERSONIDENT_2))
            }

        // Assert
        assertThat(feil.message).isEqualTo("Fikk ikke gyldig svar fra Tilgangsmaskinen for 1 av 2 identer. traceIder=[]")
    }

    @Test
    fun `skal returnere tom liste når det ikke er noen identer å sjekke`() {
        // Arrange
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(emptySet(), Regeltype.KJERNE_REGELTYPE) } returns emptyList()

        // Act
        val tilganger = tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(emptySet())

        // Assert
        assertThat(tilganger).isEmpty()
    }

    @Test
    fun `skal kaste feil uten å kalle Tilgangsmaskinen når en ident er blank`() {
        // Act
        val feil =
            assertThrows<Feil> {
                tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT, " "))
            }

        // Assert
        assertThat(feil.httpStatus).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        verify(exactly = 0) { tilgangsmaskinKlient.sjekkTilgangTilPersoner(any(), any()) }
    }

    @Test
    fun `skal kaste IntegrasjonException når kallet mot Tilgangsmaskinen feiler`() {
        // Arrange
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(any(), any()) } throws
            TilgangsmaskinException("Feil ved kall mot Tilgangsmaskinen: HTTP 500", httpStatus = 500)

        // Act & Assert
        assertThrows<IntegrasjonException> {
            tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT))
        }
    }

    private fun harTilgang(personIdent: String) = TilgangsmaskinResultat(personIdent = personIdent, harTilgang = true, httpStatus = 204)

    companion object {
        private const val PERSONIDENT = "12345678910"
        private const val PERSONIDENT_2 = "10987654321"
        private const val TRACE_ID = "trace-id-1"
    }
}
