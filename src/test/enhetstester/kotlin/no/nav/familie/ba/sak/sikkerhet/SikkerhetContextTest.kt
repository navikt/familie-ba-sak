package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.erMaskinTilMaskinToken
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.erSystemKontekst
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.harRolle
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.hentGrupper
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.hentHøyesteRolletilgangForInnloggetBruker
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.hentRolletilgangFraSikkerhetscontext
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.hentSaksbehandler
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.hentSaksbehandlerEpost
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.hentSaksbehandlerNavn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.Instant

class SikkerhetContextTest {
    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    private fun lagJwt(claims: Map<String, Any> = emptyMap()): Jwt =
        Jwt
            .withTokenValue("mock-token")
            .header("alg", "RS256")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .subject("test-subject")
            .apply { claims.forEach(::claim) }
            .build()

    private fun settSikkerhetscontext(
        jwt: Jwt,
        roller: List<Rolle> = emptyList(),
    ) {
        val authorities = roller.map { SimpleGrantedAuthority(it.authority()) }
        SecurityContextHolder.setContext(SecurityContextImpl(JwtAuthenticationToken(jwt, authorities)))
    }

    @Nested
    inner class ErSystemKontekstTest {
        @Test
        fun `returnerer false når NAVident er en saksbehandler-ident`() {
            settSikkerhetscontext(lagJwt(mapOf("NAVident" to "Z999999")))
            assertThat(erSystemKontekst()).isFalse()
        }

        @Test
        fun `returnerer true når det ikke finnes noe token (fallback til systemforkortelse)`() {
            assertThat(erSystemKontekst()).isTrue()
        }
    }

    @Nested
    inner class ErMaskinTilMaskinTokenTest {
        @Test
        fun `returnerer true når oid er lik sub og roles inneholder access_as_application`() {
            val appId = "appId"
            settSikkerhetscontext(
                lagJwt(
                    mapOf(
                        "oid" to appId,
                        "sub" to appId,
                        "roles" to listOf("access_as_application"),
                    ),
                ),
            )
            assertThat(erMaskinTilMaskinToken()).isTrue()
        }

        @Test
        fun `returnerer false når oid er ulik sub`() {
            settSikkerhetscontext(
                lagJwt(
                    mapOf(
                        "oid" to "oid",
                        "sub" to "sub",
                        "roles" to listOf("access_as_application"),
                    ),
                ),
            )
            assertThat(erMaskinTilMaskinToken()).isFalse()
        }

        @Test
        fun `returnerer false når oid er lik sub men rollen mangler`() {
            val appId = "appId"
            settSikkerhetscontext(
                lagJwt(
                    mapOf(
                        "oid" to appId,
                        "sub" to appId,
                        "roles" to emptyList<String>(),
                    ),
                ),
            )
            assertThat(erMaskinTilMaskinToken()).isFalse()
        }

        @Test
        fun `returnerer false når det ikke finnes noe token`() {
            assertThat(erMaskinTilMaskinToken()).isFalse()
        }
    }

    @Nested
    inner class HentSaksbehandlerTest {
        @Test
        fun `returnerer NAVident-claim fra token`() {
            settSikkerhetscontext(lagJwt(mapOf("NAVident" to "Z999999")))
            assertThat(hentSaksbehandler()).isEqualTo("Z999999")
        }

        @Test
        fun `returnerer systemforkortelse når NAVident-claim mangler`() {
            settSikkerhetscontext(lagJwt())
            assertThat(hentSaksbehandler()).isEqualTo(SikkerhetContext.SYSTEM_FORKORTELSE)
        }
    }

    @Nested
    inner class HentSaksbehandlerEpostTest {
        @Test
        fun `returnerer preferred_username-claim fra token`() {
            settSikkerhetscontext(lagJwt(mapOf("preferred_username" to "fornavn.etternavn@nav.no")))
            assertThat(hentSaksbehandlerEpost()).isEqualTo("fornavn.etternavn@nav.no")
        }

        @Test
        fun `returnerer systemforkortelse når preferred_username-claim mangler`() {
            settSikkerhetscontext(lagJwt())
            assertThat(hentSaksbehandlerEpost()).isEqualTo(SikkerhetContext.SYSTEM_FORKORTELSE)
        }
    }

    @Nested
    inner class HentSaksbehandlerNavnTest {
        @Test
        fun `returnerer name-claim fra token`() {
            settSikkerhetscontext(lagJwt(mapOf("name" to "Etternavn, Fornavn")))
            assertThat(hentSaksbehandlerNavn()).isEqualTo("Etternavn, Fornavn")
        }

        @Test
        fun `returnerer systemnavn når name-claim mangler`() {
            settSikkerhetscontext(lagJwt())
            assertThat(hentSaksbehandlerNavn()).isEqualTo(SikkerhetContext.SYSTEM_NAVN)
        }
    }

    @Nested
    inner class HentGrupperTest {
        @Test
        fun `returnerer groups-claim som liste`() {
            val grupper = listOf("gruppe-a", "gruppe-b")
            settSikkerhetscontext(lagJwt(mapOf("groups" to grupper)))
            assertThat(hentGrupper()).containsExactlyElementsOf(grupper)
        }

        @Test
        fun `returnerer tom liste når groups-claim mangler`() {
            settSikkerhetscontext(lagJwt())
            assertThat(hentGrupper()).isEmpty()
        }
    }

    @Nested
    inner class HarRolleTest {
        @Test
        fun `returnerer true når bruker har den angitte rollen`() {
            settSikkerhetscontext(lagJwt(), roller = listOf(Rolle.SAKSBEHANDLER))
            assertThat(harRolle(Rolle.SAKSBEHANDLER)).isTrue()
        }

        @Test
        fun `returnerer false når bruker mangler den angitte rollen`() {
            settSikkerhetscontext(lagJwt(), roller = listOf(Rolle.VEILEDER))
            assertThat(harRolle(Rolle.SAKSBEHANDLER)).isFalse()
        }
    }

    @Nested
    inner class HentHøyesteRolletilgangForInnloggetBrukerTest {
        @Nested
        inner class Enkeltroller {
            @Test
            fun `returnerer SYSTEM ved systemkontekst uten token`() {
                assertThat(hentHøyesteRolletilgangForInnloggetBruker()).isEqualTo(BehandlerRolle.SYSTEM)
            }

            @Test
            fun `returnerer BESLUTTER ved kun BESLUTTER-rolle`() {
                settSikkerhetscontext(lagJwt(mapOf("NAVident" to "Z999999")), listOf(Rolle.BESLUTTER))
                assertThat(hentHøyesteRolletilgangForInnloggetBruker()).isEqualTo(BehandlerRolle.BESLUTTER)
            }

            @Test
            fun `returnerer SAKSBEHANDLER ved kun SAKSBEHANDLER-rolle`() {
                settSikkerhetscontext(lagJwt(mapOf("NAVident" to "Z999999")), listOf(Rolle.SAKSBEHANDLER))
                assertThat(hentHøyesteRolletilgangForInnloggetBruker()).isEqualTo(BehandlerRolle.SAKSBEHANDLER)
            }

            @Test
            fun `returnerer FORVALTER ved kun FORVALTER-rolle`() {
                settSikkerhetscontext(lagJwt(mapOf("NAVident" to "Z999999")), listOf(Rolle.FORVALTER))
                assertThat(hentHøyesteRolletilgangForInnloggetBruker()).isEqualTo(BehandlerRolle.FORVALTER)
            }

            @Test
            fun `returnerer VEILEDER ved kun VEILEDER-rolle`() {
                settSikkerhetscontext(lagJwt(mapOf("NAVident" to "Z999999")), listOf(Rolle.VEILEDER))
                assertThat(hentHøyesteRolletilgangForInnloggetBruker()).isEqualTo(BehandlerRolle.VEILEDER)
            }

            @Test
            fun `returnerer UKJENT uten noen roller`() {
                settSikkerhetscontext(lagJwt(mapOf("NAVident" to "Z999999")), emptyList())
                assertThat(hentHøyesteRolletilgangForInnloggetBruker()).isEqualTo(BehandlerRolle.UKJENT)
            }
        }

        @Nested
        inner class Rekkefølgetester {
            @Test
            fun `BESLUTTER returneres før alle lavere roller når bruker har alle roller`() {
                settSikkerhetscontext(
                    lagJwt(mapOf("NAVident" to "Z999999")),
                    listOf(Rolle.BESLUTTER, Rolle.SAKSBEHANDLER, Rolle.FORVALTER, Rolle.VEILEDER),
                )
                assertThat(hentHøyesteRolletilgangForInnloggetBruker()).isEqualTo(BehandlerRolle.BESLUTTER)
            }

            @Test
            fun `SAKSBEHANDLER returneres før FORVALTER og VEILEDER`() {
                settSikkerhetscontext(
                    lagJwt(mapOf("NAVident" to "Z999999")),
                    listOf(Rolle.SAKSBEHANDLER, Rolle.FORVALTER, Rolle.VEILEDER),
                )
                assertThat(hentHøyesteRolletilgangForInnloggetBruker()).isEqualTo(BehandlerRolle.SAKSBEHANDLER)
            }

            @Test
            fun `FORVALTER returneres før VEILEDER`() {
                settSikkerhetscontext(lagJwt(mapOf("NAVident" to "Z999999")), listOf(Rolle.FORVALTER, Rolle.VEILEDER))
                assertThat(hentHøyesteRolletilgangForInnloggetBruker()).isEqualTo(BehandlerRolle.FORVALTER)
            }
        }
    }

    @Nested
    inner class HentRolletilgangFraSikkerhetscontextTest {
        @Test
        fun `returnerer SYSTEM ved systemkontekst uavhengig av lavesteSikkerhetsnivå`() {
            assertThat(hentRolletilgangFraSikkerhetscontext(BehandlerRolle.SAKSBEHANDLER)).isEqualTo(BehandlerRolle.SYSTEM)
        }

        @Test
        fun `returnerer UKJENT når lavesteSikkerhetsnivå er null og bruker ikke er system`() {
            settSikkerhetscontext(lagJwt(mapOf("NAVident" to "Z999999")), listOf(Rolle.SAKSBEHANDLER))
            assertThat(hentRolletilgangFraSikkerhetscontext(null)).isEqualTo(BehandlerRolle.UKJENT)
        }

        @Test
        fun `returnerer lavesteSikkerhetsnivå når bruker har tilstrekkelig nivå`() {
            settSikkerhetscontext(lagJwt(mapOf("NAVident" to "Z999999")), listOf(Rolle.BESLUTTER))
            assertThat(hentRolletilgangFraSikkerhetscontext(BehandlerRolle.SAKSBEHANDLER)).isEqualTo(BehandlerRolle.SAKSBEHANDLER)
        }

        @Test
        fun `returnerer UKJENT når bruker ikke har tilstrekkelig nivå`() {
            settSikkerhetscontext(lagJwt(mapOf("NAVident" to "Z999999")), listOf(Rolle.VEILEDER))
            assertThat(hentRolletilgangFraSikkerhetscontext(BehandlerRolle.SAKSBEHANDLER)).isEqualTo(BehandlerRolle.UKJENT)
        }
    }
}
