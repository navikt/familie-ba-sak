package no.nav.familie.ba.sak.internal

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
class GithubController {
    @GetMapping("/hent-branch")
    fun hentBranch(): ResponseEntity<Ressurs<GithubBranchDto>> {
        val branch: String? = System.getenv("GITHUB_BRANCH")
        val sha: String? = System.getenv("GITHUB_SHA")
        return ResponseEntity.ok().body(
            Ressurs.success(
                data = GithubBranchDto(branch = branch, sha = sha),
            ),
        )
    }
}

data class GithubBranchDto(
    val branch: String?,
    val sha: String?,
)
