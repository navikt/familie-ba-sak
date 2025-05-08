package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.EnhetConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class EnhetConfigIntegrationTest : AbstractSpringIntegrationTest() {
    @Autowired
    private lateinit var enhetConfig: EnhetConfig

    @Test
    fun `skal instansiere EnhetConfig med riktig verdier fra application yaml`() {
        // Assert
        assertThat(enhetConfig.enheter["VIKAFOSSEN"]).isEqualTo("e2cf416e-eb2b-4c86-8b5b-8c5b00385bcf")
        assertThat(enhetConfig.enheter["DRAMMEN"]).isEqualTo("16d14203-a5e0-4813-ba76-6d0a69aeb88b")
        assertThat(enhetConfig.enheter["VADSO"]).isEqualTo("a996c91f-6dd1-466d-bed0-06ddccab87f5")
        assertThat(enhetConfig.enheter["OSLO"]).isEqualTo("48ff353a-fd52-4109-be32-d0e825322b1f")
        assertThat(enhetConfig.enheter["STORD"]).isEqualTo("b00084c4-0325-4ec3-b17f-70aa0a03ed37")
        assertThat(enhetConfig.enheter["STEINKJER"]).isEqualTo("b5a21ebf-d8c7-415c-9a88-6b4735f845cd")
        assertThat(enhetConfig.enheter["MIDLERTIDIG_ENHET"]).isEqualTo("22a9b5f5-645f-4090-ae2d-29a38a126de6")
    }
}
