package no.nav.familie.ba.sak.util

import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder

object BrukerContextUtil {

    fun clearBrukerContext() {
        RequestContextHolder.resetRequestAttributes()
    }

    fun mockBrukerContext(preferredUsername: String = "A", groups: List<String> = emptyList()) {
        val tokenValidationContext = mockk<TokenValidationContext>()
        val jwtTokenClaims = mockk<JwtTokenClaims>()
        val requestAttributes = mockk<RequestAttributes>()
        RequestContextHolder.setRequestAttributes(requestAttributes)
        every {
            requestAttributes.getAttribute(
                SpringTokenValidationContextHolder::class.java.name,
                RequestAttributes.SCOPE_REQUEST
            )
        } returns tokenValidationContext
        every { tokenValidationContext.getClaims("azuread") } returns jwtTokenClaims
        every { jwtTokenClaims.get("preferred_username") } returns preferredUsername
        every { jwtTokenClaims.get("NAVident") } returns preferredUsername
        every { jwtTokenClaims.get("name") } returns preferredUsername
        every { jwtTokenClaims.get("groups") } returns groups
    }

    fun testWithBrukerContext(preferredUsername: String = "A", groups: List<String> = emptyList(), fn: () -> Unit) {
        mockBrukerContext()
        try {
            mockBrukerContext(preferredUsername, groups)
            fn()
        } finally {
            clearBrukerContext()
        }
    }
}
