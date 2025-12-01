package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService.IdentMedAdressebeskyttelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DiskresjonskodeUtilsTest {
    val personBarnUtenDiskresjonskode =
        IdentMedAdressebeskyttelse(
            ident = "BARN_IDENT_UTEN",
            adressebeskyttelsegradering = null,
            personType = PersonType.BARN,
        )
    val personBarnFortrolig =
        IdentMedAdressebeskyttelse(
            ident = "BARN_IDENT_FORTROLIG",
            adressebeskyttelsegradering = ADRESSEBESKYTTELSEGRADERING.FORTROLIG,
            personType = PersonType.BARN,
        ) // Kode 7
    val personBarnStrengtFortrolig =
        IdentMedAdressebeskyttelse(
            ident = "BARN_IDENT_STRENGT_FORTROLIG",
            adressebeskyttelsegradering = ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG,
            personType = PersonType.BARN,
        ) // Kode 6
    val personSøkerStrengtFortrolig =
        IdentMedAdressebeskyttelse(
            ident = "SØKER_IDENT_STRENGT_FORTROLIG",
            adressebeskyttelsegradering = ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG,
            personType = PersonType.SØKER,
        ) // Kode 6

    @Test
    fun `ingen har adressebeskyttelse - skal gi null`() {
        assertEquals(
            null,
            finnPersonMedStrengesteAdressebeskyttelse(
                listOf(
                    personBarnUtenDiskresjonskode,
                    personBarnUtenDiskresjonskode,
                ),
            ),
        )
    }

    @Test
    fun `en har adressebeskyttelse STRENGT_FORTROLIG, en har adressebeskyttelse FORTROLIG, en har ingen adressebeskyttelse - skal gi adressebeskyttelse STRENGT_FORTROLIG`() {
        assertEquals(
            "BARN_IDENT_STRENGT_FORTROLIG",
            finnPersonMedStrengesteAdressebeskyttelse(
                listOf(
                    personBarnFortrolig,
                    personBarnStrengtFortrolig,
                    personBarnUtenDiskresjonskode,
                ),
            ),
        )
    }

    @Test
    fun `barn har adressebeskyttelse STRENGT_FORTROLIG, barn har adressebeskyttelse FORTROLIG, søker har adressebeskyttelse STRENGT_FORTROLIG - skal gi søker med adressebeskyttelse STRENGT_FORTROLIG`() {
        assertEquals(
            "SØKER_IDENT_STRENGT_FORTROLIG",
            finnPersonMedStrengesteAdressebeskyttelse(
                listOf(
                    personBarnStrengtFortrolig,
                    personSøkerStrengtFortrolig,
                    personBarnFortrolig,
                ),
            ),
        )
    }

    @Test
    fun `en har adressebeskyttelse FORTROLIG, en har ingen adressebeskyttelse - skal gi adressebeskyttelse FORTROLIG`() {
        assertEquals(
            "BARN_IDENT_FORTROLIG",
            finnPersonMedStrengesteAdressebeskyttelse(
                listOf(
                    personBarnUtenDiskresjonskode,
                    personBarnFortrolig,
                ),
            ),
        )
    }

    @Test
    fun `tom liste - skal gi null`() {
        assertEquals(
            null,
            finnPersonMedStrengesteAdressebeskyttelse(listOf()),
        )
    }
}
