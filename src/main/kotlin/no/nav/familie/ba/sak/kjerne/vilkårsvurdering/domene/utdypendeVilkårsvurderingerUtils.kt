package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import no.nav.familie.ba.sak.common.Utils
import javax.persistence.AttributeConverter
import javax.persistence.Converter

enum class UtdypendeVilkårsvurderingType(val tekst: String) {
    VURDERING_ANNET_GRUNNLAG(tekst = "Vurdering annet grunnlag"),
    VURDERT_MEDLEMSKAP(tekst = "Vurdert medlemskap"),
    NORSK_LOVGIVNING(tekst = "Omfattet av norsk lovgivning"),
    UTLAND(tekst = "Utland"),
    IKKE_NORSK_LOVGIVNING(tekst = "Ikke omfattet av norsk lovgivning"),
    IKKE_BOSATT_EØS_ELLER_EØS_BORGER(tekst = "Ikke bosatt i et EØS-land / EØS-borger"),
    BOSATT_EØS(tekst = "Bosatt i et EØS-land"),
    IKKE_BOSATT_EØS(tekst = "Ikke bosatt i et EØS-land"),
    DELT_BOSTED(tekst = "Delt bosted"),
    DELT_BOSTED_SKAL_DELE(tekst = "Delt bosted: Skal deles"),
    DELT_BOSTED_SKAL_IKKE_DELE(tekst = "Delt bosted: Skal ikke deles"),
    DELT_BOSTED_EØS(tekst = "Delt bosted (kan også eksistere i EØS saker)"),
    VURDERING_ANNET_GRUNNLAG_EØS(tekst = "Vurdering annet grunnlag (omsorgsrett for barn som bor med søker/ektefelle i et annet EØS-land)"),
    EØS_BOR_MED_SØKER(tekst = "Barnet bor med søker i EØS-land"),
    EØS_BOR_MED_ANNEN_FORELDER(tekst = "Barn bor med annen forelder i EØS-land"),
    INGEN_OMSORGSRETT_ELLER_FORELDREANSVAR(tekst = "Har ikke lenger omsorgsrett eller foreldreansvar for barnet"),
    BARN_DØD(tekst = "Barnet er død"),
    SEPARERT(tekst = "Separert"),
    SKILT(tekst = "Skilt"),
    BRUDD_SAMBOERFORHOLD(tekst = "Brudd i samboerforhold"),
    ALENE_ETTER_FØDSEL(tekst = "Alene etter fødsel"),
    DØDSFALL(tekst = "Dødsfall"),
    FENGSEL_VARETEKT(tekst = "Fengsel eller varetekt"),
    FORVALING_ELLER_TVUNGET_PSYKISK_HELSEVERN(tekst = "Forvaring eller tvungent psykisk helsevern"),
    FAKTISK_SEPARASJON(tekst = "Faktisk separasjon"),
    ENSLIG_MINDREÅRIG_FLYKTNING(tekst = "Enslig mindreårig flykning"),
    FORSVUNNET(tekst = "Forsvunnet"),
    GIFT(tekst = "Gift"),
    SAMBOER(tekst = "Samboer"),
    NYTT_BARN(tekst = "Nytt barn"),
}

@Converter
class UtdypendeVilkårsvurderingerConverter : AttributeConverter<List<UtdypendeVilkårsvurderingType>, String> {

    override fun convertToDatabaseColumn(enumListe: List<UtdypendeVilkårsvurderingType>) =
        Utils.konverterEnumsTilString(enumListe)

    override fun convertToEntityAttribute(string: String?): List<UtdypendeVilkårsvurderingType> =
        Utils.konverterStringTilEnums(string)
}
