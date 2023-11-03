package no.nav.familie.ba.sak.integrasjoner.sanity

const val hentBegrunnelser =
    "*[_type == \"begrunnelse\" && regelverk != \"EØS\" && apiNavn != null && navnISystem != null]"

const val hentEØSBegrunnelser =
    "*[_type == \"begrunnelse\" && regelverk == \"EØS\" && apiNavn != null && navnISystem != null]"
