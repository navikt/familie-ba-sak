package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import no.nav.familie.ba.sak.common.Utils
import javax.persistence.AttributeConverter
import javax.persistence.Converter

enum class UtdypendeVilkårsvurdering(val tekst: String) {
    VURDERING_ANNET_GRUNNLAG(tekst = "Vurdering annet grunnlag"),
    VURDERT_MEDLEMSKAP(tekst = "Vurdert medlemskap"),
    DELT_BOSTED(tekst = "Delt bosted: skal deles"),
    DELT_BOSTED_SKAL_IKKE_DELES(tekst = "Delt bosted: skal ikke deles"),
    OMFATTET_AV_NORSK_LOVGIVNING(tekst = "Omfattet av norsk lovgivning"),
    OMFATTET_AV_NORSK_LOVGIVNING_UTLAND(tekst = "Omfattet av norsk lovgivning Utland"),
    BARN_BOR_I_NORGE(tekst = "Barn bor i Norge"),
    BARN_BOR_I_EØS(tekst = "Barn bor i EØS-land"),
    BARN_BOR_I_STORBRITANNIA(tekst = "Barn bor i Storbritannia"),
    BARN_BOR_I_NORGE_MED_SØKER(tekst = "Barn bor i Norge med søker"),
    BARN_BOR_I_EØS_MED_SØKER(tekst = "Barn bor i EØS-land med søker"),
    BARN_BOR_I_EØS_MED_ANNEN_FORELDER(tekst = "Barn bor i EØS-land med annen forelder"),
    BARN_BOR_I_STORBRITANNIA_MED_SØKER(tekst = "Barn bor i Storbritannia med søker"),
    BARN_BOR_I_STORBRITANNIA_MED_ANNEN_FORELDER(tekst = "Barn bor i Storbritannia med søker"),
    BARN_BOR_ALENE_I_ANNET_EØS_LAND(tekst = "Barn bor alene i annet EØS-land")
}

@Converter
class UtdypendeVilkårsvurderingerConverter : AttributeConverter<List<UtdypendeVilkårsvurdering>, String> {

    override fun convertToDatabaseColumn(enumListe: List<UtdypendeVilkårsvurdering>) =
        Utils.konverterEnumsTilString(enumListe)

    override fun convertToEntityAttribute(string: String?): List<UtdypendeVilkårsvurdering> =
        Utils.konverterStringTilEnums(string)
}
