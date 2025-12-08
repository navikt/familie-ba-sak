package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.brev.domene.ISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.Tema
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import kotlin.collections.contains

data class TemaerForBegrunnelser(
    val temaerForOpphør: Set<Tema>,
    val temaForUtbetaling: Tema,
)

fun hentTemaSomPeriodeErVurdertEtter(
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
): TemaerForBegrunnelser {
    val regelverkSomBlirBorteFraForrigePeriode =
        finnRegelverkSomBlirBorte(
            dennePerioden = begrunnelseGrunnlag.dennePerioden,
            forrigePeriode = begrunnelseGrunnlag.forrigePeriode,
        )
    val regelverkSomBlirBorteFraForrigeBehandling =
        finnRegelverkSomBlirBorte(
            dennePerioden = begrunnelseGrunnlag.dennePerioden,
            forrigePeriode = begrunnelseGrunnlag.sammePeriodeForrigeBehandling,
        )
    val vurdertEtterEøsDennePerioden =
        begrunnelseGrunnlag.dennePerioden.vilkårResultater.any { it.vurderesEtter == Regelverk.EØS_FORORDNINGEN }

    val harIkkeInnvilgetEøsVilkårDennePerioden =
        begrunnelseGrunnlag.dennePerioden.vilkårResultater.any { it.vurderesEtter == Regelverk.EØS_FORORDNINGEN && it.resultat == Resultat.IKKE_OPPFYLT }

    val regelverkSomBlirBorte =
        listOfNotNull(regelverkSomBlirBorteFraForrigePeriode, regelverkSomBlirBorteFraForrigeBehandling).toSet()

    val temaerForOpphør =
        regelverkSomBlirBorte
            .ifEmpty { setOf(Tema.NASJONAL) }
            .let { regelverkSomBlirBorte ->
                if (harIkkeInnvilgetEøsVilkårDennePerioden) regelverkSomBlirBorte + Tema.EØS else regelverkSomBlirBorte
            }

    return TemaerForBegrunnelser(
        temaerForOpphør = temaerForOpphør,
        temaForUtbetaling = if (vurdertEtterEøsDennePerioden) Tema.EØS else Tema.NASJONAL,
    )
}

fun ISanityBegrunnelse.erSammeTemaSomPeriode(
    temaerForBegrunnelser: TemaerForBegrunnelser,
): Boolean =
    if (this.periodeResultat == SanityPeriodeResultat.IKKE_INNVILGET) {
        temaerForBegrunnelser.temaerForOpphør.contains(tema) || tema == Tema.FELLES
    } else {
        temaerForBegrunnelser.temaForUtbetaling == tema || tema == Tema.FELLES
    }
