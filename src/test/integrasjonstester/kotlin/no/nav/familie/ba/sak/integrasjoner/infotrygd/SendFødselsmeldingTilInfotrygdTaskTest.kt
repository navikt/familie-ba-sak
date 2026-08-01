package no.nav.familie.ba.sak.integrasjoner.infotrygd

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.InfotrygdFødselhendelsesFeedTaskDto
import no.nav.familie.ba.sak.task.SendFødselsmeldingTilInfotrygdTask
import no.nav.familie.kontrakter.felles.jsonMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SendFødselsmeldingTilInfotrygdTaskTest : AbstractSpringIntegrationTest() {
    @Test
    fun `Legg til fødselsmelding til task`() {
        // Arrange
        val fnrBarn = "12345678910"
        val testTask = SendFødselsmeldingTilInfotrygdTask.opprettTask(listOf(fnrBarn))

        // Act
        val infotrygdFeedDto = jsonMapper.readValue(testTask.payload, InfotrygdFødselhendelsesFeedTaskDto::class.java)

        // Assert
        Assertions.assertEquals(listOf(fnrBarn), infotrygdFeedDto.fnrBarn)
    }
}
