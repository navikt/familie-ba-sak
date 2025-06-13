package no.nav.familie.ba.sak.task

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.minside.MinsideAktiveringKafkaProducer
import no.nav.familie.ba.sak.kjerne.minside.MinsideAktiveringService
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.task.AktiverMinsideTask.Companion.TASK_STEP_TYPE
import no.nav.familie.ba.sak.task.dto.AktiverMinsideDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AktiverMinsideTaskTest {
    private val minsideAktiveringKafkaProducer: MinsideAktiveringKafkaProducer = mockk()
    private val minsideAktiveringService: MinsideAktiveringService = mockk()
    private val aktørIdRepository: AktørIdRepository = mockk()
    private val aktiverMinsideTask =
        AktiverMinsideTask(
            minsideAktiveringKafkaProducer = minsideAktiveringKafkaProducer,
            minsideAktiveringService = minsideAktiveringService,
            aktørIdRepository = aktørIdRepository,
        )

    @Nested
    inner class DoTask {
        @Test
        fun `skal kaste feil dersom aktør med aktørId ikke finnes`() {
            // Arrange
            val aktørId = "12345678901"
            val task =
                Task(
                    type = TASK_STEP_TYPE,
                    payload = objectMapper.writeValueAsString(AktiverMinsideDTO(aktørId)),
                )

            every { aktørIdRepository.findByAktørIdOrNull(aktørId) } returns null

            // Act & Assert
            val exception =
                assertThrows<Feil> {
                    aktiverMinsideTask.doTask(task)
                }

            assertThat(exception.message).isEqualTo("Aktør med aktørId $aktørId finnes ikke")
        }

        @Test
        fun `skal aktivere minside for aktør når minside ikke allerede er aktivert`() {
            // Arrange
            val aktør = randomAktør()
            val task =
                Task(
                    type = TASK_STEP_TYPE,
                    payload = objectMapper.writeValueAsString(AktiverMinsideDTO(aktør.aktørId)),
                )

            every { aktørIdRepository.findByAktørIdOrNull(aktør.aktørId) } returns aktør
            every { minsideAktiveringService.harAktivertMinsideAktivering(aktør) } returns false
            every { minsideAktiveringService.aktiverMinsideAktivering(aktør) } returns mockk()
            every { minsideAktiveringKafkaProducer.aktiver(aktør.aktivFødselsnummer()) } just Runs

            // Act
            aktiverMinsideTask.doTask(task)

            // Assert
            verify(exactly = 1) {
                aktørIdRepository.findByAktørIdOrNull(aktør.aktørId)
                minsideAktiveringService.harAktivertMinsideAktivering(aktør)
                minsideAktiveringService.aktiverMinsideAktivering(aktør)
                minsideAktiveringKafkaProducer.aktiver(aktør.aktivFødselsnummer())
            }
        }

        @Test
        fun `skal ikke aktivere minside for aktør når minside allerede er aktivert`() {
            // Arrange
            val aktør = randomAktør()
            val task =
                Task(
                    type = TASK_STEP_TYPE,
                    payload = objectMapper.writeValueAsString(AktiverMinsideDTO(aktør.aktørId)),
                )

            every { aktørIdRepository.findByAktørIdOrNull(aktør.aktørId) } returns aktør
            every { minsideAktiveringService.harAktivertMinsideAktivering(aktør) } returns true
            every { minsideAktiveringService.aktiverMinsideAktivering(aktør) } returns mockk()
            every { minsideAktiveringKafkaProducer.aktiver(aktør.aktivFødselsnummer()) } just Runs

            // Act
            aktiverMinsideTask.doTask(task)

            // Assert
            verify(exactly = 1) {
                aktørIdRepository.findByAktørIdOrNull(aktør.aktørId)
                minsideAktiveringService.harAktivertMinsideAktivering(aktør)
            }
            verify(exactly = 0) {
                minsideAktiveringService.aktiverMinsideAktivering(aktør)
                minsideAktiveringKafkaProducer.aktiver(aktør.aktivFødselsnummer())
            }
        }
    }

    @Nested
    inner class OpprettTask {
        @Test
        fun `skal opprette task med riktig type og payload`() {
            // Arrange
            val aktørId = "12345678901"

            // Act
            val task = AktiverMinsideTask.opprettTask(aktørId)

            // Assert
            assertThat(task.type).isEqualTo(TASK_STEP_TYPE)
            assertThat(task.payload).isEqualTo(objectMapper.writeValueAsString(AktiverMinsideDTO(aktørId)))
        }
    }
}
