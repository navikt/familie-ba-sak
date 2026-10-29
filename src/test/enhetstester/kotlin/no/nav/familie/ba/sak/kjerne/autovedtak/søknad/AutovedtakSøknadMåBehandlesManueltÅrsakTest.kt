package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource

class AutovedtakSøknadMåBehandlesManueltÅrsakTest {
    @Nested
    inner class FraVilkårForSøker {
        @ParameterizedTest
        @CsvSource(
            "BOSATT_I_RIKET, SØKER_VILKÅR_BOSATT_I_RIKET",
            "LOVLIG_OPPHOLD, SØKER_VILKÅR_LOVLIG_OPPHOLD",
            "UTVIDET_BARNETRYGD, SØKER_VILKÅR_UTVIDET_BARNETRYGD",
        )
        fun `skal mappe gyldig vilkår for søker til riktig årsak`(
            vilkår: Vilkår,
            forventetÅrsak: AutovedtakSøknadMåBehandlesManueltÅrsak,
        ) {
            // Act
            val årsak = AutovedtakSøknadMåBehandlesManueltÅrsak.fraVilkår(vilkår = vilkår, erSøker = true)

            // Assert
            assertThat(årsak).isEqualTo(forventetÅrsak)
        }

        @ParameterizedTest
        @EnumSource(value = Vilkår::class, names = ["UNDER_18_ÅR", "BOR_MED_SØKER", "GIFT_PARTNERSKAP"])
        fun `skal kaste feil for vilkår som ikke gjelder søker`(vilkår: Vilkår) {
            // Act & Assert
            val feil =
                assertThrows<Feil> {
                    AutovedtakSøknadMåBehandlesManueltÅrsak.fraVilkår(vilkår = vilkår, erSøker = true)
                }
            assertThat(feil.message).isEqualTo("Ugyldig vilkårstype $vilkår for søker ved automatisk behandling av søknad")
        }
    }

    @Nested
    inner class FraVilkårForBarn {
        @ParameterizedTest
        @CsvSource(
            "UNDER_18_ÅR, BARN_VILKÅR_UNDER_18_ÅR",
            "BOR_MED_SØKER, BARN_VILKÅR_BOR_MED_SØKER",
            "GIFT_PARTNERSKAP, BARN_VILKÅR_GIFT_PARTNERSKAP",
            "BOSATT_I_RIKET, BARN_VILKÅR_BOSATT_I_RIKET",
            "LOVLIG_OPPHOLD, BARN_VILKÅR_LOVLIG_OPPHOLD",
        )
        fun `skal mappe gyldig vilkår for barn til riktig årsak`(
            vilkår: Vilkår,
            forventetÅrsak: AutovedtakSøknadMåBehandlesManueltÅrsak,
        ) {
            // Act
            val årsak = AutovedtakSøknadMåBehandlesManueltÅrsak.fraVilkår(vilkår = vilkår, erSøker = false)

            // Assert
            assertThat(årsak).isEqualTo(forventetÅrsak)
        }

        @Test
        fun `skal kaste feil for utvidet barnetrygd for barn`() {
            // Act & Assert
            val feil =
                assertThrows<Feil> {
                    AutovedtakSøknadMåBehandlesManueltÅrsak.fraVilkår(vilkår = Vilkår.UTVIDET_BARNETRYGD, erSøker = false)
                }
            assertThat(feil.message).isEqualTo("Ugyldig vilkårstype UTVIDET_BARNETRYGD for barn ved automatisk behandling av søknad")
        }
    }
}
