package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagUtbetalingsperiode
import no.nav.familie.ba.sak.common.lagUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.common.lagVedtaksbegrunnelse
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.AvslagBrevPeriode
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.FortsattInnvilgetBrevPeriode
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.InnvilgelseBrevPeriode
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.OpphørBrevPeriode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksbegrunnelseFritekst
import no.nav.familie.ba.sak.kjerne.vedtak.domene.byggBegrunnelserOgFriteksterForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilBrevPeriode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class VedtaksperiodeMedBegrunnelseTest {

    val søker = tilfeldigSøker()
    val barn1 = tilfeldigPerson(personType = PersonType.BARN)
    val barn2 = tilfeldigPerson(personType = PersonType.BARN)
    val personerIPersongrunnlag = listOf(barn1, barn2, søker)

    @Test
    fun `Skal gi riktig antall brevbegrunnelser med riktig tekst`() {
        val vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(
                type = Vedtaksperiodetype.FORTSATT_INNVILGET,
        )
        val vedtaksperiode = vedtaksperiodeMedBegrunnelser.copy(
                begrunnelser = mutableSetOf(
                        lagVedtaksbegrunnelse(
                                vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                                personIdenter = listOf(barn1.personIdent.ident),
                                vedtakBegrunnelseSpesifikasjon =
                                VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_SØKER_OG_BARN_BOSATT_I_RIKET,
                        ),
                        lagVedtaksbegrunnelse(
                                vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                                personIdenter = listOf(barn2.personIdent.ident),
                                vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_BOR_MED_SØKER,
                        ),
                ),
                fritekster = mutableSetOf(),
        )
        val begrunnelserOgFritekster = byggBegrunnelserOgFriteksterForVedtaksperiode(
                vedtaksperiode = vedtaksperiode,
                personerIPersongrunnlag = personerIPersongrunnlag,
                målform = Målform.NB,
                brukBegrunnelserFraSanity = false,
        )

        Assertions.assertEquals(2, begrunnelserOgFritekster.size)
    }

    @Test
    fun `Skal med riktig rekkefølge og tekst på fritekstene`() {
        val vedtaksperiode = lagVedtaksperiodeMedBegrunnelser(
                type = Vedtaksperiodetype.FORTSATT_INNVILGET,
                begrunnelser = mutableSetOf(
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
                personerIPersongrunnlag = personerIPersongrunnlag,
                målform = Målform.NB,
                brukBegrunnelserFraSanity = false,
        )

        Assertions.assertEquals("Fritekst1", begrunnelserOgFritekster[0])
        Assertions.assertEquals("Fritekst2", begrunnelserOgFritekster[1])
    }

    @Test
    fun `Skal gi fortsatt innvilget-brevperiode`() {
        val vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(
                type = Vedtaksperiodetype.FORTSATT_INNVILGET,
        )
        val fortsatInnvilgetPeriode = vedtaksperiodeMedBegrunnelser.copy(
                begrunnelser = mutableSetOf(
                        lagVedtaksbegrunnelse(
                                vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                                personIdenter = listOf(barn1.personIdent.ident),
                                vedtakBegrunnelseSpesifikasjon =
                                VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_SØKER_OG_BARN_BOSATT_I_RIKET,
                        ),
                ),
        )

        Assertions.assertTrue(
                fortsatInnvilgetPeriode.tilBrevPeriode(
                        personerIPersongrunnlag = personerIPersongrunnlag,
                        utbetalingsperioder = listOf(lagUtbetalingsperiode(
                                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
                        )),
                        målform = Målform.NB,
                        brukBegrunnelserFraSanity = false,
                ) is FortsattInnvilgetBrevPeriode
        )
    }

    @Test
    fun `Skal gi innvilget-brevperiode`() {
        val vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(
                type = Vedtaksperiodetype.UTBETALING
        )
        val utbetalingsperiode = vedtaksperiodeMedBegrunnelser.copy(
                begrunnelser = mutableSetOf(
                        lagVedtaksbegrunnelse(
                                vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                                personIdenter = listOf(barn1.personIdent.ident),
                                vedtakBegrunnelseSpesifikasjon =
                                VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET,
                        ),
                ),
        )

        Assertions.assertTrue(
                utbetalingsperiode.tilBrevPeriode(
                        personerIPersongrunnlag = personerIPersongrunnlag,
                        utbetalingsperioder = listOf(lagUtbetalingsperiode(
                                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
                        )),
                        målform = Målform.NB,
                        brukBegrunnelserFraSanity = false,
                ) is InnvilgelseBrevPeriode
        )
    }

    @Test
    fun `Skal gi avslagbrevperiode`() {
        val vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(
                type = Vedtaksperiodetype.AVSLAG,
        )
        val avslagsperiode = vedtaksperiodeMedBegrunnelser.copy(
                begrunnelser = mutableSetOf(
                        lagVedtaksbegrunnelse(
                                vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                                personIdenter = listOf(barn1.personIdent.ident),
                                vedtakBegrunnelseSpesifikasjon =
                                VedtakBegrunnelseSpesifikasjon.AVSLAG_BOR_HOS_SØKER,
                        ),
                ),
        )

        Assertions.assertTrue(
                avslagsperiode.tilBrevPeriode(
                        personerIPersongrunnlag = personerIPersongrunnlag,
                        utbetalingsperioder = listOf(),
                        målform = Målform.NB,
                        brukBegrunnelserFraSanity = false,
                ) is AvslagBrevPeriode
        )
    }

    @Test
    fun `Skal gi opphørbrevperiode`() {
        val vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(
                type = Vedtaksperiodetype.OPPHØR,
        )
        val opphørsperiode = vedtaksperiodeMedBegrunnelser.copy(
                begrunnelser = mutableSetOf(
                        lagVedtaksbegrunnelse(
                                vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                                personIdenter = listOf(barn1.personIdent.ident),
                                vedtakBegrunnelseSpesifikasjon =
                                VedtakBegrunnelseSpesifikasjon.OPPHØR_BARN_FLYTTET_FRA_SØKER,
                        ),
                ),
        )

        Assertions.assertTrue(
                opphørsperiode.tilBrevPeriode(
                        personerIPersongrunnlag = personerIPersongrunnlag,
                        utbetalingsperioder = listOf(),
                        målform = Målform.NB,
                        brukBegrunnelserFraSanity = false,
                ) is OpphørBrevPeriode
        )
    }

    @Test
    fun `Skal gi null ved ingen begrunnelser eller fritekster`() {
        val opphørsperiode = lagVedtaksperiodeMedBegrunnelser(
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf(),
                fritekster = mutableSetOf(),
        )

        Assertions.assertTrue(
                opphørsperiode.tilBrevPeriode(
                        personerIPersongrunnlag = personerIPersongrunnlag,
                        utbetalingsperioder = listOf(),
                        målform = Målform.NB,
                        brukBegrunnelserFraSanity = false,
                ) == null
        )
    }

}