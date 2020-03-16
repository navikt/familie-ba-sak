package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.familie.ba.sak.beregning.domene.SatsType

object YtelseSatsMapper {

    const val MAX_ALDER_TILLEGG_ORDINÆR_BARNETRYGD = 5

    fun map(ytelsetype: Ytelsetype, barnetsAlder: Int?=null): SatsType? {

        if((barnetsAlder ?: Int.MAX_VALUE) <= MAX_ALDER_TILLEGG_ORDINÆR_BARNETRYGD && ytelsetype == Ytelsetype.ORDINÆR_BARNETRYGD) {
            return SatsType.TILLEGG_ORBA
        } else {
            return map(ytelsetype)
        }
    }

    private fun map(ytelsetype: Ytelsetype): SatsType? {
        return when (ytelsetype) {
            Ytelsetype.ORDINÆR_BARNETRYGD -> SatsType.ORBA
            Ytelsetype.UTVIDET_BARNETRYGD -> SatsType.ORBA
            Ytelsetype.SMÅBARNSTILLEGG -> SatsType.SMA
            Ytelsetype.EØS -> null
            Ytelsetype.MANUELL_VURDERING -> null
        }
    }
}