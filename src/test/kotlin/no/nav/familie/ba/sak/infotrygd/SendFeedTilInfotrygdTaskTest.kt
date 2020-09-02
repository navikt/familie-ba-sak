package no.nav.familie.ba.sak.infotrygd

import no.nav.familie.ba.sak.task.SendFeedTilInfotrygdTask
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev")
class SendFeedTilInfotrygdTaskTest {

    @Test
    fun `Legg til fødselsmelding til task`() {
        val fnrBarn = "12345678910"
        val testTask = SendFeedTilInfotrygdTask.opprettTask(fnrBarn)

        val infotrygdFeedDto = objectMapper.readValue(testTask.payload, InfotrygdFødselhendelsesFeedDto::class.java)

        Assertions.assertEquals(fnrBarn, infotrygdFeedDto.fnrBarn)
    }

}