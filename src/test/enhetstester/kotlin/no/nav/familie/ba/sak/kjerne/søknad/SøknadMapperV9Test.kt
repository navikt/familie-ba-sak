package no.nav.familie.ba.sak.kjerne.søknad

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagBarnetrygdSøknadV9
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.kontrakter.ba.søknad.VersjonertBarnetrygdSøknadV8
import no.nav.familie.kontrakter.ba.søknad.VersjonertBarnetrygdSøknadV9
import no.nav.familie.kontrakter.ba.søknad.v4.Søknadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import no.nav.familie.kontrakter.ba.søknad.v8.Søknad as BarnetrygdSøknadV8

class SøknadMapperV9Test {
    @Nested
    inner class MapTilSøknad {
        @Test
        fun `skal mappe VersjonertBarnetrygdSøknadV9 til Søknad`() {
            // Arrange
            val barn1 = randomFnr()
            val barn2 = randomFnr()
            val versjonertBarnetrygdSøknadV9 =
                VersjonertBarnetrygdSøknadV9(
                    barnetrygdSøknad = lagBarnetrygdSøknadV9(barnFnr = listOf(barn1, barn2), søknadstype = Søknadstype.ORDINÆR, erEøs = true, originalspråk = "nn"),
                )

            // Act
            val søknad = SøknadMapperV9().mapTilSøknad(versjonertBarnetrygdSøknadV9)

            // Assert
            assertThat(søknad.barn).hasSize(2)
            assertThat(søknad.barn.map { it.fnr }).containsExactlyInAnyOrder(barn1, barn2)
            assertThat(søknad.behandlingKategori).isEqualTo(BehandlingKategori.EØS)
            assertThat(søknad.behandlingUnderkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
            assertThat(søknad.målform).isEqualTo(Målform.NN)
        }

        @Test
        fun `skal kaste feil dersom vi forsøker å mappe en annen versjon`() {
            // Arrange
            val barnetrygdSøknadV8 = mockk<BarnetrygdSøknadV8>()
            val versjonertBarnetrygdSøknadV8 =
                VersjonertBarnetrygdSøknadV8(
                    barnetrygdSøknad = barnetrygdSøknadV8,
                )

            every { barnetrygdSøknadV8.kontraktVersjon } returns 8

            // Act & Assert
            val exception =
                assertThrows<IllegalArgumentException> {
                    SøknadMapperV9().mapTilSøknad(versjonertBarnetrygdSøknadV8)
                }

            assertThat(exception.message)
                .isEqualTo("Kan ikke mappe søknad av type ${versjonertBarnetrygdSøknadV8::class.simpleName} til versjon 9")
        }
    }
}
