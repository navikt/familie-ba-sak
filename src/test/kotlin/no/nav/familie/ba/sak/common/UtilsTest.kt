package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.tilBrevTekst
import no.nav.familie.ba.sak.common.Utils.hentPropertyFraMaven
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtilsTest {

    @Test
    fun `hent property fra maven skal ikke være blank`() {
        val result = hentPropertyFraMaven("java.version")
        assertTrue(result?.isNotBlank() == true)
    }

    @Test
    fun `hent property som mangler skal returnere null`() {
        val result = hentPropertyFraMaven("skalikkefinnes")
        assertTrue(result.isNullOrEmpty())
    }

    @Test
    fun `Test transformering av en personer til brevtekst`() {
        val førsteBarn = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(6))

        assertEquals(førsteBarn.fødselsdato.tilKortString(),
                     listOf(førsteBarn).tilBrevTekst())
    }

    @Test
    fun `Test transformering av to personer til brevtekst`() {
        val førsteBarn = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(6))
        val andreBarn = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(6))

        assertEquals("${førsteBarn.fødselsdato.tilKortString()} og ${andreBarn.fødselsdato.tilKortString()}",
                     listOf(førsteBarn, andreBarn).tilBrevTekst())
    }

    @Test
    fun `Test transformering av tre personer til brevtekst`() {
        val førsteBarn = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(6))
        val andreBarn = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(6))
        val tredjeBarn = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(6))

        assertEquals("${førsteBarn.fødselsdato.tilKortString()}, ${andreBarn.fødselsdato.tilKortString()} og ${tredjeBarn.fødselsdato.tilKortString()}",
                     listOf(førsteBarn, andreBarn, tredjeBarn).tilBrevTekst())
    }
}

