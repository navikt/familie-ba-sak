package no.nav.familie.ba.sak.kjerne.skjermetbarnsøker

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SkjermetBarnSøkerRepository : JpaRepository<SkjermetBarnSøker, Long> {
    fun findByAktørId(aktørId: String): SkjermetBarnSøker?
}
