package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

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
        assertTrue(
            VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET
                .triggesForPeriode(
                    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                    vilkårsvurdering = vilkårsvurdering,
                    persongrunnlag = personopplysningGrunnlag,
                    identerMedUtbetaling = identerMedUtbetaling,
                    triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET))
                )
        )
    }

    @Test
    fun `Annen periode type skal gi false`() {
        assertFalse(
            VedtakBegrunnelseSpesifikasjon.OPPHØR_UTVANDRET
                .triggesForPeriode(
                    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                    vilkårsvurdering = vilkårsvurdering,
                    persongrunnlag = personopplysningGrunnlag,
                    identerMedUtbetaling = identerMedUtbetaling,
                    triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET))
                )
        )
    }

    @Test
    fun `Har ikke barn med seksårsdag skal gi false`() {
        // val persongrunnlag = mockk<PersonopplysningGrunnlag>()
        // every { persongrunnlag.harBarnMedSeksårsdagPåFom(vedtaksperiodeMedBegrunnelser.fom) } returns true
        assertFalse(
            VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR
                .triggesForPeriode(
                    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                    vilkårsvurdering = vilkårsvurdering,
                    persongrunnlag = personopplysningGrunnlag,
                    identerMedUtbetaling = identerMedUtbetaling,
                    triggesAv = TriggesAv(barnMedSeksårsdag = true)
                )
        )
    }

    @Test
    fun `Har barn med seksårsdag skal gi true`() {
        val persongrunnlag = mockk<PersonopplysningGrunnlag>()
        every { persongrunnlag.harBarnMedSeksårsdagPåFom(vedtaksperiodeMedBegrunnelser.fom) } returns true

        assertTrue(
            VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR
                .triggesForPeriode(
                    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                    vilkårsvurdering = vilkårsvurdering,
                    persongrunnlag = persongrunnlag,
                    identerMedUtbetaling = identerMedUtbetaling,
                    triggesAv = TriggesAv(barnMedSeksårsdag = true)
                )
        )
    }

    @Test
    fun `Har sats endring skal gi true`() {
        val vedtaksperiodeMedBegrunnelserSatsEndring = lagVedtaksperiodeMedBegrunnelser(
            fom = LocalDate.of(2021, 9, 1),
            type = Vedtaksperiodetype.UTBETALING,
        )

        assertTrue(
            VedtakBegrunnelseSpesifikasjon.INNVILGET_SATSENDRING
                .triggesForPeriode(
                    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelserSatsEndring,
                    vilkårsvurdering = vilkårsvurdering,
                    persongrunnlag = personopplysningGrunnlag,
                    identerMedUtbetaling = identerMedUtbetaling,
                    triggesAv = TriggesAv(satsendring = true)
                )
        )
    }

    @Test
    fun `Har ikke sats endring skal gi false`() {
        val vedtaksperiodeMedBegrunnelserSatsEndring = lagVedtaksperiodeMedBegrunnelser(
            fom = LocalDate.of(2021, 8, 1),
            type = Vedtaksperiodetype.UTBETALING,
        )

        assertFalse(
            VedtakBegrunnelseSpesifikasjon.INNVILGET_SATSENDRING
                .triggesForPeriode(
                    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelserSatsEndring,
                    vilkårsvurdering = vilkårsvurdering,
                    persongrunnlag = personopplysningGrunnlag,
                    identerMedUtbetaling = identerMedUtbetaling,
                    triggesAv = TriggesAv(satsendring = true)
                )
        )
    }

    @Test
    fun `Oppfyller ikke vilkår for person skal gi false`() {
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barn)

        assertFalse(
            VedtakBegrunnelseSpesifikasjon.INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER
                .triggesForPeriode(
                    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                    vilkårsvurdering = vilkårsvurdering,
                    persongrunnlag = personopplysningGrunnlag,
                    identerMedUtbetaling = identerMedUtbetaling,
                    triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD), personTyper = setOf(PersonType.SØKER))
                )
        )
    }

    @Test
    fun `Oppfyller vilkår for person skal gi true`() {
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søker)

        assertTrue(
            VedtakBegrunnelseSpesifikasjon.INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER
                .triggesForPeriode(
                    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                    vilkårsvurdering = vilkårsvurdering,
                    persongrunnlag = personopplysningGrunnlag,
                    identerMedUtbetaling = identerMedUtbetaling,
                    triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD), personTyper = setOf(PersonType.SØKER))
                )
        )
    }

    @Test
    fun `Oppfyller etter endringsperiode skal gi true`() {
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barn)

        assertTrue(
            VedtakBegrunnelseSpesifikasjon.INNVILGET_ETTER_ENDRET_UTBETALING_DELT_BOSTED
                .triggesForPeriode(
                    persongrunnlag = personopplysningGrunnlag,
                    vilkårsvurdering = vilkårsvurdering,
                    identerMedUtbetaling = identerMedUtbetaling,
                    vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(
                        type = Vedtaksperiodetype.UTBETALING,
                        fom = LocalDate.of(2021, 10, 1),
                        tom = LocalDate.of(2021, 10, 31)
                    ),
                    triggesAv = TriggesAv(etterEndretUtbetaling = true, endringsaarsaker = setOf(Årsak.DELT_BOSTED)),
                    endretUtbetalingAndeler = listOf(
                        EndretUtbetalingAndel(
                            behandlingId = behandling.id,
                            person = barn,
                            fom = YearMonth.of(2021, 6),
                            tom = YearMonth.of(2021, 9),
                            årsak = Årsak.DELT_BOSTED
                        )
                    )
                )
        )
    }

    @Test
    fun `Alle begrunnelser er unike`() {
        val vedtakBegrunnelser = VedtakBegrunnelseSpesifikasjon.values().groupBy { it.sanityApiNavn }
        Assertions.assertEquals(vedtakBegrunnelser.size, VedtakBegrunnelseSpesifikasjon.values().size)
    }
}
