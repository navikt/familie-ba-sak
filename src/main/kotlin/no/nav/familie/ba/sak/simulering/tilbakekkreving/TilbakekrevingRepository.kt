package no.nav.familie.ba.sak.simulering.tilbakekkreving

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface TilbakekrevingRepository : JpaRepository<Tilbakekreving, Long>