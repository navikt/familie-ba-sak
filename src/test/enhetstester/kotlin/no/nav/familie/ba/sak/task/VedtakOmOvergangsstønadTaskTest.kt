package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle.AUTOMATISK_SATSENDRING_SMÅBARNSTILLEGG
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.StartSatsendring
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.RekjørSenereException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

internal class VedtakOmOvergangsstønadTaskTest {
    private val autovedtakStegService = mockk<AutovedtakStegService>()
    private val personidentService = mockk<PersonidentService>()
    private val startSatsendring = mockk<StartSatsendring>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val vedtakOmOvergangsstønadTask =
        VedtakOmOvergangsstønadTask(
            autovedtakStegService = autovedtakStegService,
            personidentService = personidentService,
            startSatsendring = startSatsendring,
            featureToggleService = featureToggleService,
        )

    @Test
    fun `skal kaste rekjørSenereException hvis satsendring skal opprettes`() {
        // Arrange
        val personIdent = "12345678910"
        val aktør = randomAktør()
        val task =
            Task(
                type = VedtakOmOvergangsstønadTask.TASK_STEP_TYPE,
                payload = personIdent,
            )

        every { personidentService.hentAktør(personIdent) } returns aktør
        every { startSatsendring.sjekkOgOpprettSatsendringVedGammelSats(personIdent) } returns true
        every { featureToggleService.isEnabled(AUTOMATISK_SATSENDRING_SMÅBARNSTILLEGG) } returns true

        // Act
        val rekjørSenereException = assertThrows<RekjørSenereException> { vedtakOmOvergangsstønadTask.doTask(task) }

        // Assert
        assertThat(rekjørSenereException.triggerTid).isAfter(LocalDateTime.now().plusMinutes(58))
        assertThat(rekjørSenereException.årsak).isEqualTo("Satsendring må kjøre ferdig før man behandler autovedtak småbarnstillegg")
    }

    @Test
    fun `doTask skal ikke kjøre behandling av småbarnstillegg hvis satsendring blir opprettet`() {
        // Arrange
        val personIdent = "12345678910"
        val aktør = randomAktør()

        val task =
            Task(
                type = VedtakOmOvergangsstønadTask.TASK_STEP_TYPE,
                payload = personIdent,
            )

        every { personidentService.hentAktør(personIdent) } returns aktør
        every { startSatsendring.sjekkOgOpprettSatsendringVedGammelSats(personIdent) } returns true
        every { featureToggleService.isEnabled(AUTOMATISK_SATSENDRING_SMÅBARNSTILLEGG) } returns true

        // Act & Assert
        assertThrows<RekjørSenereException> { vedtakOmOvergangsstønadTask.doTask(task) }
        verify(exactly = 0) { autovedtakStegService.kjørBehandlingSmåbarnstillegg(any(), any(), any()) }
    }

    @Test
    fun `doTask skal fullføre behandling uten å kaste exception når alt er OK`() {
        // Arrange
        val personIdent = "12345678910"
        val aktør = randomAktør()

        val task =
            Task(
                type = VedtakOmOvergangsstønadTask.TASK_STEP_TYPE,
                payload = personIdent,
            )

        every { personidentService.hentAktør(personIdent) } returns aktør
        every { startSatsendring.sjekkOgOpprettSatsendringVedGammelSats(personIdent) } returns false
        every { featureToggleService.isEnabled(AUTOMATISK_SATSENDRING_SMÅBARNSTILLEGG) } returns true
        every {
            autovedtakStegService.kjørBehandlingSmåbarnstillegg(
                mottakersAktør = aktør,
                aktør = aktør,
                førstegangKjørt = any(),
            )
        } returns AutovedtakStegService.BEHANDLING_FERDIG

        // Act & Assert
        assertDoesNotThrow { vedtakOmOvergangsstønadTask.doTask(task) }

        verify(exactly = 1) { startSatsendring.sjekkOgOpprettSatsendringVedGammelSats(personIdent) }
        verify(exactly = 1) {
            autovedtakStegService.kjørBehandlingSmåbarnstillegg(
                mottakersAktør = aktør,
                aktør = aktør,
                førstegangKjørt = any(),
            )
        }
    }
}
