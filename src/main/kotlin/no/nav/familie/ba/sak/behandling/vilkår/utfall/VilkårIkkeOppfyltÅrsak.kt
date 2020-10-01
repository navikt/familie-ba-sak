package no.nav.familie.ba.sak.behandling.vilkår.utfall

import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.nare.EvalueringÅrsak

enum class VilkårIkkeOppfyltÅrsak(val beskrivelse: String, val metrikkBeskrivelse: String? = null, val vilkår: Vilkår) :
        EvalueringÅrsak {

    // Under 18 år
    ER_IKKE_UNDER_18_ÅR(beskrivelse = "Barn er ikke under 18 år", vilkår = Vilkår.UNDER_18_ÅR),

    // Bor med søker
    SØKER_ER_IKKE_MOR(beskrivelse = "Søker er ikke mor", vilkår = Vilkår.BOR_MED_SØKER),
    BARNET_BOR_IKKE_MED_SØKER(beskrivelse = "Barnet bor ikke med mor", vilkår = Vilkår.BOR_MED_SØKER),

    // Gift eller partnerskap hos barn
    BARN_ER_GIFT_ELLER_HAR_PARTNERSKAP(beskrivelse = "Person er gift eller har registrert partner",
                                       vilkår = Vilkår.GIFT_PARTNERSKAP),

    // Bosatt i riket
    MOR_BOR_IKKE_I_RIKET(beskrivelse = "Mor er ikke bosatt i riket", vilkår = Vilkår.BOSATT_I_RIKET),

    // Lovlig opphold
    STATSBORGERSKAP_ANNEN_FORELDER_UKLART(beskrivelse = "Statsborgerskap for annen forelder kan ikke avgjøres.",
                                          metrikkBeskrivelse = "Statsborgerskap for annen forelder kan ikke avgjøres.",
                                          vilkår = Vilkår.LOVLIG_OPPHOLD),
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
    EØS_IKKE_REGISTRERT_MEDFORELDER_OG_MOR_IKKE_INNFRIDD_BOTIDSKRAV(
            beskrivelse = "Mor har ikke lovlig opphold - EØS borger. Mor er ikke registrert med arbeidsforhold. Det er ikke registrert medforelder på barnet. Mor har ikke hatt bostedsadresse i Norge i mer enn fem år.",
            metrikkBeskrivelse = "Mor EØS. Ikke arb. MF ikke reg. Mor ikke bosatt 5 år",
            vilkår = Vilkår.LOVLIG_OPPHOLD
    ),
    EØS_IKKE_REGISTRERT_MEDFORELDER_OG_MOR_IKKE_INNFRIDD_ARBEIDSMENGDE(
            beskrivelse = "Mor har ikke lovlig opphold - EØS borger. Mor er ikke registrert med arbeidsforhold. Det er ikke registrert medforelder på barnet. Mor har ikke hatt arbeidsforhold i Norge de siste fem årene.",
            metrikkBeskrivelse = "Mor EØS. Ikke arb. MF ikke reg. Mor ikke arbeid 5 år",
            vilkår = Vilkår.LOVLIG_OPPHOLD
    ),
    EØS_BOR_IKKE_SAMMEN_MED_MEDFORELDER_OG_MOR_IKKE_INNFRIDD_BOTIDSKRAV(
            beskrivelse = "Mor har ikke lovlig opphold - EØS borger. Mor er ikke registrert med arbeidsforhold. Barnets mor og medforelder har ikke felles bostedsadresse. Mor har ikke hatt bostedsadresse i Norge i mer enn fem år.",
            metrikkBeskrivelse = "Mor EØS. Ikke arb. Bor ikke med MF. Mor ikke bosatt 5 år",
            vilkår = Vilkår.LOVLIG_OPPHOLD
    ),
    EØS_BOR_IKKE_SAMMEN_MED_MEDFORELDER_OG_MOR_IKKE_INNFRIDD_ARBEIDSMENGDE(
            beskrivelse = "Mor har ikke lovlig opphold - EØS borger. Mor er ikke registrert med arbeidsforhold. Barnets mor og medforelder har ikke felles bostedsadresse. Mor har ikke hatt arbeidsforhold i Norge de siste fem årene.",
            metrikkBeskrivelse = "Mor EØS. Ikke arb. Bor ikke med MF. Mor ikke arbeid 5 år",
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
    ),
    EØS_MEDFORELDER_IKKE_I_ARBEID_OG_MOR_IKKE_INNFRIDD_BOTIDSKRAV(
            beskrivelse = "Mor har ikke lovlig opphold - EØS borger. Mor er ikke registrert med arbeidsforhold. Medforelder er ikke registrert med arbeidsforhold i Norge. Mor har ikke hatt bostedsadresse i Norge i mer enn fem år.",
            metrikkBeskrivelse = "Mor EØS. Ikke arb. MF EØS ikke arbeid. Mor ikke bosatt 5 år",
            vilkår = Vilkår.LOVLIG_OPPHOLD
    ),
    EØS_MEDFORELDER_IKKE_I_ARBEID_OG_MOR_IKKE_INNFRIDD_ARBEIDSMENGDE(
            beskrivelse = "Mor har ikke lovlig opphold - EØS borger. Mor er ikke registrert med arbeidsforhold. Medforelder er ikke registrert med arbeidsforhold i Norge. Mor har ikke hatt arbeidsforhold i Norge de siste fem årene.",
            metrikkBeskrivelse = "Mor EØS. Ikke arb. MF EØS ikke arbeid. Mor ikke arbeid 5 år",
            vilkår = Vilkår.LOVLIG_OPPHOLD
    );

    override fun hentBeskrivelse(): String {
        return beskrivelse
    }

    override fun hentMetrikkBeskrivelse(): String {
        return metrikkBeskrivelse ?: beskrivelse
    }

    override fun hentIdentifikator(): String {
        return vilkår.spesifikasjon.identifikator
    }
}