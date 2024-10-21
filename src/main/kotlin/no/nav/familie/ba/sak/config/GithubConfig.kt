package no.nav.familie.ba.sak.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class GithubConfig(
    // Verdiene blir sendt inn til docker-imaget via github actions
    @Value("\${GITHUB_BRANCH:Ikke satt}")
    val githubBranch: String,
    @Value("\${GITHUB_SHA:#{null}}")
    val githubSha: String?,
)
