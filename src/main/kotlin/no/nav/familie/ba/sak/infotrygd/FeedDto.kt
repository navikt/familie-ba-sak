package no.nav.familie.ba.sak.infotrygd

import java.time.LocalDate

data class InfotrygdFødselhendelsesFeedDto(val fnrBarn: String)
data class InfotrygdVedtakFeedDto(val fnrStoenadsmottaker: String, val datoStartNyBa: LocalDate)
