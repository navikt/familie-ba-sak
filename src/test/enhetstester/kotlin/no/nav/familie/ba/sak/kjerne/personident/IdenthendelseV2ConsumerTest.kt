package no.nav.familie.ba.sak.kjerne.personident

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.mockk.mockk
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IdenthendelseV2ConsumerTest {
    private val consumer = IdenthendelseV2Consumer(personidentService = mockk())

    private lateinit var listAppender: ListAppender<ILoggingEvent>

    @BeforeEach
    fun settOppLogglytter() {
        listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        (IdenthendelseV2Consumer.log as Logger).addAppender(listAppender)
    }

    @AfterEach
    fun ryddOppLogglytter() {
        (IdenthendelseV2Consumer.log as Logger).detachAppender(listAppender)
    }

    @Test
    fun `skal logge warn når identhendelsen inneholder ukjent identtype`() {
        // Arrange
        val aktør = Aktor(listOf(Identifikator("1234567890123", Type.UKJENT, true)))

        // Act
        consumer.loggHvisUkjentIdenttype(aktør)

        // Assert
        assertTrue(listAppender.list.any { it.formattedMessage.contains("ukjent identtype") })
    }

    @Test
    fun `skal ikke logge warn når identhendelsen kun har kjente identtyper`() {
        // Arrange
        val aktør = Aktor(listOf(Identifikator("1234567890123", Type.AKTORID, true)))

        // Act
        consumer.loggHvisUkjentIdenttype(aktør)

        // Assert
        assertTrue(listAppender.list.isEmpty())
    }
}
