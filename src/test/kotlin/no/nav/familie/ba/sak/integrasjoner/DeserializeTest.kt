package no.nav.familie.ba.sak.integrasjoner

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Kjønn
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DeserializeTest {
    private val mapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())

    @Test
    fun testDeserializaPersoninfo() {
        assertThat(getPersoninfo("M").kjønn).isEqualTo(Kjønn.MANN)
        assertThat(getPersoninfo("MANN").kjønn).isEqualTo(Kjønn.MANN)
        assertThat(getPersoninfo("K").kjønn).isEqualTo(Kjønn.KVINNE)
        assertThat(getPersoninfo("KVINNE").kjønn).isEqualTo(Kjønn.KVINNE)
        assertThat(getPersoninfo("UKJENT").kjønn).isEqualTo(Kjønn.UKJENT)
    }

    private fun getPersoninfo(kjønn: String): Personinfo {
        val json = """
            {
                "kjønn": "$kjønn",
                "fødselsdato": "1982-08-05"
              }
        """.trimIndent()
        return mapper.readValue(json, Personinfo::class.java)
    }
}


