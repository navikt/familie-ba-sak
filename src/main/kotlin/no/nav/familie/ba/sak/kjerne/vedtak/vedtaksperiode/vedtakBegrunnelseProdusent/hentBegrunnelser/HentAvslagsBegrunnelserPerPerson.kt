package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.hentBegrunnelser

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.BehandlingsGrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvedeVilkårTidslinjer
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull

internal fun VedtaksperiodeMedBegrunnelser.hentAvslagsbegrunnelserPerPerson(
    behandlingsGrunnlagForVedtaksperioder: BehandlingsGrunnlagForVedtaksperioder,
): Map<Person, Set<IVedtakBegrunnelse>> =
    behandlingsGrunnlagForVedtaksperioder.persongrunnlag.personer.associateWith { person ->
        val vilkårResultaterForPerson =
            behandlingsGrunnlagForVedtaksperioder
                .personResultater
                .firstOrNull { it.aktør == person.aktør }
                ?.vilkårResultater ?: emptyList()

        val (generelleAvslag, vilkårResultaterUtenGenerelleAvslag) = vilkårResultaterForPerson.partition { it.erEksplisittAvslagUtenPeriode() }

        val generelleAvslagsbegrunnelser = generelleAvslag.flatMap { it.standardbegrunnelser }

        val avslagsbegrunnelserMedPeriodeTidslinjer =
            vilkårResultaterUtenGenerelleAvslag
                .tilForskjøvedeVilkårTidslinjer(person.fødselsdato)
                .filtrerKunEksplisittAvslagsPerioder()

        val avslagsbegrunnelserMedPeriode = avslagsbegrunnelserMedPeriodeTidslinjer.flatMap { it.tilPerioder() }.filter { it.fom?.toYearMonth() == this.fom?.toYearMonth() }.flatMap { it.verdi?.standardbegrunnelser ?: emptyList() }

        (generelleAvslagsbegrunnelser + avslagsbegrunnelserMedPeriode).toSet()
    }

private fun List<Tidslinje<VilkårResultat>>.filtrerKunEksplisittAvslagsPerioder(): List<Tidslinje<VilkårResultat>> =
    this.map { tidslinjeForVilkår ->
        tidslinjeForVilkår
            .tilPerioderIkkeNull()
            .filter { it.verdi.erEksplisittAvslagPåSøknad == true }
            .tilTidslinje()
    }
