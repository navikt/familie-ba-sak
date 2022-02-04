package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.time.LocalDate
import java.time.YearMonth

val MAX_MÅNED = LocalDate.MAX.toYearMonth()
val MIN_MÅNED = LocalDate.MIN.toYearMonth()

data class VilkårResultatMåned(
    val vilkårType: Vilkår,
    val resultat: Resultat?,
    val måned: YearMonth,
    val vurderesEtter: Regelverk?
)
