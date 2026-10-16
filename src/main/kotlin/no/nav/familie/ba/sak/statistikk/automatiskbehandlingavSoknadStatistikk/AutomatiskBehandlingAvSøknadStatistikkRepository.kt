package no.nav.familie.ba.sak.statistikk.automatiskbehandlingavSoknadStatistikk

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AutomatiskBehandlingAvSøknadStatistikkRepository : JpaRepository<AutomatiskBehandlingAvSøknadStatistikk, Long>
