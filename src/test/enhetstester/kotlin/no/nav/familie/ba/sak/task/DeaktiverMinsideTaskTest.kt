package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.minside.MinsideAktiveringService
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.task.DeaktiverMinsideTask.Companion.TASK_STEP_TYPE
import no.nav.familie.ba.sak.task.dto.MinsideDTO
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DeaktiverMinsideTaskTest {
    private val minsideAktiveringService: MinsideAktiveringService = mockk()
    private val aktørIdRepository: AktørIdRepository = mockk()
    private val aktiverMinsideTask =
        DeaktiverMinsideTask(
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
                    payload = jsonMapper.writeValueAsString(MinsideDTO(aktørId)),
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
        fun `skal deaktivere minside for aktør når minside er aktivert`() {
            // Arrange
            val aktør = randomAktør()
            val task =
                Task(
                    type = TASK_STEP_TYPE,
                    payload = jsonMapper.writeValueAsString(MinsideDTO(aktør.aktørId)),
                )

            every { aktørIdRepository.findByAktørIdOrNull(aktør.aktørId) } returns aktør
            every { minsideAktiveringService.harAktivertMinsideAktivering(aktør) } returns true
            every { minsideAktiveringService.deaktiverMinsideAktivering(aktør) } returns mockk()

            // Act
            aktiverMinsideTask.doTask(task)

            // Assert
            verify(exactly = 1) {
                aktørIdRepository.findByAktørIdOrNull(aktør.aktørId)
                minsideAktiveringService.harAktivertMinsideAktivering(aktør)
                minsideAktiveringService.deaktiverMinsideAktivering(aktør)
            }
        }

        @Test
        fun `skal ikke gjøre noe dersom minside ikke er aktivert for aktør`() {
            // Arrange
            val aktør = randomAktør()
            val task =
                Task(
                    type = TASK_STEP_TYPE,
                    payload = jsonMapper.writeValueAsString(MinsideDTO(aktør.aktørId)),
                )

            every { aktørIdRepository.findByAktørIdOrNull(aktør.aktørId) } returns aktør
            every { minsideAktiveringService.harAktivertMinsideAktivering(aktør) } returns false

            // Act
            aktiverMinsideTask.doTask(task)

            // Assert
            verify(exactly = 1) {
                aktørIdRepository.findByAktørIdOrNull(aktør.aktørId)
                minsideAktiveringService.harAktivertMinsideAktivering(aktør)
            }

            verify(exactly = 0) {
                minsideAktiveringService.deaktiverMinsideAktivering(aktør)
            }
        }
    }

    @Nested
    inner class OpprettTask {
        @Test
        fun `skal opprette task med riktig type og payload`() {
            // Arrange
            val aktør = randomAktør("12345678901")

            // Act
            val task = DeaktiverMinsideTask.opprettTask(aktør)

            // Assert
            assertThat(task.type).isEqualTo(TASK_STEP_TYPE)
            assertThat(task.payload).isEqualTo(jsonMapper.writeValueAsString(MinsideDTO(aktør.aktørId)))
            assertThat(task.metadata["aktørId"]).isEqualTo(aktør.aktørId)
            assertThat(task.metadata["fnr"]).isEqualTo(aktør.aktivFødselsnummer())
        }
    }
}
