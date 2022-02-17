package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene

import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import java.time.YearMonth

data class RegelverkPeriode(
    val fom: YearMonth,
    val tom: YearMonth?,
    val vurderesEtter: Regelverk?
)
