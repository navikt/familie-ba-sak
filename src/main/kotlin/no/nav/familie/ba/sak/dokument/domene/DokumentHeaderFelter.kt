package no.nav.familie.ba.sak.dokument.domene

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform

data class DokumentHeaderFelter(
        val fodselsnummer: String,
        val navn: String,
        val dokumentDato: String,
        val antallBarn: Int? = null,
        val maalform: Målform,
)