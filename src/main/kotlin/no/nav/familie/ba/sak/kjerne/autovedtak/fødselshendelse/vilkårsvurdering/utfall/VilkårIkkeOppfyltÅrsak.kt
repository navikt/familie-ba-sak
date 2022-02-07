package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.EvalueringÅrsak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

enum class VilkårIkkeOppfyltÅrsak(val beskrivelse: String, val metrikkBeskrivelse: String? = null, val vilkår: Vilkår) :
    EvalueringÅrsak {

    // Under 18 år
    ER_IKKE_UNDER_18_ÅR(beskrivelse = "Barn er ikke under 18 år", vilkår = Vilkår.UNDER_18_ÅR),

    // Bor med søker
    SØKER_ER_IKKE_MOR(beskrivelse = "Søker er ikke mor", vilkår = Vilkår.BOR_MED_SØKER),
    BARNET_BOR_IKKE_MED_MOR(beskrivelse = "Barnet bor ikke med mor", vilkår = Vilkår.BOR_MED_SØKER),

    // Gift eller partnerskap hos barn
    BARN_ER_GIFT_ELLER_HAR_PARTNERSKAP(
        beskrivelse = "Person er gift eller har registrert partner",
        vilkår = Vilkår.GIFT_PARTNERSKAP
    ),

    // Bosatt i riket
    BOR_IKKE_I_RIKET(beskrivelse = "Er ikke bosatt i riket", vilkår = Vilkår.BOSATT_I_RIKET),
    BOR_IKKE_I_RIKET_FLERE_ADRESSER_UTEN_FOM(
        beskrivelse = "Er ikke bosatt i riket, flere adresser uten fom",
        vilkår = Vilkår.BOSATT_I_RIKET
    ),

    // Lovlig opphold
    STATSBORGERSKAP_ANNEN_FORELDER_UKLART(
        beskrivelse = "Statsborgerskap for annen forelder kan ikke avgjøres.",
        metrikkBeskrivelse = "Statsborgerskap for annen forelder kan ikke avgjøres.",
        vilkår = Vilkår.LOVLIG_OPPHOLD
    ),
    TREDJELANDSBORGER_UTEN_LOVLIG_OPPHOLD(
        beskrivelse = "Mor har ikke lovlig opphold - tredjelandsborger.",
        metrikkBeskrivelse = "Mor tredjelandsborger",
        vilkår = Vilkår.LOVLIG_OPPHOLD
    ),
    STATSLØS(
        beskrivelse = "Mor har ikke lovlig opphold - er statsløs eller mangler statsborgerskap.",
        metrikkBeskrivelse = "Mor statsløs eller mangler statsborgerskap",
        vilkår = Vilkår.LOVLIG_OPPHOLD
    ),
    EØS_UKJENT_ANNEN_FORELDER(
        beskrivelse = "Mor er EØS-borger og har ikke løpende arbeidsforhold i Norge. Finner ikke annen forelder.",
        metrikkBeskrivelse = "Mor er EØS-borger, finner ikke annen forelder.",
        vilkår = Vilkår.LOVLIG_OPPHOLD
    ),
    EØS_BOR_IKKE_SAMMEN_MED_ANNEN_FORELDER(
        beskrivelse = "Mor er EØS-borger og har ikke løpende arbeidsforhold i Norge. Finner ikke annen forelder.",
        metrikkBeskrivelse = "Mor er EØS-borger, finner ikke annen forelder.",
        vilkår = Vilkår.LOVLIG_OPPHOLD
    ),
    ANNEN_FORELDER_EØS_MEN_IKKE_MED_LØPENDE_ARBEIDSFORHOLD(
        beskrivelse = "Annen forelder er fra EØS, men har ikke et løpende arbeidsforhold i Norge.",
        metrikkBeskrivelse = "Annen forelder er fra EØS, men har ikke et løpende arbeidsforhold i Norge.",
        vilkår = Vilkår.LOVLIG_OPPHOLD
    ),
    EØS_MEDFORELDER_TREDJELANDSBORGER(
        beskrivelse = "Mor har ikke lovlig opphold - EØS borger. Mor er ikke registrert med arbeidsforhold. Medforelder er tredjelandsborger.",
        metrikkBeskrivelse = "Mor EØS. Ikke arb. MF tredjelandsborger",
        vilkår = Vilkår.LOVLIG_OPPHOLD
    ),
    EØS_MEDFORELDER_STATSLØS(
        beskrivelse = "Mor har ikke lovlig opphold - EØS borger. Mor er ikke registrert med arbeidsforhold. Medforelder er statsløs.",
        metrikkBeskrivelse = "Mor EØS. Ikke arb. MF statsløs",
        vilkår = Vilkår.LOVLIG_OPPHOLD
    );

    override fun hentBeskrivelse(): String {
        return beskrivelse
    }

    override fun hentMetrikkBeskrivelse(): String {
        return metrikkBeskrivelse ?: beskrivelse
    }

    override fun hentIdentifikator(): String {
        return vilkår.name
    }
}
