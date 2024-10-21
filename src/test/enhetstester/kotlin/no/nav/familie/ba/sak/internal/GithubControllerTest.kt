package no.nav.familie.ba.sak.internal

import no.nav.familie.ba.sak.config.GithubConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GithubControllerTest {
    private val githubConfig: GithubConfig = GithubConfig(githubBranch = "tester", githubSha = "123")
    private val githubController = GithubController(githubConfig = githubConfig)

    @Test
    fun `skal vise info om github branch og sha`() {
        // Act
        val response = githubController.hentBranch()

        // Assert
        assertThat(response.body?.data).isEqualTo(GithubBranchDto(branch = "tester", sha = "123"))
    }
}
