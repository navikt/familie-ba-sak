package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType

fun sats(ytelseType: YtelseType) =
        when (ytelseType) {
            YtelseType.ORDINÆR_BARNETRYGD -> 1054
            YtelseType.UTVIDET_BARNETRYGD -> 1054
            YtelseType.SMÅBARNSTILLEGG -> 660
            YtelseType.MANUELL_VURDERING->0
            YtelseType.EØS->0
        }
