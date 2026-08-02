package no.nav.familie.ba.sak.internal

import io.mockk.mockk
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.cache.CacheManager
import org.springframework.core.env.Environment

class PreprodControllerTest {
    private fun lagPreprodController(
        branch: String,
        versjon: String,
    ) = PreprodController(
        testVerktøyService = mockk<TestVerktøyService>(),
        tilgangService = mockk<TilgangService>(),
        environment = mockk<Environment>(),
        shortCacheManager = mockk<CacheManager>(),
        branch = branch,
        versjon = versjon,
    )

    @Test
    fun `skal returnere deployet branch og versjon`() {
        // Arrange
        val preprodController = lagPreprodController(branch = "NAV-22519_min_branch", versjon = "familie-ba-sak:abc123")

        // Act
        val respons = preprodController.hentVersjonsinfo()

        // Assert
        assertThat(respons.body?.data?.branch).isEqualTo("NAV-22519_min_branch")
        assertThat(respons.body?.data?.versjon).isEqualTo("familie-ba-sak:abc123")
    }
}
