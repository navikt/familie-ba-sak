package no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering

import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BosattIRiketVilkårTest {

    val defaultAdresse = Bostedsadresse(
        angittFlyttedato = LocalDate.parse("2020-07-13"),
        gyldigTilOgMed = null,
        matrikkeladresse = Matrikkeladresse(
            matrikkelId = 123L,
            bruksenhetsnummer = "H301",
            tilleggsnavn = "navn",
            postnummer = "0202",
            kommunenummer = "2231"
        )
    )

    @Test
    fun `Skal sjekke at person bor i riket dersom vedkommende har vært utvandret langt tilbake i tid`() {
        val søker = tilfeldigPerson(personType = PersonType.BARN)

        søker.apply {
            bostedsadresser = mutableListOf(
                GrBostedsadresse.fraBostedsadresse(
                    defaultAdresse.copy(
                        angittFlyttedato = LocalDate.parse("2019-01-19")
                    ),
                    søker
                ),
                GrBostedsadresse.fraBostedsadresse(
                    defaultAdresse.copy(
                        angittFlyttedato = LocalDate.parse("2011-06-02")
                    ),
                    søker
                ),
                GrBostedsadresse.fraBostedsadresse(
                    defaultAdresse.copy(
                        angittFlyttedato = LocalDate.parse("1988-06-23"),
                        gyldigTilOgMed = LocalDate.parse("2006-01-01")
                    ),
                    søker
                ),
                GrBostedsadresse.fraBostedsadresse(
                    defaultAdresse.copy(
                        angittFlyttedato = LocalDate.parse("2013-09-22")
                    ),
                    søker
                ),
                GrBostedsadresse.fraBostedsadresse(
                    defaultAdresse.copy(
                        angittFlyttedato = LocalDate.parse("2016-10-01")
                    ),
                    søker
                ),
                GrBostedsadresse.fraBostedsadresse(
                    defaultAdresse.copy(
                        angittFlyttedato = LocalDate.parse("2012-07-11")
                    ),
                    søker
                ),
                GrBostedsadresse.fraBostedsadresse(
                    defaultAdresse.copy(
                        angittFlyttedato = LocalDate.parse("2006-06-04")
                    ),
                    søker
                ),
                GrBostedsadresse.fraBostedsadresse(
                    defaultAdresse.copy(
                        angittFlyttedato = LocalDate.parse("2011-06-01")
                    ),
                    søker
                ),
                GrBostedsadresse.fraBostedsadresse(
                    defaultAdresse.copy(
                        angittFlyttedato = LocalDate.parse("2020-07-13")
                    ),
                    søker
                ),
                GrBostedsadresse.fraBostedsadresse(
                    defaultAdresse.copy(
                        angittFlyttedato = LocalDate.parse("2020-06-08")
                    ),
                    søker
                ),
            )
        }

        val evaluering = VurderPersonErBosattIRiket(
            adresser = søker.bostedsadresser,
            vurderFra = LocalDate.now().minusDays(1)
        ).vurder()

        assertEquals(Resultat.OPPFYLT, evaluering.resultat)
    }

    @Test
    fun `Skal sjekke at person ikke bor i riket`() {
        val søker = tilfeldigPerson(personType = PersonType.BARN)

        søker.apply {
            bostedsadresser = mutableListOf(
                GrBostedsadresse.fraBostedsadresse(
                    defaultAdresse.copy(
                        angittFlyttedato = LocalDate.parse("2019-01-19"),
                        gyldigTilOgMed = LocalDate.parse("2021-05-01")
                    ),
                    søker
                ),
                GrBostedsadresse.fraBostedsadresse(
                    defaultAdresse.copy(
                        angittFlyttedato = LocalDate.parse("2011-06-02"),
                    ),
                    søker
                ),
            )
        }

        val evaluering = VurderPersonErBosattIRiket(
            adresser = søker.bostedsadresser,
            vurderFra = LocalDate.now().minusDays(1)
        ).vurder()

        assertEquals(Resultat.IKKE_OPPFYLT, evaluering.resultat)
    }

    @Test
    fun `Skal sjekke at person ikke bor i riket dersom vedkommende har vært utvandret`() {
        val søker = tilfeldigPerson(personType = PersonType.BARN)

        søker.apply {
            bostedsadresser = mutableListOf(
                GrBostedsadresse.fraBostedsadresse(
                    defaultAdresse.copy(
                        angittFlyttedato = LocalDate.now().minusMonths(3),
                        gyldigTilOgMed = LocalDate.now().minusMonths(2),
                    ),
                    søker
                ),
                GrBostedsadresse.fraBostedsadresse(
                    defaultAdresse.copy(
                        angittFlyttedato = LocalDate.now().minusMonths(1),
                    ),
                    søker
                ),
            )
        }

        val evaluering = VurderPersonErBosattIRiket(
            adresser = søker.bostedsadresser,
            vurderFra = LocalDate.now().minusMonths(4)
        ).vurder()

        assertEquals(Resultat.IKKE_OPPFYLT, evaluering.resultat)
    }

    @Test
    fun `Skal sjekke at person ikke bor i riket dersom vedkommende har vært utvandret først i perioden`() {
        val søker = tilfeldigPerson(personType = PersonType.BARN)

        søker.apply {
            bostedsadresser = mutableListOf(
                GrBostedsadresse.fraBostedsadresse(
                    defaultAdresse.copy(
                        angittFlyttedato = LocalDate.now().minusMonths(3),
                    ),
                    søker
                ),
            )
        }

        val evaluering = VurderPersonErBosattIRiket(
            adresser = søker.bostedsadresser,
            vurderFra = LocalDate.now().minusMonths(4)
        ).vurder()

        assertEquals(Resultat.IKKE_OPPFYLT, evaluering.resultat)
    }

    @Test
    fun `Skal sjekke at person ikke bor i riket dersom vedkommende har vært utvandret sist i perioden`() {
        val søker = tilfeldigPerson(personType = PersonType.BARN)

        søker.apply {
            bostedsadresser = mutableListOf(
                GrBostedsadresse.fraBostedsadresse(
                    defaultAdresse.copy(
                        angittFlyttedato = LocalDate.now().minusMonths(7),
                        gyldigTilOgMed = LocalDate.now().minusMonths(2),
                    ),
                    søker
                ),
            )
        }

        val evaluering = VurderPersonErBosattIRiket(
            adresser = søker.bostedsadresser,
            vurderFra = LocalDate.now().minusMonths(4)
        ).vurder()

        assertEquals(Resultat.IKKE_OPPFYLT, evaluering.resultat)
    }

    @Test
    fun `Skal sjekke at person bor i riket selv om hen har ekstra adresse uten fom`() {
        val søker = tilfeldigPerson(personType = PersonType.BARN)

        søker.apply {
            bostedsadresser = mutableListOf(
                GrBostedsadresse.fraBostedsadresse(
                    defaultAdresse.copy(
                        angittFlyttedato = null,
                    ),
                    søker
                ),
                GrBostedsadresse.fraBostedsadresse(
                    defaultAdresse.copy(
                        angittFlyttedato = LocalDate.now().minusMonths(7),
                    ),
                    søker
                ),
            )
        }

        val evaluering = VurderPersonErBosattIRiket(
            adresser = søker.bostedsadresser,
            vurderFra = LocalDate.now().minusMonths(4)
        ).vurder()

        assertEquals(Resultat.OPPFYLT, evaluering.resultat)
    }
}
