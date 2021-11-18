package no.nav.familie.ba.sak.kjerne.personident

import org.springframework.data.jpa.repository.JpaRepository

interface PersonidentRepository : JpaRepository<Personident, String>
