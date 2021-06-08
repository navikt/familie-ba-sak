package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.vedtak.domene.VedtaksbegrunnelseFritekst
import no.nav.familie.ba.sak.behandling.vedtak.domene.byggBegrunnelserOgFriteksterForVedtaksperiode
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.common.lagVedtaksbegrunnelse
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class VedtaksperiodeMedBegrunnelseTekst {

    val søker = tilfeldigSøker()
    val barn1 = tilfeldigPerson(personType = PersonType.BARN)
    val barn2 = tilfeldigPerson(personType = PersonType.BARN)
    val personerIPersongrunnlag = listOf(barn1, barn2, søker)
    val vedtaksperiode = lagVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.FORTSATT_INNVILGET,
            begrunnelser = mutableSetOf(
                    lagVedtaksbegrunnelse(
                            personIdenter = listOf(
                                    barn1.personIdent.ident,
                            ),
                            vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_SØKER_OG_BARN_BOSATT_I_RIKET,
                    ),
                    lagVedtaksbegrunnelse(
                            personIdenter = listOf(
                                    barn2.personIdent.ident,
                            ),
                            vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_BOR_MED_SØKER,
                    ),
            ),
            fritekster = mutableSetOf(VedtaksbegrunnelseFritekst(
                    id = 1,
                    fritekst = "Fritekst1",
                    vedtaksperiodeMedBegrunnelser = mockk(),
            ), VedtaksbegrunnelseFritekst(
                    id = 2,
                    fritekst = "Fritekst2",
                    vedtaksperiodeMedBegrunnelser = mockk(),
            )),
    )
    val begrunnelserOgFritekster = byggBegrunnelserOgFriteksterForVedtaksperiode(
            vedtaksperiode = vedtaksperiode,
            søker = søker,
            personerIPersongrunnlag,
    )

    @Test
    fun `Skal gi riktig antall brevbegrunnelser med riktig tekst`() {
        Assertions.assertEquals(3, begrunnelserOgFritekster.size)
    }

    @Test
    fun `Skal med riktig rekkefølge og tekst på fritekstene`() {
        Assertions.assertEquals("Fritekst1", begrunnelserOgFritekster[2])
        Assertions.assertEquals("Fritekst2", begrunnelserOgFritekster[3])
    }
}