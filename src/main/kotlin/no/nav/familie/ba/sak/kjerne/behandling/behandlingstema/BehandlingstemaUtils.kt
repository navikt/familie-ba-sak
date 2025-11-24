package no.nav.familie.ba.sak.kjerne.behandling.behandlingstema

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak

fun bestemKategoriVedOpprettelse(
    overstyrtKategori: BehandlingKategori?,
    behandlingType: BehandlingType,
    behandlingÅrsak: BehandlingÅrsak,
    tilbakefallendeKategori: BehandlingKategori,
): BehandlingKategori =
    when {
        behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING ||
            (behandlingType == BehandlingType.REVURDERING && behandlingÅrsak == BehandlingÅrsak.SØKNAD) -> {
            overstyrtKategori
                ?: throw FunksjonellFeil(
                    "Behandling med type ${behandlingType.visningsnavn} " +
                        "og årsak ${behandlingÅrsak.visningsnavn} $ krever behandlingskategori",
                )
        }

        behandlingType == BehandlingType.MIGRERING_FRA_INFOTRYGD && behandlingÅrsak.erFørstegangMigreringsårsak() -> {
            overstyrtKategori ?: throw FunksjonellFeil(
                "Behandling med type ${behandlingType.visningsnavn} " +
                    "og årsak ${behandlingÅrsak.visningsnavn} $ krever behandlingskategori",
            )
        }

        else -> {
            tilbakefallendeKategori
        }
    }

fun bestemUnderkategori(
    overstyrtUnderkategori: BehandlingUnderkategori?,
    løpendeUnderkategoriFraForrigeVedtatteBehandling: BehandlingUnderkategori?,
    underkategoriFraAktivBehandling: BehandlingUnderkategori? = null,
): BehandlingUnderkategori {
    if (løpendeUnderkategoriFraForrigeVedtatteBehandling == BehandlingUnderkategori.UTVIDET) {
        return BehandlingUnderkategori.UTVIDET
    }
    val oppdatertUnderkategori = overstyrtUnderkategori ?: underkategoriFraAktivBehandling
    return oppdatertUnderkategori ?: BehandlingUnderkategori.ORDINÆR
}
