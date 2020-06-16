package no.nav.familie.ba.sak.infotrygd

import no.nav.commons.foedselsnummer.FoedselsNr

data class InfotrygdSøkRequest(val brukere: List<FoedselsNr>,
                               val barn: List<FoedselsNr>? = null)

data class InfotrygdSøkResponse(val ingenTreff: Boolean)