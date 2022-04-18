package no.nav.familie.ba.sak.kjerne.behandling.behandlingstema

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.finnHøyesteKategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.Tidslinjer
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdForTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk

fun bestemKategori(
    overstyrtKategori: BehandlingKategori?,
    kategoriFraLøpendeBehandling: BehandlingKategori?,
    kategoriFraInneværendeBehandling: BehandlingKategori? = null,
): BehandlingKategori {
    if (kategoriFraLøpendeBehandling == BehandlingKategori.EØS) return BehandlingKategori.EØS

    val oppdatertKategori = listOf(overstyrtKategori, kategoriFraInneværendeBehandling).finnHøyesteKategori()

    return oppdatertKategori ?: BehandlingKategori.NASJONAL
}

fun bestemUnderkategori(
    overstyrtUnderkategori: BehandlingUnderkategori?,
    underkategoriFraLøpendeBehandling: BehandlingUnderkategori?,
    underkategoriFraInneværendeBehandling: BehandlingUnderkategori? = null,
): BehandlingUnderkategori {
    if (underkategoriFraLøpendeBehandling == BehandlingUnderkategori.UTVIDET) return BehandlingUnderkategori.UTVIDET

    val oppdatertUnderkategori =
        listOf(overstyrtUnderkategori, underkategoriFraInneværendeBehandling).finnHøyesteKategori()

    return oppdatertUnderkategori ?: BehandlingUnderkategori.ORDINÆR
}

fun utledLøpendeUnderkategori(andeler: List<AndelTilkjentYtelse>): BehandlingUnderkategori {
    return if (andeler.any { it.erUtvidet() && it.erLøpende() }) BehandlingUnderkategori.UTVIDET else BehandlingUnderkategori.ORDINÆR
}

fun utledLøpendeKategori(
    barnasTidslinjer: Map<Aktør, Tidslinjer.BarnetsTidslinjer>?,
): BehandlingKategori {
    if (barnasTidslinjer == null) return BehandlingKategori.NASJONAL

    val nå = MånedTidspunkt.nå()

    val etBarnHarMinstEnLøpendeEØSPeriode = barnasTidslinjer
        .values
        .map { it.regelverkTidslinje.innholdForTidspunkt(nå) }
        .any { it == Regelverk.EØS_FORORDNINGEN }

    return if (etBarnHarMinstEnLøpendeEØSPeriode) {
        BehandlingKategori.EØS
    } else {
        BehandlingKategori.NASJONAL
    }
}
