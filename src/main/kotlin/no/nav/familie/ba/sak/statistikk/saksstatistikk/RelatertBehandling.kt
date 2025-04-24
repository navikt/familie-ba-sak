package no.nav.familie.ba.sak.statistikk.saksstatistikk

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.EksternBehandlingRelasjon

data class RelatertBehandling(
    val id: String,
    val fagsystem: Fagsystem,
) {
    enum class Fagsystem {
        BA,
        KLAGE,
        TILBAKEKREVING,
    }

    companion object Factory {
        fun fraBarnetrygdbehandling(barnetrygdbehandling: Behandling) =
            RelatertBehandling(
                id = barnetrygdbehandling.id.toString(),
                fagsystem = Fagsystem.BA,
            )

        fun fraEksternBehandlingRelasjon(eksternBehandlingRelasjon: EksternBehandlingRelasjon): RelatertBehandling =
            RelatertBehandling(
                id = eksternBehandlingRelasjon.eksternBehandlingId,
                fagsystem =
                    when (eksternBehandlingRelasjon.eksternBehandlingFagsystem) {
                        EksternBehandlingRelasjon.Fagsystem.KLAGE -> Fagsystem.KLAGE
                        EksternBehandlingRelasjon.Fagsystem.TILBAKEKREVING -> Fagsystem.TILBAKEKREVING
                    },
            )
    }
}
