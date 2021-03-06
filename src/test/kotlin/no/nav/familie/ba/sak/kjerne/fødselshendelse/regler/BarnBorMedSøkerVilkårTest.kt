package no.nav.familie.ba.sak.kjerne.fødselshendelse.regler

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.*
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrVegadresse
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.FaktaTilVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.Vilkår
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class BarnBorMedSøkerVilkårTest {

    @Test
    fun `Samme matrikkelId men ellers forskjellige adresser`() {
        val fakta = opprettFaktaObject(adresseMatrikkelId1barn, adresseMatrikkelId1SøkerBruksenhetsnummer)

        val evaluering = vilkår.spesifikasjon.evaluer(fakta)
        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    @Test
    fun `Forskjellige matrikkelId`() {
        val fakta = opprettFaktaObject(adresseMatrikkelId1barn, adresseMatrikkelId2Søker)

        val evaluering = vilkår.spesifikasjon.evaluer(fakta)
        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @Test
    fun `Address som mangler postnummer`() {
        val fakta = opprettFaktaObject(adresseIkkePostnummerBarn, adresseIkkePostnummerSøker)

        val evaluering = vilkår.spesifikasjon.evaluer(fakta)
        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @Test
    fun `Address som mangler matrikkelid`() {
        val fakta = opprettFaktaObject(adresseAttrBarn, adresseAttrSøker)

        val evaluering = vilkår.spesifikasjon.evaluer(fakta)
        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    @Test
    fun `To forskjellige address som begge mangler matrikkelid`() {
        val fakta = opprettFaktaObject(adresseAttrBarn, adresseAttr2Søker)

        val evaluering = vilkår.spesifikasjon.evaluer(fakta)
        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    private fun opprettFaktaObject(bostedsadresseSøker: GrBostedsadresse, bostedsadresseBarn: GrBostedsadresse): FaktaTilVilkårsvurdering {
        val barnMedAdresse = barn.copy(bostedsadresser = mutableListOf(bostedsadresseBarn))
        barnMedAdresse.personopplysningGrunnlag.personer.clear()
        barnMedAdresse.personopplysningGrunnlag.personer.add(søker.copy(bostedsadresser = mutableListOf(bostedsadresseSøker)))

        return FaktaTilVilkårsvurdering(personForVurdering = barnMedAdresse)
    }

    companion object {
        val vilkår = Vilkår.BOR_MED_SØKER
        val barn = tilfeldigPerson(personType = PersonType.BARN)

        val søker = tilfeldigPerson(personType = PersonType.SØKER, kjønn = Kjønn.KVINNE)

        val adresseMatrikkelId1barn = opprettAdresse(1234L)
        val adresseMatrikkelId2Søker = opprettAdresse(4321L)
        val adresseMatrikkelId1SøkerBruksenhetsnummer = opprettAdresse(matrikkelId = 1234L, bruksenhetsnummer = "123")
        val adresseIkkePostnummerBarn = opprettAdresse(adressenavn = "Fågelveien", husnummer = "123")
        val adresseIkkePostnummerSøker = opprettAdresse(adressenavn = "Fågelveien", husnummer = "123")
        val adresseAttrBarn = opprettAdresse(adressenavn = "Fågelveien", husnummer = "123", postnummer = "0245")
        val adresseAttrSøker = opprettAdresse(adressenavn = "Fågelveien", husnummer = "123", postnummer = "0245")
        val adresseAttr2Søker = opprettAdresse(adressenavn = "Fågelveien", husnummer = "11", postnummer = "0245")

        private fun opprettAdresse(matrikkelId: Long? = null,
                                   bruksenhetsnummer: String? = null,
                                   adressenavn: String? = null,
                                   husnummer: String? = null,
                                   husbokstav: String? = null,
                                   postnummer: String? = null) =
                GrVegadresse(matrikkelId = matrikkelId,
                             husnummer = husnummer,
                             husbokstav = husbokstav,
                             bruksenhetsnummer = bruksenhetsnummer,
                             adressenavn = adressenavn,
                             kommunenummer = null,
                             tilleggsnavn = null,
                             postnummer = postnummer)
    }
}