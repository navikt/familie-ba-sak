package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.vedtak.YtelseType
import no.nav.familie.ba.sak.common.dato
import no.nav.fpsak.tidsserie.LocalDateSegment

fun sats(ytelseType: YtelseType) =
        when (ytelseType) {
            YtelseType.ORDINÆR_BARNETRYGD -> 1054
            YtelseType.UTVIDET_BARNETRYGD -> 1054
            YtelseType.SMÅBARNSTILLEGG -> 660
            YtelseType.MANUELL_VURDERING->0
            YtelseType.EØS->0
        }

fun lagSegmentBeløp(fom: String, tom: String, beløp: Int): LocalDateSegment<Int> =
        LocalDateSegment(dato(fom), dato(tom), beløp)




