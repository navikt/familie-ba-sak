package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.ekstern.pensjon.FerdigstiltBehandlingForPensjonRepository
import no.nav.familie.ba.sak.ekstern.pensjon.kafka.FerdigstiltBehandlingForPensjonPayload
import no.nav.familie.ba.sak.ekstern.pensjon.kafka.PensjonKafkaProducer
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FerdigstiltBehandlingForPensjonTaskIntegrasjonTest(
    @Autowired private val ferdigstiltBehandlingForPensjonRepository: FerdigstiltBehandlingForPensjonRepository,
    @Autowired transactionManager: PlatformTransactionManager,
) : AbstractSpringIntegrationTest() {
    private val transactionTemplate = TransactionTemplate(transactionManager)
    private val ident = "12345678901"
    private val vedtaktidspunkt = LocalDateTime.of(2026, 10, 8, 12, 0)
    private val endringstidspunkt = LocalDate.of(2026, 10, 8)

    @Test
    fun `skal lagre ferdigstilt behandling med neste sekvensnummer og publisere den til kafka`() {
        // Arrange
        val behandlingId = -902001L
        val taskRepository = mockk<TaskRepositoryWrapper>()
        val pensjonKafkaProducer = mockk<PensjonKafkaProducer>()
        val lagreTaskSteg = lagLagreTaskSteg(taskRepository)
        val publiserTaskSteg = PubliserFerdigstiltBehandlingForPensjonTask(ferdigstiltBehandlingForPensjonRepository, pensjonKafkaProducer)
        val opprettetPubliserTask = slot<Task>()
        val forventetSekvensnummer = ferdigstiltBehandlingForPensjonRepository.finnHøyesteSekvensnummer() + 1
        every { taskRepository.save(capture(opprettetPubliserTask)) } answers { firstArg() }
        every { pensjonKafkaProducer.sendMeldingOmFerdigstiltBehandlingForPensjon(any()) } returns Unit

        try {
            // Act
            kjørILagreTransaksjon(lagreTaskSteg, behandlingId)
            publiserTaskSteg.doTask(opprettetPubliserTask.captured)

            // Assert
            val lagretFerdigstiltBehandling = ferdigstiltBehandlingForPensjonRepository.findByBehandlingId(behandlingId)
            assertThat(lagretFerdigstiltBehandling?.sekvensnummer).isEqualTo(forventetSekvensnummer)
            verify(exactly = 1) {
                pensjonKafkaProducer.sendMeldingOmFerdigstiltBehandlingForPensjon(
                    FerdigstiltBehandlingForPensjonPayload(ident, vedtaktidspunkt, endringstidspunkt, forventetSekvensnummer),
                )
            }
        } finally {
            slettLagredeFerdigstilteBehandlinger(behandlingId)
        }
    }

    @Test
    fun `skal kaste feil og ikke lagre på nytt dersom ferdigstilt behandling allerede er lagret`() {
        // Arrange
        val behandlingId = -902002L
        val taskRepository = mockk<TaskRepositoryWrapper>()
        val lagreTaskSteg = lagLagreTaskSteg(taskRepository)
        every { taskRepository.save(any()) } answers { firstArg() }

        try {
            kjørILagreTransaksjon(lagreTaskSteg, behandlingId)
            val høyesteSekvensnummerEtterFørsteLagring = ferdigstiltBehandlingForPensjonRepository.finnHøyesteSekvensnummer()

            // Act & Assert
            assertThrows<Feil> { kjørILagreTransaksjon(lagreTaskSteg, behandlingId) }
            assertThat(ferdigstiltBehandlingForPensjonRepository.finnHøyesteSekvensnummer()).isEqualTo(høyesteSekvensnummerEtterFørsteLagring)
            verify(exactly = 1) { taskRepository.save(any()) }
        } finally {
            slettLagredeFerdigstilteBehandlinger(behandlingId)
        }
    }

    @Test
    fun `skal rulle tilbake lagret ferdigstilt behandling uten hull i sekvensen når opprettelse av publiseringstask feiler`() {
        // Arrange
        val behandlingId = 1L
        val taskRepository = mockk<TaskRepositoryWrapper>()
        val lagreTaskSteg = lagLagreTaskSteg(taskRepository)
        val høyesteSekvensnummerFørTest = ferdigstiltBehandlingForPensjonRepository.finnHøyesteSekvensnummer()
        every { taskRepository.save(any()) } throws IllegalStateException("Opprettelse av task feilet")

        try {
            // Act & Assert
            assertThrows<IllegalStateException> { kjørILagreTransaksjon(lagreTaskSteg, behandlingId) }
            assertThat(ferdigstiltBehandlingForPensjonRepository.findByBehandlingId(behandlingId)).isNull()
            assertThat(ferdigstiltBehandlingForPensjonRepository.finnHøyesteSekvensnummer()).isEqualTo(høyesteSekvensnummerFørTest)

            // Arrange
            every { taskRepository.save(any()) } answers { firstArg() }

            // Act
            kjørILagreTransaksjon(lagreTaskSteg, behandlingId)

            // Assert
            val lagretFerdigstiltBehandling = ferdigstiltBehandlingForPensjonRepository.findByBehandlingId(behandlingId)
            assertThat(lagretFerdigstiltBehandling?.sekvensnummer).isEqualTo(høyesteSekvensnummerFørTest + 1)
        } finally {
            slettLagredeFerdigstilteBehandlinger(behandlingId)
        }
    }

    @Test
    fun `skal tildele unike sammenhengende sekvensnumre ved samtidig lagring`() {
        // Arrange
        val behandlingIder = listOf(1L, 2L)
        val taskRepository = mockk<TaskRepositoryWrapper>()
        val lagreTaskSteg = lagLagreTaskSteg(taskRepository)
        val høyesteSekvensnummerFørTest = ferdigstiltBehandlingForPensjonRepository.finnHøyesteSekvensnummer()
        val alleTråderErKlare = CountDownLatch(behandlingIder.size)
        val startSignal = CountDownLatch(1)
        val trådpool = Executors.newFixedThreadPool(behandlingIder.size)
        every { taskRepository.save(any()) } answers { firstArg() }

        try {
            // Act
            val samtidigeLagringer =
                behandlingIder.map { behandlingId ->
                    trådpool.submit {
                        alleTråderErKlare.countDown()
                        check(startSignal.await(10, TimeUnit.SECONDS))
                        kjørILagreTransaksjon(lagreTaskSteg, behandlingId)
                    }
                }
            check(alleTråderErKlare.await(10, TimeUnit.SECONDS))
            startSignal.countDown()
            samtidigeLagringer.forEach { it.get(30, TimeUnit.SECONDS) }

            // Assert
            val tildelteSekvensnumre = behandlingIder.map { ferdigstiltBehandlingForPensjonRepository.findByBehandlingId(it)?.sekvensnummer }
            assertThat(tildelteSekvensnumre).containsExactlyInAnyOrder(høyesteSekvensnummerFørTest + 1, høyesteSekvensnummerFørTest + 2)
        } finally {
            startSignal.countDown()
            trådpool.shutdownNow()
            check(trådpool.awaitTermination(30, TimeUnit.SECONDS))
            slettLagredeFerdigstilteBehandlinger(*behandlingIder.toLongArray())
        }
    }

    private fun lagLagreTaskSteg(taskRepository: TaskRepositoryWrapper): LagreFerdigstiltBehandlingForPensjonTask {
        val vedtakService = mockk<VedtakService>()
        val vedtaksperiodeService = mockk<VedtaksperiodeService>()
        every { vedtakService.hentAktivForBehandlingThrows(any()) } returns lagVedtak(vedtaksdato = vedtaktidspunkt)
        every { vedtaksperiodeService.finnEndringstidspunktForBehandling(any()) } returns endringstidspunkt
        return LagreFerdigstiltBehandlingForPensjonTask(vedtakService, vedtaksperiodeService, ferdigstiltBehandlingForPensjonRepository, taskRepository)
    }

    // familie-prosessering kjører doTask i en transaksjon, så testen må gjøre det samme
    private fun kjørILagreTransaksjon(
        lagreTaskSteg: LagreFerdigstiltBehandlingForPensjonTask,
        behandlingId: Long,
    ) {
        transactionTemplate.executeWithoutResult {
            lagreTaskSteg.doTask(LagreFerdigstiltBehandlingForPensjonTask.opprettTask(ident, behandlingId))
        }
    }

    private fun slettLagredeFerdigstilteBehandlinger(vararg behandlingIder: Long) {
        ferdigstiltBehandlingForPensjonRepository.deleteAll(
            behandlingIder.toList().mapNotNull { ferdigstiltBehandlingForPensjonRepository.findByBehandlingId(it) },
        )
    }
}
