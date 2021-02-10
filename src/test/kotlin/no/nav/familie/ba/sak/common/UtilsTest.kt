package no.nav.familie.ba.sak.common

import junit.framework.Assert
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.tilBrevTekst
import no.nav.familie.ba.sak.common.Utils.hentPropertyFraMaven
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.io.InputStreamReader
import java.time.LocalDate

internal class UtilsTest {

    @Test
    fun `hent property fra maven skal ikke være blank`() {
        val result = hentPropertyFraMaven("java.version")
        Assertions.assertThat(result).isNotBlank()
    }

    @Test
    fun `hent property som mangler skal returnere null`() {
        val result = hentPropertyFraMaven("skalikkefinnes")
        Assertions.assertThat(result).isNullOrEmpty()
    }

    @Test
    fun `Test transformering av en personer til brevtekst`() {
        val førsteBarn = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(6))

        Assert.assertEquals("${førsteBarn.fødselsdato.tilKortString()}",
                            listOf(førsteBarn).tilBrevTekst())
    }

    @Test
    fun `Test transformering av to personer til brevtekst`() {
        val førsteBarn = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(6))
        val andreBarn = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(6))

        Assert.assertEquals("${førsteBarn.fødselsdato.tilKortString()} og ${andreBarn.fødselsdato.tilKortString()}",
                            listOf(førsteBarn, andreBarn).tilBrevTekst())
    }

    @Test
    fun `Test transformering av tre personer til brevtekst`() {
        val førsteBarn = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(6))
        val andreBarn = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(6))
        val tredjeBarn = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(6))

        Assert.assertEquals("${førsteBarn.fødselsdato.tilKortString()}, ${andreBarn.fødselsdato.tilKortString()} og ${tredjeBarn.fødselsdato.tilKortString()}",
                            listOf(førsteBarn, andreBarn, tredjeBarn).tilBrevTekst())
    }
}

