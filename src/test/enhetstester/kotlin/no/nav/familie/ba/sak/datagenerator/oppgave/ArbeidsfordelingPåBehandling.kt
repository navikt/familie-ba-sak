package no.nav.familie.ba.sak.datagenerator.oppgave

import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.kontrakter.felles.enhet.Enhet

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

fun lagEnhet(
    enhetsnummer: String,
): Enhet = Enhet(enhetsnummer = enhetsnummer)
