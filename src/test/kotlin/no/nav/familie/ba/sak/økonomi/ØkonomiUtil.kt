package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.familie.ba.sak.common.dato
import no.nav.fpsak.tidsserie.LocalDateSegment

fun sats(ytelsetype: Ytelsetype) =
        when (ytelsetype) {
            Ytelsetype.ORDINÆR_BARNETRYGD -> 1054
            Ytelsetype.UTVIDET_BARNETRYGD -> 1054
            Ytelsetype.SMÅBARNSTILLEGG -> 660
            Ytelsetype.MANUELL_VURDERING->0
            Ytelsetype.EØS->0
        }

fun lagSegmentBeløp(fom: String, tom: String, beløp: Int): LocalDateSegment<Int> =
        LocalDateSegment(dato(fom), dato(tom), beløp)




