package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.Feil

object VilkårServiceValidering {
    const val fantIkkeAktivVilkårsvurderingFeilmelding = "Fant ikke aktiv vilkårsvurdering"
    const val fantIkkeVilkårsvurderingForPersonFeilmelding = "Fant ikke vilkårsvurdering for person"

    val EØS_SKRUDD_AV = Feil(
        message = "EØS er ikke togglet på",
        frontendFeilmelding = "Funksjonalitet for EØS er ikke lansert."
    )

    val MANGLER_VILKÅRSVURDERING_VED_ENDRING_AV_VILKÅR = Feil(
        message = "Fant ikke aktiv vilkårsvurdering ved endring på vilkår",
        frontendFeilmelding = fantIkkeAktivVilkårsvurderingFeilmelding
    )

    val MANGLER_VILKÅRSVURDERING_VED_SLETTING_AV_VILKÅR = Feil(
        message = "Fant ikke aktiv vilkårsvurdering ved sletting av vilkår",
        frontendFeilmelding = fantIkkeAktivVilkårsvurderingFeilmelding
    )

    val MANGLER_VILKÅRSVURDERING_VED_OPPRETTELSE_AV_VILKÅRSPERIODE = Feil(
        message = "Fant ikke aktiv vilkårsvurdering ved opprettelse av vilkårsperiode",
        frontendFeilmelding = fantIkkeAktivVilkårsvurderingFeilmelding
    )

    val MANGLER_VILKÅRSVURDERING_FOR_PERSON: (String) -> Feil = { ident ->
        Feil(
            message = fantIkkeVilkårsvurderingForPersonFeilmelding,
            frontendFeilmelding = "Fant ikke vilkårsvurdering for person med ident '$ident"
        )
    }

    val MANGLER_VILKÅRSVURDERING_FOR_BEHANDLING: (Long) -> Feil = { behandlingId ->
        Feil("Fant ikke aktiv vilkårsvurdering for behandling $behandlingId")
    }

    val MANGLER_PERSONOPPLYSNINGSGRUNNLAG_FOR_BEHANDLING: (Long) -> Feil = { behandlingId ->
        Feil(
            message = "Fant ikke personopplysninggrunnlag " +
                "for behandling $behandlingId"
        )
    }
}
