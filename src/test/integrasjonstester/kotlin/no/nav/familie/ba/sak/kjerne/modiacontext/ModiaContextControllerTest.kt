package no.nav.familie.ba.sak.kjerne.modiacontext

import io.mockk.mockk
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.ekstern.restDomene.NyAktivBrukerIModiaContextDto
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

class ModiaContextControllerTest(
    @Autowired private val modiaContextService: ModiaContextService,
) : AbstractSpringIntegrationTest() {
    val modiaContextController =
        ModiaContextController(
            modiaContextService = modiaContextService,
            tilgangService = mockk(relaxed = true),
        )

    @Test
    fun `skal hente context`() {
        val response = modiaContextController.hentContext()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(response.body?.data?.aktivBruker).isEqualTo("13025514402")
        assertThat(response.body?.data?.aktivEnhet).isEqualTo("0000")
    }

    @Test
    fun `skal oppdatere context`() {
        val response =
            modiaContextController.settNyAktivBruker(
                NyAktivBrukerIModiaContextDto(personIdent = "13025514402"),
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(response.body?.data?.aktivBruker).isEqualTo("13025514402")
        assertThat(response.body?.data?.aktivEnhet).isEqualTo("0000")
    }

    @Test
    fun `skal kaste feil ved ugyldig personIdent`() {
        val exception =
            assertThrows<IllegalStateException> {
                modiaContextController.settNyAktivBruker(
                    NyAktivBrukerIModiaContextDto(personIdent = "12345678910"),
                )
            }

        assertThat(exception.message).isEqualTo("12345678910")
    }
}
