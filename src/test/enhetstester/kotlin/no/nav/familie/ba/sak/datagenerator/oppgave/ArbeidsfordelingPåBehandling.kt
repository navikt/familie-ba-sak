package no.nav.familie.ba.sak.datagenerator.oppgave

import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.kontrakter.felles.enhet.Enhet
import no.nav.familie.kontrakter.felles.enhet.EnhetTilgang

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

fun lagEnhetTilgang(
    enheter: List<Enhet> = emptyList(),
): EnhetTilgang =
    EnhetTilgang(
        enheter,
    )

fun lagEnhet(
    enhetsnummer: String,
): Enhet = Enhet(enhetsnummer = enhetsnummer)
