package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.hentUtsnitt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer.Tidslinjer
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import java.time.YearMonth

object Behandlingutils {

    fun hentSisteBehandlingSomErIverksatt(iverksatteBehandlinger: List<Behandling>): Behandling? {
        return iverksatteBehandlinger
            .filter { it.steg == StegType.BEHANDLING_AVSLUTTET }
            .maxByOrNull { it.opprettetTidspunkt }
    }

    fun hentForrigeBehandlingSomErVedtatt(
        behandlinger: List<Behandling>,
        behandlingFørFølgende: Behandling
    ): Behandling? {
        return behandlinger
            .filter { it.opprettetTidspunkt.isBefore(behandlingFørFølgende.opprettetTidspunkt) && it.steg == StegType.BEHANDLING_AVSLUTTET && !it.erHenlagt() }
            .maxByOrNull { it.opprettetTidspunkt }
    }

    fun hentForrigeIverksatteBehandling(
        iverksatteBehandlinger: List<Behandling>,
        behandlingFørFølgende: Behandling
    ): Behandling? {
        return hentIverksatteBehandlinger(
            iverksatteBehandlinger,
            behandlingFørFølgende
        ).maxByOrNull { it.opprettetTidspunkt }
    }

    fun hentIverksatteBehandlinger(
        iverksatteBehandlinger: List<Behandling>,
        behandlingFørFølgende: Behandling
    ): List<Behandling> {
        return iverksatteBehandlinger
            .filter { it.opprettetTidspunkt.isBefore(behandlingFørFølgende.opprettetTidspunkt) && it.steg == StegType.BEHANDLING_AVSLUTTET }
    }

    fun bestemKategori(
        behandlingÅrsak: BehandlingÅrsak,
        nyBehandlingKategori: BehandlingKategori?,
        løpendeBehandlingKategori: BehandlingKategori?,
        utledetBehandlingKategori: BehandlingKategori?,
    ): BehandlingKategori {
        if (nyBehandlingKategori != null) return nyBehandlingKategori

        return if (behandlingÅrsak.medførerNyVurdering() && løpendeBehandlingKategori != null) {
            løpendeBehandlingKategori
        } else if (!behandlingÅrsak.medførerNyVurdering() && utledetBehandlingKategori != null) {
            utledetBehandlingKategori
        } else {
            BehandlingKategori.NASJONAL
        }
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
            .map { it.regelverkTidslinje.hentUtsnitt(nå) }
            .any { it == Regelverk.EØS_FORORDNINGEN }

        return if (etBarnHarMinstEnEØSPeriode) {
            BehandlingKategori.EØS
        } else {
            BehandlingKategori.NASJONAL
        }
    }

    fun harBehandlingsårsakAlleredeKjørt(
        behandlingÅrsak: BehandlingÅrsak,
        behandlinger: List<Behandling>,
        måned: YearMonth
    ): Boolean {
        return behandlinger.any {
            it.opprettetTidspunkt.toLocalDate().toYearMonth() == måned && it.opprettetÅrsak == behandlingÅrsak
        }
    }

    fun validerhenleggelsestype(henleggÅrsak: HenleggÅrsak, tekniskVedlikeholdToggel: Boolean, behandlingId: Long) {
        if (!tekniskVedlikeholdToggel && henleggÅrsak == HenleggÅrsak.TEKNISK_VEDLIKEHOLD) {
            throw Feil(
                "Teknisk vedlikehold henleggele er ikke påslått for " +
                    "${SikkerhetContext.hentSaksbehandlerNavn()}. Kan ikke henlegge behandling $behandlingId."
            )
        }
    }

    fun validerBehandlingIkkeSendtTilEksterneTjenester(behandling: Behandling) {
        if (behandling.harUtførtSteg(StegType.IVERKSETT_MOT_OPPDRAG)) {
            throw FunksjonellFeil("Kan ikke henlegge behandlingen. Den er allerede sendt til økonomi.")
        }
        if (behandling.harUtførtSteg(StegType.DISTRIBUER_VEDTAKSBREV)) {
            throw FunksjonellFeil("Kan ikke henlegge behandlingen. Brev er allerede distribuert.")
        }
        if (behandling.harUtførtSteg(StegType.JOURNALFØR_VEDTAKSBREV)) {
            throw FunksjonellFeil("Kan ikke henlegge behandlingen. Brev er allerede journalført.")
        }
    }
}
