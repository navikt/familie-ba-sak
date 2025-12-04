package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.common.Utils.avrundetHeltallAvProsent
import no.nav.familie.ba.sak.common.Utils.hentPropertyFraMaven
import no.nav.familie.ba.sak.common.Utils.storForbokstavIAlleNavn
import no.nav.familie.ba.sak.common.Utils.storForbokstavIHvertOrd
import no.nav.familie.ba.sak.common.Utils.tilEtterfølgendePar
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.bostedsadresse.GrVegadresseBostedsadresse
import no.nav.familie.ba.sak.kjerne.personident.Identkonverterer.er11Siffer
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilBrevTekst
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

internal class UtilsTest {
    @Test
    fun `skal regne ut prosent og gi heltall med riktig avrunding`() {
        assertEquals(200, 200.toBigDecimal().avrundetHeltallAvProsent(100.toBigDecimal()))
        assertEquals(100, 200.toBigDecimal().avrundetHeltallAvProsent(50.toBigDecimal()))
        assertEquals(201, BigDecimal(201.4).avrundetHeltallAvProsent(100.toBigDecimal()))
        assertEquals(202, BigDecimal(201.5).avrundetHeltallAvProsent(100.toBigDecimal()))
    }

    @Test
    fun `Navn i uppercase blir formatert korrekt`() = assertEquals("Store Bokstaver Her", "STORE BOKSTAVER HER ".storForbokstavIHvertOrd())

    @Test
    fun `Navn i uppercase med mellomrom og bindestrek blir formatert korrekt`() = assertEquals("Hense-Ravnen Hopp", "HENSE-RAVNEN HOPP".storForbokstavIAlleNavn())

    @Test
    fun `Nullable verdier blir tom string`() {
        val adresse =
            GrVegadresseBostedsadresse(
                matrikkelId = null,
                bruksenhetsnummer = null,
                husnummer = "1",
                kommunenummer = null,
                tilleggsnavn = null,
                adressenavn = "TEST",
                husbokstav = null,
                postnummer = "1234",
                poststed = null,
            )

        assertEquals("Test 1, 1234", adresse.tilFrontendString())
    }

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

        assertEquals(
            førsteBarn.fødselsdato.tilKortString(),
            listOf(førsteBarn.fødselsdato).tilBrevTekst(),
        )
    }

    @Test
    fun `Test transformering av to personer til brevtekst`() {
        val førsteBarn = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(6))
        val andreBarn = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(6))

        assertEquals(
            "${førsteBarn.fødselsdato.tilKortString()} og ${andreBarn.fødselsdato.tilKortString()}",
            listOf(førsteBarn.fødselsdato, andreBarn.fødselsdato).tilBrevTekst(),
        )
    }

    @Test
    fun `Test transformering av tre personer til brevtekst`() {
        val førsteBarn = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(6))
        val andreBarn = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(6))
        val tredjeBarn = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(6))

        assertEquals(
            "${førsteBarn.fødselsdato.tilKortString()}, ${andreBarn.fødselsdato.tilKortString()} og ${tredjeBarn.fødselsdato.tilKortString()}",
            listOf(førsteBarn.fødselsdato, andreBarn.fødselsdato, tredjeBarn.fødselsdato).tilBrevTekst(),
        )
    }

    @Test
    fun `Sjekker om ident er 11 siffer`() {
        assertFalse(er11Siffer("abc"))
        assertFalse(er11Siffer(""))
        assertFalse(er11Siffer("12345"))
        assertFalse(er11Siffer("1234567890A"))
        assertFalse(er11Siffer("1234567890123"))
        assertTrue(er11Siffer("12345678901"))
    }

    @Nested
    inner class TilEtterfølgendePar {
        @Test
        fun `skal plukke ut to og to etterfølgende elementer inkludert det siste elementet`() {
            val liste = listOf(1, 2, 3, 4, 5)
            val par = liste.tilEtterfølgendePar { a, b -> a to b }
            assertThat(par).hasSize(5)
            assertThat(par[0]).isEqualTo(Pair(1, 2))
            assertThat(par[1]).isEqualTo(Pair(2, 3))
            assertThat(par[2]).isEqualTo(Pair(3, 4))
            assertThat(par[3]).isEqualTo(Pair(4, 5))
            assertThat(par[4]).isEqualTo(Pair(5, null))
        }
    }
}
