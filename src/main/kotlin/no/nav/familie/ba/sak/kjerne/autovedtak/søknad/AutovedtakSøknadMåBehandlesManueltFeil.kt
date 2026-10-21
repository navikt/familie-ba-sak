package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOR_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.GIFT_PARTNERSKAP
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.LOVLIG_OPPHOLD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.UNDER_18_ÅR
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.UTVIDET_BARNETRYGD

class AutovedtakSøknadMåBehandlesManueltFeil(
    beskrivelse: String,
    val årsaker: List<AutovedtakSøknadMåBehandlesManueltÅrsak>,
) : AutovedtakMåBehandlesManueltFeil(beskrivelse) {
    constructor(
        beskrivelse: String,
        årsak: AutovedtakSøknadMåBehandlesManueltÅrsak,
    ) : this(beskrivelse, listOf(årsak))
}

enum class AutovedtakSøknadMåBehandlesManueltÅrsak {
    SØKER_VILKÅR_GIFT_PARTNERSKAP,
    SØKER_VILKÅR_BOSATT_I_RIKET,
    SØKER_VILKÅR_LOVLIG_OPPHOLD,
    SØKER_VILKÅR_UTVIDET_BARNETRYGD,
    BARN_VILKÅR_UNDER_18_ÅR,
    BARN_VILKÅR_BOR_MED_SØKER,
    BARN_VILKÅR_GIFT_PARTNERSKAP,
    BARN_VILKÅR_BOSATT_I_RIKET,
    BARN_VILKÅR_LOVLIG_OPPHOLD,
    BARN_VILKÅR_UTVIDET_BARNETRYGD,
    BEHANDLINGSRESULTAT_IKKE_INNVILGET,
    ENDREDE_ANDELER_FOR_BARN_UTEN_FREMSTILT_KRAV,
    OVERLAPP_TIDLIGERE_UTBETALING,
    OVERLAPP_INFOTRYGD,
    INGEN_UTBETALING,
    FEILUTBETALING,
    MANGLER_PERIODE_MED_NY_INNVILGELSE,
    UTBETALING_OVER_100_PROSENT_BARN,
    UTBETALING_OVER_100_PROSENT_SØKER,
    ;

    companion object {
        fun fraVilkår(
            vilkår: Vilkår,
            erSøker: Boolean,
        ): AutovedtakSøknadMåBehandlesManueltÅrsak =
            if (erSøker) {
                when (vilkår) {
                    GIFT_PARTNERSKAP -> SØKER_VILKÅR_GIFT_PARTNERSKAP
                    BOSATT_I_RIKET -> SØKER_VILKÅR_BOSATT_I_RIKET
                    LOVLIG_OPPHOLD -> SØKER_VILKÅR_LOVLIG_OPPHOLD
                    UTVIDET_BARNETRYGD -> SØKER_VILKÅR_UTVIDET_BARNETRYGD
                    UNDER_18_ÅR, BOR_MED_SØKER -> throw Feil("Ugyldig vilkårstype $vilkår for søker ved automatisk behandling av søknad")
                }
            } else {
                when (vilkår) {
                    UNDER_18_ÅR -> BARN_VILKÅR_UNDER_18_ÅR
                    BOR_MED_SØKER -> BARN_VILKÅR_BOR_MED_SØKER
                    GIFT_PARTNERSKAP -> BARN_VILKÅR_GIFT_PARTNERSKAP
                    BOSATT_I_RIKET -> BARN_VILKÅR_BOSATT_I_RIKET
                    LOVLIG_OPPHOLD -> BARN_VILKÅR_LOVLIG_OPPHOLD
                    UTVIDET_BARNETRYGD -> BARN_VILKÅR_UTVIDET_BARNETRYGD
                }
            }
    }
}
