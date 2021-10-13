package no.nav.familie.ba.sak.kjerne.fødselshendelse

import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityVilkår
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.vedtakBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class VilkårsvurderingUtilsTest {
    @Test
    fun `skal liste opp begrunnelser uten vilkår`() {
        val sanityBegrunnelser =
            listOf(SanityBegrunnelse(vilkaar = null, apiNavn = "innvilgetBosattIRiket", navnISystem = ""))
        val vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET

        val restVedtakBegrunnelserTilknyttetVilkår =
            vedtakBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(sanityBegrunnelser, vedtakBegrunnelse)

        Assertions.assertEquals(1, restVedtakBegrunnelserTilknyttetVilkår.size)
    }

    @Test
    fun `skal liste opp begrunnelsene en gang per vilkår`() {
        val sanityBegrunnelser =
            listOf(
                SanityBegrunnelse(
                    vilkaar = listOf(SanityVilkår.BOSATT_I_RIKET, SanityVilkår.LOVLIG_OPPHOLD),
                    apiNavn = "innvilgetBosattIRiket",
                    navnISystem = ""
                )
            )
        val vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET

        val restVedtakBegrunnelserTilknyttetVilkår =
            vedtakBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(sanityBegrunnelser, vedtakBegrunnelse)

        Assertions.assertEquals(2, restVedtakBegrunnelserTilknyttetVilkår.size)
    }
}
