package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.Vilkår

enum class VilkårIkkeOppfyltÅrsak(val beskrivelse: String, val vilkår: Vilkår) {
    MOR_IKKE_BOSTATT_I_RIKET("Fødselshendelse: Mor er ikke bosatt i riket", Vilkår.BOSATT_I_RIKET),

    UNDER_18("Fødselshendelse: Barn over 18 år", Vilkår.UNDER_18_ÅR),

    BOR_MED_SØKER("Fødselshendelse: Barnet ikke bosatt med mor", Vilkår.BOR_MED_SØKER),

    GIFT_PARTNERSKAP("Fødselshendelse: Barnet er gift", Vilkår.GIFT_PARTNERSKAP),
}

