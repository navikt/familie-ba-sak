package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat

/**
 * NB: Bør ikke brukes internt, men kun ut mot eksterne tjenester siden klassen
 * inneholder aktiv personIdent og ikke aktørId.
 */
data class MinimertRestPersonResultat(
    val personIdent: String,
    val minimerteVilkårResultater: List<MinimertVilkårResultat>,
    val andreVurderinger: List<AnnenVurdering>
)

fun PersonResultat.tilMinimertPersonResultat() =
    MinimertRestPersonResultat(
        personIdent = this.personIdent,
        minimerteVilkårResultater = this.vilkårResultater.map { it.tilMinimertVilkårResultat() },
        andreVurderinger = this.andreVurderinger.toList()
    )

fun List<MinimertRestPersonResultat>.harPersonerSomManglerOpplysninger(): Boolean =
    this.any { personResultat ->
        personResultat.andreVurderinger.any {
            it.type == AnnenVurderingType.OPPLYSNINGSPLIKT && it.resultat == Resultat.IKKE_OPPFYLT
        }
    }
