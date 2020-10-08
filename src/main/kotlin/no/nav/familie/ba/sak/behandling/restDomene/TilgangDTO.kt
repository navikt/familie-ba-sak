package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.pdl.internal.ADRESSEBESKYTTELSEGRADERING

data class TilgangDTO (
        val saksbehandlerHarTilgang: Boolean,
        val adressebeskyttelsegradering: ADRESSEBESKYTTELSEGRADERING
)