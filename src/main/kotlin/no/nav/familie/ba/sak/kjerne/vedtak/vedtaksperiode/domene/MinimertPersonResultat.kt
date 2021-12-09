package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat

data class MinimertPersonResultat(
    val personIdent: String,
    val minimerteVilkårResultater: List<MinimertVilkårResultat>,
    val andreVurderinger: List<AnnenVurdering>
)

fun PersonResultat.tilMinimertPersonResultat() =
    MinimertPersonResultat(
        personIdent = this.personIdent,
        minimerteVilkårResultater = this.vilkårResultater.map { it.tilMinimertVilkårResultat() },
        andreVurderinger = this.andreVurderinger.toList()
    )

fun List<MinimertPersonResultat>.harPersonerSomManglerOpplysninger(): Boolean =
    this.any { personResultat ->
        personResultat.andreVurderinger.any {
            it.type == AnnenVurderingType.OPPLYSNINGSPLIKT && it.resultat == Resultat.IKKE_OPPFYLT
        }
    }
