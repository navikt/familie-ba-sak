package no.nav.familie.ba.sak.task.dto

data class StatusFraOppdragDTO(val fagsystem: String,
                               val personIdent: String,
                               val behandlingsId: Long,
                               val vedtaksId: Long)
