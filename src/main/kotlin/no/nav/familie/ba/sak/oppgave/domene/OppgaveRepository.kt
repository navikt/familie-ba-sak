package no.nav.familie.ba.sak.oppgave.domene

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface OppgaveRepository: JpaRepository<Oppgave, Long> {

    @Query(value = "SELECT o FROM Oppgave o WHERE o.type = :oppgavetype")
    fun findByOppgavetype(oppgavetype: Oppgavetype): List<Oppgave>

    @Query(value = "SELECT o FROM Oppgave o WHERE o.erFerdigstilt = false AND o.behandling = :behandling AND o.type = :oppgavetype")
    fun findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(oppgavetype: Oppgavetype, behandling: Behandling): Oppgave?
}