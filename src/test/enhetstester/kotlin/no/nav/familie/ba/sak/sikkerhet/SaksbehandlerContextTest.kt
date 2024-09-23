package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class SaksbehandlerContextTest {
    private val saksbehandlerContext = SaksbehandlerContext("kode6", kode7GruppeId = "kode7")

    @Test
    fun `skal returnere true på adressebeskyttelsegradering ugradert`() {
        // Arrange
        mockBrukerContext("A", groups = listOf("kode6", "kode7"))

        // Act
        val harTilgang = saksbehandlerContext.harTilgang(ADRESSEBESKYTTELSEGRADERING.UGRADERT)

        // Assert
        assertThat(harTilgang).isTrue()
    }

    @Test
    fun `skal returnere true på adressebeskyttelsegradering fortrolig når saksbehandler har tilgang til den gruppen`() {
        // Arrange
        mockBrukerContext("A", groups = listOf("kode7"))

        // Act
        val harTilgang = saksbehandlerContext.harTilgang(ADRESSEBESKYTTELSEGRADERING.FORTROLIG)

        // Assert
        assertThat(harTilgang).isTrue()
    }

    @Test
    fun `skal returnere false på adressebeskyttelsegradering fortrolig når saksbehandler ikke har tilgang til den gruppen`() {
        // Arrange
        mockBrukerContext("A", groups = listOf(""))

        // Act
        val harTilgang = saksbehandlerContext.harTilgang(ADRESSEBESKYTTELSEGRADERING.FORTROLIG)

        // Assert
        assertThat(harTilgang).isFalse()
    }

    @ParameterizedTest
    @EnumSource(ADRESSEBESKYTTELSEGRADERING::class, names = ["STRENGT_FORTROLIG", "STRENGT_FORTROLIG_UTLAND"], mode = EnumSource.Mode.INCLUDE)
    fun `skal returnere true på adressebeskyttelsegradering strengt fortrolig når saksbehandler har tilgang til den gruppen`(gradering: ADRESSEBESKYTTELSEGRADERING) {
        // Arrange
        mockBrukerContext("A", groups = listOf("kode6"))

        // Act
        val harTilgang = saksbehandlerContext.harTilgang(gradering)

        // Assert
        assertThat(harTilgang).isTrue()
    }

    @ParameterizedTest
    @EnumSource(ADRESSEBESKYTTELSEGRADERING::class, names = ["STRENGT_FORTROLIG", "STRENGT_FORTROLIG_UTLAND"], mode = EnumSource.Mode.INCLUDE)
    fun `skal returnere false på adressebeskyttelsegradering strengt fortrolig når saksbehandler ikke har tilgang til den gruppen`(gradering: ADRESSEBESKYTTELSEGRADERING) {
        // Arrange
        mockBrukerContext("A", groups = listOf(""))

        // Act
        val harTilgang = saksbehandlerContext.harTilgang(gradering)

        // Assert
        assertThat(harTilgang).isFalse()
    }
}
