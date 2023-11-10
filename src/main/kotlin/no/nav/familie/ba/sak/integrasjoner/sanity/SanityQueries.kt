package no.nav.familie.ba.sak.integrasjoner.sanity

const val HENT_BEGRUNNELSER =
    "*[_type == \"begrunnelse\" && regelverk != \"EØS\" && apiNavn != null && navnISystem != null]"

const val HENT_EØS_BEGRUNNELSER =
    "*[_type == \"begrunnelse\" && regelverk == \"EØS\" && apiNavn != null && navnISystem != null]"
