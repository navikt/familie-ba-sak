package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import no.nav.familie.ba.sak.common.Utils
import javax.persistence.AttributeConverter
import javax.persistence.Converter

enum class UtdypendeVilkårsvurdering(val tekst: String) {
    VURDERING_ANNET_GRUNNLAG(tekst = "Vurdering annet grunnlag"),
    VURDERT_MEDLEMSKAP(tekst = "Vurdert medlemskap"),
    DELT_BOSTED(tekst = "Delt bosted"),
}

@Converter
class UtdypendeVilkårsvurderingerConverter : AttributeConverter<List<UtdypendeVilkårsvurdering>, String> {

    override fun convertToDatabaseColumn(enumListe: List<UtdypendeVilkårsvurdering>) =
        Utils.konverterEnumsTilString(enumListe)

    override fun convertToEntityAttribute(string: String?): List<UtdypendeVilkårsvurdering> =
        Utils.konverterStringTilEnums(string)
}
