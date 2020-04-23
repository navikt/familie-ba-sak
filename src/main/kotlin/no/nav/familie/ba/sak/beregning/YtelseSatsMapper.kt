package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.vedtak.YtelseType
import no.nav.familie.ba.sak.beregning.domene.SatsType

object YtelseSatsMapper {

    const val MAX_ALDER_TILLEGG_ORDINÆR_BARNETRYGD = 5

    fun map(ytelseType: YtelseType, barnetsAlder: Int?=null): SatsType? {

        if((barnetsAlder ?: Int.MAX_VALUE) <= MAX_ALDER_TILLEGG_ORDINÆR_BARNETRYGD && ytelseType == YtelseType.ORDINÆR_BARNETRYGD) {
            return SatsType.TILLEGG_ORBA
        } else {
            return map(ytelseType)
        }
    }

    private fun map(ytelseType: YtelseType): SatsType? {
        return when (ytelseType) {
            YtelseType.ORDINÆR_BARNETRYGD -> SatsType.ORBA
            YtelseType.UTVIDET_BARNETRYGD -> SatsType.ORBA
            YtelseType.SMÅBARNSTILLEGG -> SatsType.SMA
            YtelseType.EØS -> null
            YtelseType.MANUELL_VURDERING -> null
        }
    }
}