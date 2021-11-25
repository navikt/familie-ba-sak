package no.nav.familie.ba.sak.kjerne.personident

import org.springframework.data.jpa.repository.JpaRepository

interface AktørIdRepository : JpaRepository<Aktør, String>
