package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.hentUtsnitt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer.Tidslinjer
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk

object BehandlingKategoriutils {
    fun bestemKategori(
        nyBehandlingKategori: BehandlingKategori?,
        løpendeBehandlingKategori: BehandlingKategori?,
        kategoriFraInneværendeBehandling: BehandlingKategori?,
    ): BehandlingKategori =
        when {
            nyBehandlingKategori != null -> nyBehandlingKategori
            kategoriFraInneværendeBehandling != null -> kategoriFraInneværendeBehandling
            løpendeBehandlingKategori != null -> løpendeBehandlingKategori
            else -> BehandlingKategori.NASJONAL
        }

    fun bestemUnderkategori(
        nyUnderkategori: BehandlingUnderkategori?,
        nyBehandlingType: BehandlingType,
        nyBehandlingÅrsak: BehandlingÅrsak,
        løpendeUnderkategori: BehandlingUnderkategori?
    ): BehandlingUnderkategori {
        if (nyUnderkategori == null && løpendeUnderkategori == null) return BehandlingUnderkategori.ORDINÆR
        return when {
            nyUnderkategori == BehandlingUnderkategori.UTVIDET -> nyUnderkategori

            nyBehandlingType == BehandlingType.REVURDERING || nyBehandlingÅrsak == BehandlingÅrsak.ENDRE_MIGRERINGSDATO ->
                løpendeUnderkategori
                    ?: (nyUnderkategori ?: BehandlingUnderkategori.ORDINÆR)

            else -> nyUnderkategori ?: BehandlingUnderkategori.ORDINÆR
        }
    }

    fun utledLøpendeUnderkategori(andeler: List<AndelTilkjentYtelse>): BehandlingUnderkategori {
        return if (andeler.any { it.erUtvidet() && it.erLøpende() }) BehandlingUnderkategori.UTVIDET else BehandlingUnderkategori.ORDINÆR
    }

    fun utledLøpendekategori(barnasTidslinjer: Map<Aktør, Tidslinjer.BarnetsTidslinjerTimeline>?): BehandlingKategori {
        if (barnasTidslinjer == null) return BehandlingKategori.NASJONAL

        val nå = MånedTidspunkt.nå()

        val etBarnHarMinstEnEØSPeriode = barnasTidslinjer
            .values
            .map { it.regelverkTidslinje }
            .map { it.hentUtsnitt(nå) }
            .any { it == Regelverk.EØS_FORORDNINGEN }

        return if (etBarnHarMinstEnEØSPeriode) {
            BehandlingKategori.EØS
        } else {
            BehandlingKategori.NASJONAL
        }
    }
}
