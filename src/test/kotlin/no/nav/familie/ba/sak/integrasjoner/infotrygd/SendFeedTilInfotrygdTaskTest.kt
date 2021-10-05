package no.nav.familie.ba.sak.integrasjoner.infotrygd

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTestDev
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.InfotrygdFødselhendelsesFeedTaskDto
import no.nav.familie.ba.sak.task.SendFeedTilInfotrygdTask
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SendFeedTilInfotrygdTaskTest : AbstractSpringIntegrationTestDev() {

    @Test
    fun `Legg til fødselsmelding til task`() {
        val fnrBarn = "12345678910"
        val testTask = SendFeedTilInfotrygdTask.opprettTask(listOf(fnrBarn))

        val infotrygdFeedDto = objectMapper.readValue(testTask.payload, InfotrygdFødselhendelsesFeedTaskDto::class.java)

        Assertions.assertEquals(listOf(fnrBarn), infotrygdFeedDto.fnrBarn)
    }
}
