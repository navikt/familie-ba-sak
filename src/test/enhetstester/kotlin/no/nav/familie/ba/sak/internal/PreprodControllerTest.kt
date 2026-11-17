package no.nav.familie.ba.sak.internal

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verifyOrder
import no.nav.familie.ba.sak.config.featureToggle.miljø.Profil
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.cache.CacheManager
import org.springframework.core.env.Environment

class PreprodControllerTest {
    private val testVerktøyService = mockk<TestVerktøyService>()
    private val tilgangService = mockk<TilgangService>()
    private val tilbakestillBehandlingService = mockk<TilbakestillBehandlingService>()
    private val environment = mockk<Environment>()

    private fun lagPreprodController(
        branch: String = "branch",
        versjon: String = "versjon",
    ) = PreprodController(
        testVerktøyService = testVerktøyService,
        tilgangService = tilgangService,
        tilbakestillBehandlingService = tilbakestillBehandlingService,
        environment = environment,
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

    @Test
    fun `skal tilbakestille behandlingen til vilkårsvurdering etter at vilkårsvurderingen er fylt ut`() {
        // Arrange
        val behandling = lagBehandling()
        every { environment.activeProfiles } returns arrayOf(Profil.Preprod.navn)
        justRun { tilgangService.validerTilgangTilBehandling(any(), any()) }
        justRun { tilgangService.validerKanRedigereBehandling(behandling.id) }
        justRun { testVerktøyService.oppdaterVilkårUtenFomTilFødselsdato(behandling.id) }
        every { tilbakestillBehandlingService.resettStegVedEndringPåVilkår(behandling.id) } returns behandling

        // Act
        lagPreprodController().settFomPåTommeVilkårTilFødselsdato(behandling.id)

        // Assert
        verifyOrder {
            tilgangService.validerKanRedigereBehandling(behandling.id)
            testVerktøyService.oppdaterVilkårUtenFomTilFødselsdato(behandling.id)
            tilbakestillBehandlingService.resettStegVedEndringPåVilkår(behandling.id)
        }
    }
}
