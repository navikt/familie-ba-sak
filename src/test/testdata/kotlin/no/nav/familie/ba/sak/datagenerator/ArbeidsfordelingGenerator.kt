package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling

fun lagArbeidsfordelingPåBehandling(
    behandlingId: Long,
    behandlendeEnhetId: String = "0000",
    behandlendeEnhetNavn: String = "behandlendeEnhetNavn",
    manueltOverstyrt: Boolean = false,
): ArbeidsfordelingPåBehandling =
    ArbeidsfordelingPåBehandling(
        behandlingId = behandlingId,
        behandlendeEnhetId = behandlendeEnhetId,
        behandlendeEnhetNavn = behandlendeEnhetNavn,
        manueltOverstyrt = manueltOverstyrt,
    )