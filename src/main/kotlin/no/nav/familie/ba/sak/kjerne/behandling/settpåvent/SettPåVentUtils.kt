package no.nav.familie.ba.sak.kjerne.behandling.settpåvent

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import java.time.LocalDate

fun validerBehandlingKanSettesPåVent(
    gammelSettPåVent: SettPåVent?,
    frist: LocalDate,
    behandling: Behandling,
) {
    if (gammelSettPåVent != null) {
        throw FunksjonellFeil(
            melding = "Behandling ${behandling.id} er allerede satt på vent.",
            frontendFeilmelding = "Behandlingen er allerede satt på vent.",
        )
    }

    validerFristErFremITiden(behandling, frist)

    if (behandling.status != BehandlingStatus.UTREDES) {
        throw FunksjonellFeil(
            melding = "Behandling ${behandling.id} har status=${behandling.status} og kan ikke settes på vent.",
            frontendFeilmelding = "Behandlingen må ha status utredes for å kunne settes på vent",
        )
    }

    if (!behandling.aktiv) {
        throw Feil(
            "Behandling ${behandling.id} er ikke aktiv og kan ikke settes på vent.",
        )
    }
}

fun validerOppdaterSettBehandlingPåVent() {
}

fun validerFristErFremITiden(
    behandling: Behandling,
    frist: LocalDate,
) {
    if (frist.isBefore(LocalDate.now())) {
        throw FunksjonellFeil(
            melding = "Frist for å vente på behandling ${behandling.id} er satt før dagens dato.",
            frontendFeilmelding = "Fristen er satt før dagens dato.",
        )
    }
}

fun validerKanGjenopptaBehandling(behandling: Behandling) {
    if (behandling.status != BehandlingStatus.SATT_PÅ_VENT) {
        throw FunksjonellFeil(
            melding = "Behandling ${behandling.id} har status=${behandling.status} og kan ikke gjenopptas.",
            frontendFeilmelding = "Behandlingen må ha status satt på vent for å kunne gjenopptas",
        )
    }
}
