package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class VedtakBegrunnelseSpesifikasjonTest {

    val behandling = lagBehandling()
    val søker = tilfeldigPerson(personType = PersonType.SØKER)
    val barn = tilfeldigPerson(personType = PersonType.BARN)
    val vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.UTBETALING,
    )
    val vilkårsvurdering = lagVilkårsvurdering(søker.personIdent.ident, lagBehandling(), Resultat.OPPFYLT)
    val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn)

    val identerMedUtbetaling = listOf(søker.personIdent.ident, barn.personIdent.ident)

    @Test
    fun `Oppfyller vilkår skal gi true`() {
        assertTrue(VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET
                           .triggesForPeriode(vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                                              vilkårsvurdering = vilkårsvurdering,
                                              persongrunnlag = personopplysningGrunnlag,
                                              identerMedUtbetaling = identerMedUtbetaling))
    }

    @Test
    fun `Ikke valgbar skal gi false`() {
        assertFalse(VedtakBegrunnelseSpesifikasjon.REDUKSJON_FRITEKST
                            .triggesForPeriode(vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                                               vilkårsvurdering = vilkårsvurdering,
                                               persongrunnlag = personopplysningGrunnlag,
                                               identerMedUtbetaling = identerMedUtbetaling))
    }

    @Test
    fun `Annen periode type skal gi false`() {
        assertFalse(VedtakBegrunnelseSpesifikasjon.OPPHØR_UTVANDRET
                            .triggesForPeriode(vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                                               vilkårsvurdering = vilkårsvurdering,
                                               persongrunnlag = personopplysningGrunnlag,
                                               identerMedUtbetaling = identerMedUtbetaling))
    }

    @Test
    fun `Har ikke barn med seksårsdag skal gi false`() {
        //val persongrunnlag = mockk<PersonopplysningGrunnlag>()
        //every { persongrunnlag.harBarnMedSeksårsdagPåFom(vedtaksperiodeMedBegrunnelser.fom) } returns true
        assertFalse(VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR
                            .triggesForPeriode(vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                                               vilkårsvurdering = vilkårsvurdering,
                                               persongrunnlag = personopplysningGrunnlag,
                                               identerMedUtbetaling = identerMedUtbetaling))
    }

    @Test
    fun `Har barn med seksårsdag skal gi true`() {
        val persongrunnlag = mockk<PersonopplysningGrunnlag>()
        every { persongrunnlag.harBarnMedSeksårsdagPåFom(vedtaksperiodeMedBegrunnelser.fom) } returns true

        assertTrue(VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR
                           .triggesForPeriode(vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                                              vilkårsvurdering = vilkårsvurdering,
                                              persongrunnlag = persongrunnlag,
                                              identerMedUtbetaling = identerMedUtbetaling))
    }

    @Test
    fun `Har sats endring skal gi true`() {
        val vedtaksperiodeMedBegrunnelserSatsEndring = lagVedtaksperiodeMedBegrunnelser(
                fom = LocalDate.of(2021, 9, 1),
                type = Vedtaksperiodetype.UTBETALING,
        )

        assertTrue(VedtakBegrunnelseSpesifikasjon.INNVILGET_SATSENDRING
                           .triggesForPeriode(vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelserSatsEndring,
                                              vilkårsvurdering = vilkårsvurdering,
                                              persongrunnlag = personopplysningGrunnlag,
                                              identerMedUtbetaling = identerMedUtbetaling))
    }

    @Test
    fun `Har ikke sats endring skal gi false`() {

        assertFalse(VedtakBegrunnelseSpesifikasjon.INNVILGET_SATSENDRING
                            .triggesForPeriode(vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                                               vilkårsvurdering = vilkårsvurdering,
                                               persongrunnlag = personopplysningGrunnlag,
                                               identerMedUtbetaling = identerMedUtbetaling))
    }

    @Test
    fun `Oppfyller ikke vilkår for person skal gi false`() {
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barn)

        assertFalse(VedtakBegrunnelseSpesifikasjon.INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER
                            .triggesForPeriode(vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                                               vilkårsvurdering = vilkårsvurdering,
                                               persongrunnlag = personopplysningGrunnlag,
                                               identerMedUtbetaling = identerMedUtbetaling))
    }

    @Test
    fun `Oppfyller vilkår for person skal gi true`() {
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søker)

        assertTrue(VedtakBegrunnelseSpesifikasjon.INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER
                           .triggesForPeriode(vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                                              vilkårsvurdering = vilkårsvurdering,
                                              persongrunnlag = personopplysningGrunnlag,
                                              identerMedUtbetaling = identerMedUtbetaling))
    }
}