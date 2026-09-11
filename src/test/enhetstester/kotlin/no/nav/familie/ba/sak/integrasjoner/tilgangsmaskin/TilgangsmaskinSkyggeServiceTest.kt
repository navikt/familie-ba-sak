package no.nav.familie.ba.sak.integrasjoner.tilgangsmaskin

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ba.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.tilgangsmaskin.Avvisningskode
import no.nav.familie.tilgangsmaskin.Regeltype
import no.nav.familie.tilgangsmaskin.TilgangsmaskinException
import no.nav.familie.tilgangsmaskin.TilgangsmaskinKlient
import no.nav.familie.tilgangsmaskin.TilgangsmaskinResultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.slf4j.LoggerFactory

class TilgangsmaskinSkyggeServiceTest {
    private val tilgangsmaskinKlient = mockk<TilgangsmaskinKlient>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val tilgangsmaskinSkyggeService = TilgangsmaskinSkyggeService(tilgangsmaskinKlient, featureToggleService)

    private val åpenLogg = LoggerFactory.getLogger(TilgangsmaskinSkyggeService::class.java) as Logger
    private val listAppender = ListAppender<ILoggingEvent>()
    private var opprinneligLoggnivå: Level? = null

    private val meterRegistry = SimpleMeterRegistry()

    @BeforeEach
    fun setUp() {
        mockBrukerContext()
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_SKYGGEKJØRE_TILGANGSMASKINEN) } returns true
        opprinneligLoggnivå = åpenLogg.level
        åpenLogg.level = Level.INFO
        listAppender.start()
        åpenLogg.addAppender(listAppender)
        Metrics.addRegistry(meterRegistry)
    }

    @AfterEach
    fun tearDown() {
        Metrics.removeRegistry(meterRegistry)
        meterRegistry.close()
        åpenLogg.detachAppender(listAppender)
        åpenLogg.level = opprinneligLoggnivå
        listAppender.stop()
        clearBrukerContext()
    }

    @Test
    fun `skal ikke kalle Tilgangsmaskinen når toggelen er av`() {
        // Arrange
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_SKYGGEKJØRE_TILGANGSMASKINEN) } returns false

        // Act
        tilgangsmaskinSkyggeService.skyggeSjekkTilgangTilPersoner(listOf(PERSONIDENT), tilganger(harTilgang = true))

        // Assert
        verify(exactly = 0) { tilgangsmaskinKlient.sjekkTilgangTilPersoner(any(), any()) }
    }

    @Test
    fun `skal ikke kalle Tilgangsmaskinen i systemkontekst`() {
        // Arrange
        clearBrukerContext()

        // Act
        tilgangsmaskinSkyggeService.skyggeSjekkTilgangTilPersoner(listOf(PERSONIDENT), tilganger(harTilgang = true))

        // Assert
        verify(exactly = 0) { tilgangsmaskinKlient.sjekkTilgangTilPersoner(any(), any()) }
    }

    @Test
    fun `skal kalle Tilgangsmaskinen med identene som unikt sett og kjerneregeltypen`() {
        // Arrange
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT), Regeltype.KJERNE_REGELTYPE) } returns
            listOf(TilgangsmaskinResultat(personIdent = PERSONIDENT, harTilgang = true, httpStatus = 204))

        // Act
        tilgangsmaskinSkyggeService.skyggeSjekkTilgangTilPersoner(
            listOf(PERSONIDENT, PERSONIDENT),
            tilganger(harTilgang = true),
        )

        // Assert
        verify(exactly = 1) { tilgangsmaskinKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT), Regeltype.KJERNE_REGELTYPE) }
    }

    @Test
    fun `skal logge oppsummering uten personident når det ikke er avvik`() {
        // Arrange
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT), Regeltype.KJERNE_REGELTYPE) } returns
            listOf(TilgangsmaskinResultat(personIdent = PERSONIDENT, harTilgang = true, httpStatus = 204))

        // Act
        tilgangsmaskinSkyggeService.skyggeSjekkTilgangTilPersoner(listOf(PERSONIDENT), tilganger(harTilgang = true))

        // Assert
        val meldinger = listAppender.list.map { it.formattedMessage }
        assertThat(meldinger.single()).contains("ingen avvik")
        assertThat(meldinger.single()).doesNotContain(PERSONIDENT)
    }

    @Test
    fun `skal ikke telle manglende svar fra Tilgangsmaskinen som avvik, men varsle med kun antall`() {
        // Arrange
        // Klienten fyller inn dette syntetiske avslaget for identer Tilgangsmaskinen ikke svarte for.
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT), Regeltype.KJERNE_REGELTYPE) } returns
            listOf(
                TilgangsmaskinResultat(
                    personIdent = PERSONIDENT,
                    harTilgang = false,
                    httpStatus = 500,
                    avvisningskode = Avvisningskode.UKJENT,
                    begrunnelse = "Fikk ikke svar fra Tilgangsmaskinen for personen",
                ),
            )

        // Act
        tilgangsmaskinSkyggeService.skyggeSjekkTilgangTilPersoner(listOf(PERSONIDENT), tilganger(harTilgang = true))

        // Assert
        val advarsler = listAppender.list.filter { it.level == Level.WARN }.map { it.formattedMessage }
        assertThat(advarsler.single()).contains("fikk ikke svar for 1 av 1 identer")
        assertThat(advarsler.single()).doesNotContain(PERSONIDENT, "hadde avvik")
        assertThat(tellerVerdi("tilgangsmaskin.skygge.manglende.svar")).isEqualTo(1.0)
        assertThat(tellerVerdi("tilgangsmaskin.skygge.sammenlignet")).isEqualTo(0.0)
        assertThat(meterRegistry.find("tilgangsmaskin.skygge.avvik").counters().sumOf { it.count() }).isEqualTo(0.0)
    }

    @Test
    fun `skal telle avvik som ny-strengere med avvisningskode når Tilgangsmaskinen avviser`() {
        // Arrange
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT), Regeltype.KJERNE_REGELTYPE) } returns
            listOf(
                TilgangsmaskinResultat(
                    personIdent = PERSONIDENT,
                    harTilgang = false,
                    httpStatus = 403,
                    avvisningskode = Avvisningskode.AVVIST_SKJERMING,
                ),
            )

        // Act
        tilgangsmaskinSkyggeService.skyggeSjekkTilgangTilPersoner(listOf(PERSONIDENT), tilganger(harTilgang = true))

        // Assert
        assertThat(tellerVerdi("tilgangsmaskin.skygge.sammenlignet")).isEqualTo(1.0)
        assertThat(
            tellerVerdi(
                "tilgangsmaskin.skygge.avvik",
                "retning",
                "ny-strengere",
                "avvisningskode",
                "AVVIST_SKJERMING",
            ),
        ).isEqualTo(1.0)
    }

    @Test
    fun `skal telle avvik som ny-mildere uten avvisningskode når Tilgangsmaskinen gir tilgang`() {
        // Arrange
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT), Regeltype.KJERNE_REGELTYPE) } returns
            listOf(TilgangsmaskinResultat(personIdent = PERSONIDENT, harTilgang = true, httpStatus = 204))

        // Act
        tilgangsmaskinSkyggeService.skyggeSjekkTilgangTilPersoner(listOf(PERSONIDENT), tilganger(harTilgang = false))

        // Assert
        assertThat(
            tellerVerdi(
                "tilgangsmaskin.skygge.avvik",
                "retning",
                "ny-mildere",
                "avvisningskode",
                "INGEN",
            ),
        ).isEqualTo(1.0)
    }

    @Test
    fun `skal svelge unntak fra Tilgangsmaskinen slik at tilgangskontrollen ikke påvirkes`() {
        // Arrange
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(any(), any()) } throws
            TilgangsmaskinException("Feil ved kall mot Tilgangsmaskinen: HTTP 500", httpStatus = 500)

        // Act & Assert
        assertDoesNotThrow {
            tilgangsmaskinSkyggeService.skyggeSjekkTilgangTilPersoner(listOf(PERSONIDENT), tilganger(harTilgang = true))
        }
    }

    @Test
    fun `skal telle feil mot Tilgangsmaskinen med feiltype og httpStatus`() {
        // Arrange
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(any(), any()) } throws
            TilgangsmaskinException("Feil ved kall mot Tilgangsmaskinen: HTTP 503", httpStatus = 503)

        // Act
        tilgangsmaskinSkyggeService.skyggeSjekkTilgangTilPersoner(listOf(PERSONIDENT), tilganger(harTilgang = true))

        // Assert
        assertThat(
            tellerVerdi(
                "tilgangsmaskin.skygge.feilet",
                "feiltype",
                "TilgangsmaskinException",
                "httpStatus",
                "503",
            ),
        ).isEqualTo(1.0)
        val advarsel =
            listAppender.list
                .filter { it.level == Level.WARN }
                .map { it.formattedMessage }
                .single()
        assertThat(advarsel).contains("TilgangsmaskinException", "HTTP 503")
        assertThat(advarsel).doesNotContain(PERSONIDENT)
    }

    private fun tilganger(harTilgang: Boolean): Map<String, Tilgang> = mapOf(PERSONIDENT to Tilgang(PERSONIDENT, harTilgang))

    private fun tellerVerdi(
        navn: String,
        vararg tags: String,
    ): Double =
        meterRegistry
            .find(navn)
            .tags(*tags)
            .counter()
            ?.count() ?: 0.0

    companion object {
        private const val PERSONIDENT = "1234567891234"
    }
}
