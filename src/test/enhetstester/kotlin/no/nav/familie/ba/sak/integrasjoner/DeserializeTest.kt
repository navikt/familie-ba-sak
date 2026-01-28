package no.nav.familie.ba.sak.integrasjoner

import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.jsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DeserializeTest {
    @Test
    fun testDeserializaPersoninfo() {
        assertThat(getPersoninfo("M").kjønn).isEqualTo(Kjønn.MANN)
        assertThat(getPersoninfo("MANN").kjønn).isEqualTo(Kjønn.MANN)
        assertThat(getPersoninfo("K").kjønn).isEqualTo(Kjønn.KVINNE)
        assertThat(getPersoninfo("KVINNE").kjønn).isEqualTo(Kjønn.KVINNE)
        assertThat(getPersoninfo("UKJENT").kjønn).isEqualTo(Kjønn.UKJENT)
    }

    private fun getPersoninfo(kjønn: String): PersonInfo {
        val json =
            """
            {
                "kjønn": "$kjønn",
                "fødselsdato": "1982-08-05"
              }
            """.trimIndent()
        return jsonMapper.readValue(json, PersonInfo::class.java)
    }
}
