package no.nav.familie.ba.sak.ekstern

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse.Companion.fregManglendeFlytteDato
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrVegadresse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RestMappingTest {

    @Test
    fun `Manglende angitt flyttedato fra freg mappes som manglende dato`() {
        val adresseUtenFlyttedato = GrVegadresse(matrikkelId = 1234,
                                                 husnummer = "11",
                                                 husbokstav = "B",
                                                 bruksenhetsnummer = "H022",
                                                 adressenavn = "Adressenavn",
                                                 kommunenummer = "1232",
                                                 tilleggsnavn = "noe",
                                                 postnummer = "4322")
                .apply { periode = DatoIntervallEntitet(fom = fregManglendeFlytteDato) }


        val flyttedato = LocalDate.of(2000, 1, 1)
        val adresseMedFlyttedato = GrVegadresse(matrikkelId = 1234,
                                                husnummer = "11",
                                                husbokstav = "B",
                                                bruksenhetsnummer = "H022",
                                                adressenavn = "Adressenavn",
                                                kommunenummer = "1232",
                                                tilleggsnavn = "noe",
                                                postnummer = "4322")
                .apply { periode = DatoIntervallEntitet(fom = flyttedato) }

        assertEquals(null, adresseUtenFlyttedato.tilRestRegisteropplysning().fom)
        assertEquals(flyttedato, adresseMedFlyttedato.tilRestRegisteropplysning().fom)
    }
}