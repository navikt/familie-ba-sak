package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagRestVedtaksbegrunnelse
import no.nav.familie.ba.sak.common.lagUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.common.lagUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.AvslagBrevPeriode
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.FortsattInnvilgetBrevPeriode
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.InnvilgelseBrevPeriode
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.OpphørBrevPeriode
import no.nav.familie.ba.sak.kjerne.dokument.tilBrevPeriode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.domene.FritekstBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksbegrunnelseFritekst
import no.nav.familie.ba.sak.kjerne.vedtak.domene.byggBegrunnelserOgFriteksterForVedtaksperiode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class VedtaksperiodeMedBegrunnelseTest {

    val søker = tilfeldigSøker()
    val barn1 = tilfeldigPerson(personType = PersonType.BARN)
    val barn2 = tilfeldigPerson(personType = PersonType.BARN)
    val personerIPersongrunnlag = listOf(barn1, barn2, søker)

    @Test
    fun `Skal gi riktig antall brevbegrunnelser med riktig tekst`() {
        val utvidetVedtaksperiodeMedBegrunnelser = lagUtvidetVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.FORTSATT_INNVILGET,
            begrunnelser = listOf(
                lagRestVedtaksbegrunnelse(
                    personIdenter = listOf(barn1.personIdent.ident),
                    vedtakBegrunnelseSpesifikasjon =
                    VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_SØKER_OG_BARN_BOSATT_I_RIKET,
                ),
                lagRestVedtaksbegrunnelse(
                    personIdenter = listOf(barn2.personIdent.ident),
                    vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_BOR_MED_SØKER,
                ),
            ),
            fritekster = mutableListOf(),
            utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
        )

        val begrunnelserOgFritekster = byggBegrunnelserOgFriteksterForVedtaksperiode(
            utvidetVedtaksperiodeMedBegrunnelser = utvidetVedtaksperiodeMedBegrunnelser,
            personerIPersongrunnlag = personerIPersongrunnlag,
            målform = Målform.NB,
        )

        Assertions.assertEquals(2, begrunnelserOgFritekster.size)
    }

    @Test
    fun `Skal med riktig rekkefølge og tekst på fritekstene`() {
        val utvidetVedtaksperiodeMedBegrunnelser = lagUtvidetVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.FORTSATT_INNVILGET,
            begrunnelser = emptyList(),
            fritekster = mutableListOf(
                VedtaksbegrunnelseFritekst(
                    id = 1,
                    fritekst = "Fritekst1",
                    vedtaksperiodeMedBegrunnelser = mockk(),
                ),
                VedtaksbegrunnelseFritekst(
                    id = 2,
                    fritekst = "Fritekst2",
                    vedtaksperiodeMedBegrunnelser = mockk(),
                )
            ),
            utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
        )
        val begrunnelserOgFritekster = byggBegrunnelserOgFriteksterForVedtaksperiode(
            utvidetVedtaksperiodeMedBegrunnelser = utvidetVedtaksperiodeMedBegrunnelser,
            personerIPersongrunnlag = personerIPersongrunnlag,
            målform = Målform.NB,
        )

        Assertions.assertEquals("Fritekst1", (begrunnelserOgFritekster[0] as FritekstBegrunnelse).fritekst)
        Assertions.assertEquals("Fritekst2", (begrunnelserOgFritekster[1] as FritekstBegrunnelse).fritekst)
    }

    @Test
    fun `Skal gi fortsatt innvilget-brevperiode`() {
        val utvidetVedtaksperiodeMedBegrunnelser = lagUtvidetVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.FORTSATT_INNVILGET,
            begrunnelser = listOf(
                lagRestVedtaksbegrunnelse(
                    personIdenter = listOf(barn1.personIdent.ident),
                    vedtakBegrunnelseSpesifikasjon =
                    VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_SØKER_OG_BARN_BOSATT_I_RIKET,
                ),
            ),
            utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
        )

        Assertions.assertTrue(
            utvidetVedtaksperiodeMedBegrunnelser.tilBrevPeriode(
                personerIPersongrunnlag = personerIPersongrunnlag,
                målform = Målform.NB,
            ) is FortsattInnvilgetBrevPeriode
        )
    }

    @Test
    fun `Skal gi innvilget-brevperiode`() {
        val utvidetVedtaksperiodeMedBegrunnelser = lagUtvidetVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.UTBETALING,
            begrunnelser = listOf(
                lagRestVedtaksbegrunnelse(
                    personIdenter = listOf(barn1.personIdent.ident),
                    vedtakBegrunnelseSpesifikasjon =
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET,
                ),
            ),
            utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
        )

        Assertions.assertTrue(
            utvidetVedtaksperiodeMedBegrunnelser.tilBrevPeriode(
                personerIPersongrunnlag = personerIPersongrunnlag,
                målform = Målform.NB,
            ) is InnvilgelseBrevPeriode
        )
    }

    @Test
    fun `Skal gi avslagbrevperiode`() {
        val utvidetVedtaksperiodeMedBegrunnelser = lagUtvidetVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.AVSLAG,
            begrunnelser = listOf(
                lagRestVedtaksbegrunnelse(
                    personIdenter = listOf(barn1.personIdent.ident),
                    vedtakBegrunnelseSpesifikasjon =
                    VedtakBegrunnelseSpesifikasjon.AVSLAG_BOR_HOS_SØKER,
                ),
            ),
        )

        Assertions.assertTrue(
            utvidetVedtaksperiodeMedBegrunnelser.tilBrevPeriode(
                personerIPersongrunnlag = personerIPersongrunnlag,
                målform = Målform.NB,
            ) is AvslagBrevPeriode
        )
    }

    @Test
    fun `Skal gi opphørbrevperiode`() {
        val utvidetVedtaksperiodeMedBegrunnelser = lagUtvidetVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.OPPHØR,
            begrunnelser = listOf(
                lagRestVedtaksbegrunnelse(
                    personIdenter = listOf(barn1.personIdent.ident),
                    vedtakBegrunnelseSpesifikasjon =
                    VedtakBegrunnelseSpesifikasjon.OPPHØR_BARN_FLYTTET_FRA_SØKER,
                ),
            ),
        )

        Assertions.assertTrue(
            utvidetVedtaksperiodeMedBegrunnelser.tilBrevPeriode(
                personerIPersongrunnlag = personerIPersongrunnlag,
                målform = Målform.NB,
            ) is OpphørBrevPeriode
        )
    }

    @Test
    fun `Skal gi null ved ingen begrunnelser eller fritekster`() {
        val utvidetVedtaksperiodeMedBegrunnelser = lagUtvidetVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.UTBETALING,
            begrunnelser = emptyList(),
            fritekster = mutableListOf(),
            utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
        )

        Assertions.assertTrue(
            utvidetVedtaksperiodeMedBegrunnelser.tilBrevPeriode(
                personerIPersongrunnlag = personerIPersongrunnlag,
                målform = Målform.NB,
            ) == null
        )
    }
}
