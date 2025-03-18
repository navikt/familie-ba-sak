package no.nav.familie.ba.sak.integrasjoner.journalføring.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType

enum class Journalføringsbehandlingstype {
    FØRSTEGANGSBEHANDLING,
    REVURDERING,
    MIGRERING_FRA_INFOTRYGD,
    TEKNISK_ENDRING,
    KLAGE,
    TILBAKEKREVING,
    ;

    fun tilBehandingType(): BehandlingType =
        when (this) {
            FØRSTEGANGSBEHANDLING -> BehandlingType.FØRSTEGANGSBEHANDLING
            REVURDERING -> BehandlingType.REVURDERING
            MIGRERING_FRA_INFOTRYGD -> BehandlingType.MIGRERING_FRA_INFOTRYGD
            TEKNISK_ENDRING -> BehandlingType.TEKNISK_ENDRING
            KLAGE -> throw Feil("Klage finnes ikke i ${BehandlingType::class.simpleName}. Behandles i ekstern applikasjon.")
            TILBAKEKREVING -> throw Feil("Tilbakekreving finnes ikke i ${BehandlingType::class.simpleName}. Behandles i ekstern applikasjon.")
        }

    fun skalBehandlesIEksternApplikasjon() = this == KLAGE || this == TILBAKEKREVING

    fun tilVisningsnavn(): String =
        when (this) {
            FØRSTEGANGSBEHANDLING -> BehandlingType.FØRSTEGANGSBEHANDLING.visningsnavn
            REVURDERING -> BehandlingType.REVURDERING.visningsnavn
            MIGRERING_FRA_INFOTRYGD -> BehandlingType.MIGRERING_FRA_INFOTRYGD.visningsnavn
            TEKNISK_ENDRING -> BehandlingType.TEKNISK_ENDRING.visningsnavn
            KLAGE -> "Klage"
            TILBAKEKREVING -> "Tilbakekreving"
        }
}
