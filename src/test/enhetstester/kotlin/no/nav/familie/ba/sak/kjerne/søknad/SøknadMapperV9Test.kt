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
                    barnetrygdSøknad =
                        lagBarnetrygdSøknadV9(
                            barnFnr = listOf(barn1, barn2),
                            søknadstype = Søknadstype.ORDINÆR,
                            erEøs = true,
                            originalspråk = "nn",
                            inneholderVedlegg = true,
                            erFosterbarn = true,
                            erIBeredskapshjem = true,
                            harKryssetForDeltBosted = true,
                            oppholderSegIInstitusjon = true,
                            søkerErAsylsøker = true,
                            barnErAsylsøker = true,
                            erAdoptertFraUtland = true,
                            borFastMedSøker = true,
                            barnHarOppholdtSegUtenforNorgeSiste12Mnd = true,
                        ),
                )

            // Act
            val søknad = SøknadMapperV9().mapTilSøknad(versjonertBarnetrygdSøknadV9)

            // Assert
            assertThat(søknad.barn).hasSize(2)
            assertThat(søknad.barn.map { it.fnr }).containsExactlyInAnyOrder(barn1, barn2)
            assertThat(søknad.behandlingKategori).isEqualTo(BehandlingKategori.EØS)
            assertThat(søknad.behandlingUnderkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
            assertThat(søknad.målform).isEqualTo(Målform.NN)
            assertThat(søknad.harKryssetPåEøsSpørsmål).isTrue()
            assertThat(søknad.inneholderVedlegg).isTrue()
            assertThat(søknad.barn.map { it.erFosterbarn }).containsOnly(true)
            assertThat(søknad.barn.map { it.erIBeredskapshjem }).containsOnly(true)
            assertThat(søknad.barn.map { it.harKryssetForDeltBosted }).containsOnly(true)
            assertThat(søknad.barn.map { it.oppholderSegIInstitusjon }).containsOnly(true)
            assertThat(søknad.barn.map { it.erAsylsøker }).containsOnly(true)
            assertThat(søknad.barn.map { it.erAdoptertFraUtland }).containsOnly(true)
            assertThat(søknad.barn.map { it.borFastMedSøker }).containsOnly(true)
            assertThat(søknad.barn.map { it.harOppholdtSegUtenforNorgeSiste12Mnd }).containsOnly(true)
            assertThat(søknad.søker.erAsylsøker).isTrue()
        }

        @Test
        fun `skal mappe hvert svar om institusjon, asyl og adopsjon til riktig felt`() {
            // Arrange
            val versjonertBarnetrygdSøknadV9 =
                VersjonertBarnetrygdSøknadV9(
                    barnetrygdSøknad =
                        lagBarnetrygdSøknadV9(
                            oppholderSegIInstitusjon = true,
                            søkerErAsylsøker = true,
                            barnErAsylsøker = false,
                            erAdoptertFraUtland = false,
                        ),
                )

            // Act
            val søknad = SøknadMapperV9().mapTilSøknad(versjonertBarnetrygdSøknadV9)

            // Assert
            assertThat(søknad.barn.map { it.oppholderSegIInstitusjon }).containsOnly(true)
            assertThat(søknad.søker.erAsylsøker).isTrue()
            assertThat(søknad.barn.map { it.erAsylsøker }).containsOnly(false)
            assertThat(søknad.barn.map { it.erAdoptertFraUtland }).containsOnly(false)
        }

        @Test
        fun `skal mappe at søker har oppholdt seg sammenhengende i Norge når søker har svart ja på spørsmålet`() {
            // Arrange
            val versjonertBarnetrygdSøknadV9 =
                VersjonertBarnetrygdSøknadV9(
                    barnetrygdSøknad = lagBarnetrygdSøknadV9(søkerHarOppholdtSegSammenhengendeINorgeSiste12Mnd = true),
                )

            // Act
            val søknad = SøknadMapperV9().mapTilSøknad(versjonertBarnetrygdSøknadV9)

            // Assert
            assertThat(søknad.søker.harOppholdtSegSammenhengendeINorgeSiste12Mnd).isTrue()
        }

        @Test
        fun `skal mappe at søker har oppholdt seg sammenhengende i Norge når svaret mangler i søknaden`() {
            // Arrange
            val barnetrygdSøknadV9 = lagBarnetrygdSøknadV9(søkerHarOppholdtSegSammenhengendeINorgeSiste12Mnd = false)
            val versjonertBarnetrygdSøknadV9 =
                VersjonertBarnetrygdSøknadV9(
                    barnetrygdSøknad =
                        barnetrygdSøknadV9.copy(
                            søker = barnetrygdSøknadV9.søker.copy(spørsmål = barnetrygdSøknadV9.søker.spørsmål - "værtINorgeITolvMåneder"),
                        ),
                )

            // Act
            val søknad = SøknadMapperV9().mapTilSøknad(versjonertBarnetrygdSøknadV9)

            // Assert
            assertThat(søknad.søker.harOppholdtSegSammenhengendeINorgeSiste12Mnd).isTrue()
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
