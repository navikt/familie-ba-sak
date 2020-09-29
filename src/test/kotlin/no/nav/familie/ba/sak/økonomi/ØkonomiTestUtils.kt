package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.beregning.domene.YtelseType

fun sats(ytelseType: YtelseType) =
        when (ytelseType) {
            YtelseType.ORDINÆR_BARNETRYGD_UNDER_6_ÅR -> 1354
            YtelseType.ORDINÆR_BARNETRYGD -> 1054
            YtelseType.UTVIDET_BARNETRYGD -> 1054
            YtelseType.SMÅBARNSTILLEGG -> 660
            YtelseType.MANUELL_VURDERING -> 0
            YtelseType.FINNMARKSTILLEGG -> 1054
            YtelseType.EØS -> 0
        }
