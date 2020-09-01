package no.nav.familie.ba.sak.infotrygd

import java.time.LocalDate

data class InfotrygdFødselshendelseFeedDto(val fnrBarn: String)
data class InfotrygdVedtakFeedDto(val fnrStonadsmottaker: String, val datoStartNyBA: LocalDate)
