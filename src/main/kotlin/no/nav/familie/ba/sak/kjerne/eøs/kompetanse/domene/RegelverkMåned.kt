package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene

import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårRegelverk
import java.time.YearMonth

data class RegelverkMåned(
    val måned: YearMonth,
    val vurderesEtter: VilkårRegelverk?
)
