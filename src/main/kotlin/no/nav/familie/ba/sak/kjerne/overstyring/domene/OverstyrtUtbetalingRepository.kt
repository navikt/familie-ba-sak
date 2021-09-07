package no.nav.familie.ba.sak.kjerne.overstyring.domene

import org.springframework.data.jpa.repository.JpaRepository

interface OverstyrtUtbetalingRepository : JpaRepository<OverstyrtUtbetaling, Long> {
}