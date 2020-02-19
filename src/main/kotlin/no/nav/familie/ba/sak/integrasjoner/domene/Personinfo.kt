package no.nav.familie.ba.sak.integrasjoner.domene

import java.time.LocalDate

data class Personinfo(var fødselsdato: LocalDate, val geografiskTilknytning: String? = null, val diskresjonskode: String? = null)
