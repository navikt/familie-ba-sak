package no.nav.familie.ba.sak.oppgave.domene

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.RestPersonInfo
import no.nav.familie.ba.sak.integrasjoner.domene.Journalpost

data class DataForManuellJournalføring(
        val oppgave: OppgaveDto,
        val person: RestPersonInfo,
        val journalpost: Journalpost
)