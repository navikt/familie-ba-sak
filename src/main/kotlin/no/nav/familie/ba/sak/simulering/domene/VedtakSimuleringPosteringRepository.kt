package no.nav.familie.ba.sak.simulering.domene

import org.springframework.data.jpa.repository.JpaRepository

interface VedtakSimuleringPosteringRepository : JpaRepository<VedtakSimuleringPostering, Long> {
}