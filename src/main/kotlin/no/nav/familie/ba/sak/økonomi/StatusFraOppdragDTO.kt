package no.nav.familie.ba.sak.økonomi

data class StatusFraOppdragDTO(val fagsystem: String,
                               val personIdent: String,
                               val behandlingsId: Long,
                               val vedtaksId: Long)
