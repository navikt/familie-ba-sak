package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling

data class RestArbeidsfordelingPåBehandling(
        val behandlendeEnhetId: String,
        val behandlendeEnhetNavn: String,
        val manueltOverstyrt: Boolean = false,
)

fun ArbeidsfordelingPåBehandling.tilRestArbeidsfordelingPåBehandling() = RestArbeidsfordelingPåBehandling(
        behandlendeEnhetId = this.behandlendeEnhetId,
        behandlendeEnhetNavn = this.behandlendeEnhetNavn,
        manueltOverstyrt = this.manueltOverstyrt
)