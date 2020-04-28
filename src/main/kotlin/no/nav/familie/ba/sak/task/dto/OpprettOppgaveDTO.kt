package no.nav.familie.ba.sak.task.dto

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import java.time.LocalDate

data class OpprettOppgaveDTO (
        val behandlingId: Long,
        val oppgavetype: Oppgavetype,
        val fristForFerdigstillelse: LocalDate
)