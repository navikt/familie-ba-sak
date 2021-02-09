package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import junit.framework.Assert.assertEquals
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.tilfeldigPerson
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PersonTest{
    @Test
    fun `Test transformering av en personer til brevtekst`() {
        val førsteBarn = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(6))

        assertEquals("${førsteBarn.fødselsdato.tilKortString()}",
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

