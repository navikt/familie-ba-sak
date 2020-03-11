package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.familie.ba.sak.beregning.domene.SatsType

class YtelseSatsMapper {

    companion object {
        fun map(ytelsetype: Ytelsetype) : SatsType?
        {
            return when(ytelsetype) {
                Ytelsetype.ORDINÆR_BARNETRYGD -> SatsType.ORBA
                Ytelsetype.UTVIDET_BARNETRYGD -> SatsType.ORBA
                Ytelsetype.SMÅBARNSTILLEGG -> SatsType.SMA
                Ytelsetype.EØS -> null
                Ytelsetype.MANUELL_VURDERING -> null
            }
        }
    }
}