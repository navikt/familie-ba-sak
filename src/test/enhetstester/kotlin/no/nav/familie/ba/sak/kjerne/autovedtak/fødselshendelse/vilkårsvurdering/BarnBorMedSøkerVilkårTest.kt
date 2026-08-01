package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering

import no.nav.familie.ba.sak.datagenerator.lagGrVegadresse
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class BarnBorMedSøkerVilkårTest {
    @Test
    fun `Samme matrikkelId men ellers forskjellige adresser`() {
        // Arrange
        val faktaPerson = opprettFaktaPerson(adresseMatrikkelId1barn, adresseMatrikkelId1SøkerBruksenhetsnummer)

        // Act
        val evaluering = vilkår.vurderVilkår(faktaPerson)

        // Assert
        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    @Test
    fun `Forskjellige matrikkelId`() {
        // Arrange
        val faktaPerson = opprettFaktaPerson(adresseMatrikkelId1barn, adresseMatrikkelId2Søker)

        // Act
        val evaluering = vilkår.vurderVilkår(faktaPerson)

        // Assert
        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @Test
    fun `Address som mangler postnummer`() {
        // Arrange
        val faktaPerson = opprettFaktaPerson(adresseIkkePostnummerBarn, adresseIkkePostnummerSøker)

        // Act
        val evaluering = vilkår.vurderVilkår(faktaPerson)

        // Assert
        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @Test
    fun `Address som mangler matrikkelid`() {
        // Arrange
        val faktaPerson = opprettFaktaPerson(adresseAttrBarn, adresseAttrSøker)

        // Act
        val evaluering = vilkår.vurderVilkår(faktaPerson)

        // Assert
        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    @Test
    fun `To forskjellige address som begge mangler matrikkelid`() {
        // Arrange
        val faktaPerson = opprettFaktaPerson(adresseAttrBarn, adresseAttr2Søker)

        // Act
        val evaluering = vilkår.vurderVilkår(faktaPerson)

        // Assert
        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    private fun opprettFaktaPerson(
        bostedsadresseSøker: GrBostedsadresse,
        bostedsadresseBarn: GrBostedsadresse,
    ): Person {
        val barnMedAdresse = barn.copy(bostedsadresser = mutableListOf(bostedsadresseBarn))
        barnMedAdresse.personopplysningGrunnlag.personer.clear()
        barnMedAdresse.personopplysningGrunnlag.personer.add(
            søker.copy(
                bostedsadresser =
                    mutableListOf(
                        bostedsadresseSøker,
                    ),
            ),
        )

        return barnMedAdresse
    }

    companion object {
        val vilkår = Vilkår.BOR_MED_SØKER
        val barn = tilfeldigPerson(personType = PersonType.BARN)

        val søker = tilfeldigPerson(personType = PersonType.SØKER, kjønn = Kjønn.KVINNE)

        val adresseMatrikkelId1barn = lagGrVegadresse(1234L)
        val adresseMatrikkelId2Søker = lagGrVegadresse(4321L)
        val adresseMatrikkelId1SøkerBruksenhetsnummer = lagGrVegadresse(matrikkelId = 1234L, bruksenhetsnummer = "123")
        val adresseIkkePostnummerBarn = lagGrVegadresse(adressenavn = "Fågelveien", husnummer = "123")
        val adresseIkkePostnummerSøker = lagGrVegadresse(adressenavn = "Fågelveien", husnummer = "123")
        val adresseAttrBarn = lagGrVegadresse(adressenavn = "Fågelveien", husnummer = "123", postnummer = "0245")
        val adresseAttrSøker = lagGrVegadresse(adressenavn = "Fågelveien", husnummer = "123", postnummer = "0245")
        val adresseAttr2Søker = lagGrVegadresse(adressenavn = "Fågelveien", husnummer = "11", postnummer = "0245")
    }
}
