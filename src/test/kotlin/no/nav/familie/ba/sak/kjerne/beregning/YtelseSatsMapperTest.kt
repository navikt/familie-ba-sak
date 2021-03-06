package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class YtelseSatsMapperTest {

    @Test
    fun `Test at barn under 6 år får tillegg`(){

        assertEquals(SatsType.TILLEGG_ORBA, YtelseSatsMapper.map(YtelseType.ORDINÆR_BARNETRYGD, 5))
        assertEquals(SatsType.TILLEGG_ORBA, YtelseSatsMapper.map(YtelseType.ORDINÆR_BARNETRYGD, 0))
    }

    @Test
    fun `Test at barn på 6 år og oppover IKKE får tillegg`(){

        assertEquals(SatsType.ORBA, YtelseSatsMapper.map(YtelseType.ORDINÆR_BARNETRYGD, 6))
        assertEquals(SatsType.ORBA, YtelseSatsMapper.map(YtelseType.ORDINÆR_BARNETRYGD, 99))
    }

    @Test
    fun `Test standardmapping`(){

        assertEquals(SatsType.ORBA, YtelseSatsMapper.map(YtelseType.ORDINÆR_BARNETRYGD))
        assertEquals(SatsType.SMA, YtelseSatsMapper.map(YtelseType.SMÅBARNSTILLEGG))
        assertEquals(SatsType.ORBA, YtelseSatsMapper.map(YtelseType.UTVIDET_BARNETRYGD))
        assertNull(YtelseSatsMapper.map(YtelseType.EØS))
        assertNull(YtelseSatsMapper.map(YtelseType.MANUELL_VURDERING))
    }

}