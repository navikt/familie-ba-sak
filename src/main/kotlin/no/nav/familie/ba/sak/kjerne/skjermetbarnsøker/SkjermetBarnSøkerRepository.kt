package no.nav.familie.ba.sak.kjerne.skjermetbarnsøker

import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SkjermetBarnSøkerRepository : JpaRepository<SkjermetBarnSøker, Long> {
    fun findByAktør(aktør: Aktør): SkjermetBarnSøker?
}
