package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent

import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.VilkårResultatForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering

internal fun hentResultaterForPeriode(
    begrunnelseGrunnlagForPeriode: BegrunnelseGrunnlagForPersonIPeriode,
    begrunnelseGrunnlagForrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
    erUtbetalingPåSøkerIPeriode: Boolean,
    erReduksjonIFinnmarkstillegg: Boolean,
): List<SanityPeriodeResultat> {
    val erAndelerPåPersonHvisBarn =
        begrunnelseGrunnlagForPeriode.person.type != PersonType.BARN ||
            begrunnelseGrunnlagForPeriode.andeler
                .toList()
                .isNotEmpty()

    val erInnvilgetEtterVilkårOgEndretUtbetaling =
        begrunnelseGrunnlagForPeriode.erOrdinæreVilkårInnvilget() && begrunnelseGrunnlagForPeriode.erInnvilgetEtterEndretUtbetaling()

    val erReduksjonIFinnmarkPgaPerson =
        hentErReduksjonIFinnmarkPgaPerson(
            begrunnelseGrunnlagForPeriode.vilkårResultater,
            begrunnelseGrunnlagForrigePeriode?.vilkårResultater,
            erReduksjonIFinnmarkstillegg,
        )

    val erReduksjonIAndel =
        erReduksjonIAndelMellomPerioder(
            begrunnelseGrunnlagForPeriode,
            begrunnelseGrunnlagForrigePeriode,
        ) || erReduksjonIFinnmarkPgaPerson

    val erEøs = begrunnelseGrunnlagForPeriode.kompetanse != null

    return if (erInnvilgetEtterVilkårOgEndretUtbetaling && erAndelerPåPersonHvisBarn) {
        val erØkingIAndel =
            erØkningIAndelMellomPerioder(
                begrunnelseGrunnlagForPeriode,
                begrunnelseGrunnlagForrigePeriode,
            )
        val erSatsøkning =
            erSatsøkningMellomPerioder(
                begrunnelseGrunnlagForPeriode,
                begrunnelseGrunnlagForrigePeriode,
            )

        val erSøker = begrunnelseGrunnlagForPeriode.person.type == PersonType.SØKER
        val erOrdinæreVilkårOppfyltIForrigePeriode =
            begrunnelseGrunnlagForrigePeriode?.erOrdinæreVilkårInnvilget() == true

        val erIngenEndring = !erØkingIAndel && !erReduksjonIAndel && erOrdinæreVilkårOppfyltIForrigePeriode
        val erKunReduksjonAvSats =
            erKunReduksjonAvSats(begrunnelseGrunnlagForPeriode, begrunnelseGrunnlagForrigePeriode)

        listOfNotNull(
            if (erØkingIAndel || erSatsøkning || erSøker || erIngenEndring || erEøs || erKunReduksjonAvSats) SanityPeriodeResultat.INNVILGET_ELLER_ØKNING else null,
            if (erReduksjonIAndel) SanityPeriodeResultat.REDUKSJON else null,
            if (erIngenEndring || erKunReduksjonAvSats) SanityPeriodeResultat.INGEN_ENDRING else null,
        )
    } else {
        listOfNotNull(
            if (erUtbetalingPåSøkerIPeriode && erEøs) SanityPeriodeResultat.INNVILGET_ELLER_ØKNING else null,
            if (erReduksjonIAndel) SanityPeriodeResultat.REDUKSJON else null,
            SanityPeriodeResultat.IKKE_INNVILGET,
        )
    }
}

private fun hentErReduksjonIFinnmarkPgaPerson(
    vilkårResultaterDennePerioden: Iterable<VilkårResultatForVedtaksperiode>,
    vilkårResultaterForrigePeriode: Iterable<VilkårResultatForVedtaksperiode>?,
    erReduksjonIFinnmarkstillegg: Boolean,
): Boolean {
    val erBosattIFinnmarkForrigePeriode =
        vilkårResultaterForrigePeriode
            ?.flatMap { it.utdypendeVilkårsvurderinger }
            ?.any { it == UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS } ?: false
    val erBosattIFinnmarkDennePeriode =
        vilkårResultaterDennePerioden
            .flatMap { it.utdypendeVilkårsvurderinger }
            .any { it == UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS }

    return erBosattIFinnmarkForrigePeriode && !erBosattIFinnmarkDennePeriode && erReduksjonIFinnmarkstillegg
}

private fun erKunReduksjonAvSats(
    begrunnelseGrunnlagForPeriode: BegrunnelseGrunnlagForPersonIPeriode,
    begrunnelseGrunnlagForrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
): Boolean {
    val andelerForrigePeriode = begrunnelseGrunnlagForrigePeriode?.andeler ?: emptyList()
    val andelerDennePerioden = begrunnelseGrunnlagForPeriode.andeler

    return andelerForrigePeriode.any { andelIForrigePeriode ->
        val sammeAndelDennePerioden = andelerDennePerioden.singleOrNull { andelIForrigePeriode.type == it.type }

        val harAndelSammeProsent =
            sammeAndelDennePerioden != null && andelIForrigePeriode.prosent == sammeAndelDennePerioden.prosent
        val satsErRedusert = andelIForrigePeriode.sats > (sammeAndelDennePerioden?.sats ?: 0)

        harAndelSammeProsent && satsErRedusert
    }
}

private fun erReduksjonIAndelMellomPerioder(
    begrunnelseGrunnlagForPeriode: BegrunnelseGrunnlagForPersonIPeriode?,
    begrunnelseGrunnlagForrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
): Boolean {
    val andelerForrigePeriode = begrunnelseGrunnlagForrigePeriode?.andeler ?: emptyList()
    val andelerDennePerioden = begrunnelseGrunnlagForPeriode?.andeler ?: emptyList()

    return andelerForrigePeriode.any { andelIForrigePeriode ->
        val sammeAndelDennePerioden = andelerDennePerioden.singleOrNull { andelIForrigePeriode.type == it.type }

        val erAndelenMistet =
            sammeAndelDennePerioden == null && begrunnelseGrunnlagForrigePeriode?.erInnvilgetEtterEndretUtbetaling() == true
        val harAndelenGåttNedIProsent =
            sammeAndelDennePerioden != null && andelIForrigePeriode.prosent > sammeAndelDennePerioden.prosent
        val erSatsenRedusert = andelIForrigePeriode.sats > (sammeAndelDennePerioden?.sats ?: 0)

        erAndelenMistet || harAndelenGåttNedIProsent || erSatsenRedusert
    }
}

private fun erØkningIAndelMellomPerioder(
    begrunnelseGrunnlagForPeriode: BegrunnelseGrunnlagForPersonIPeriode,
    begrunnelseGrunnlagForrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
): Boolean {
    val andelerForrigePeriode = begrunnelseGrunnlagForrigePeriode?.andeler ?: emptyList()
    val andelerDennePerioden = begrunnelseGrunnlagForPeriode.andeler

    return andelerDennePerioden.any { andelIPeriode ->
        val sammeAndelForrigePeriode = andelerForrigePeriode.singleOrNull { andelIPeriode.type == it.type }

        val erAndelenTjent =
            sammeAndelForrigePeriode == null && begrunnelseGrunnlagForPeriode.erInnvilgetEtterEndretUtbetaling()
        val harAndelenGåttOppIProsent =
            sammeAndelForrigePeriode != null && andelIPeriode.prosent > sammeAndelForrigePeriode.prosent

        erAndelenTjent || harAndelenGåttOppIProsent
    }
}

private fun erSatsøkningMellomPerioder(
    begrunnelseGrunnlagForPeriode: BegrunnelseGrunnlagForPersonIPeriode,
    begrunnelseGrunnlagForrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
): Boolean {
    val andelerForrigePeriode = begrunnelseGrunnlagForrigePeriode?.andeler ?: emptyList()
    val andelerDennePerioden = begrunnelseGrunnlagForPeriode.andeler
    return andelerDennePerioden.any { andelIPeriode ->
        val sammeAndelForrigePeriode = andelerForrigePeriode.singleOrNull { andelIPeriode.type == it.type }
        sammeAndelForrigePeriode != null && andelIPeriode.sats > sammeAndelForrigePeriode.sats
    }
}
