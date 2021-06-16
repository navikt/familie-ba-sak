package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse.Companion.sisteAdresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrMatrikkeladresse
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.Feil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class BostedsadresseTest {

    @Test
    fun `Skal adresse med mest nylig fom-dato`() {
        val adresse = GrMatrikkeladresse(matrikkelId = null,
                                         bruksenhetsnummer = "H301",
                                         tilleggsnavn = "navn",
                                         postnummer = "0202",
                                         kommunenummer = "2231")
        val adresseMedNullFom = adresse.copy().apply { periode = DatoIntervallEntitet(fom = null) }
        val adresseMedEldreDato = adresse.copy().apply { periode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(3)) }
        val adresseMedNyereDato = adresse.copy().apply { periode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(1)) }
        val adresserTilSortering =
                mutableListOf(adresseMedEldreDato, adresseMedNyereDato, adresseMedNullFom) as MutableList<GrBostedsadresse>
        assertEquals(adresseMedNyereDato, adresserTilSortering.sisteAdresse())
    }

    @Test
    fun `Skal returnere adresse uten datoer når dette er eneste`() {
        val adresse = GrMatrikkeladresse(matrikkelId = null,
                                         bruksenhetsnummer = "H301",
                                         tilleggsnavn = "navn",
                                         postnummer = "0202",
                                         kommunenummer = "2231").apply {
            periode = DatoIntervallEntitet(fom = null)
        } as GrBostedsadresse
        assertEquals(adresse, mutableListOf(adresse).sisteAdresse())
    }

    @Test
    fun `Skal kaste feil hvis det finnes flere adresser uten datoer`() {
        val adresse1 = GrMatrikkeladresse(matrikkelId = null,
                                          bruksenhetsnummer = "H301",
                                          tilleggsnavn = "navn",
                                          postnummer = "0202",
                                          kommunenummer = "2231").apply {
            periode = DatoIntervallEntitet(fom = null)
        } as GrBostedsadresse
        val adresse2 = GrMatrikkeladresse(matrikkelId = null,
                                          bruksenhetsnummer = "H301",
                                          tilleggsnavn = "navn",
                                          postnummer = "0202",
                                          kommunenummer = "2231").apply {
            periode = DatoIntervallEntitet(fom = null)
        } as GrBostedsadresse
        assertThrows<Feil> { mutableListOf(adresse1, adresse2).sisteAdresse() }
    }

}
