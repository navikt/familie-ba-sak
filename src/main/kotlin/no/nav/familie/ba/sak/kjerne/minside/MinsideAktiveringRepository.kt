package no.nav.familie.ba.sak.kjerne.minside

import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MinsideAktiveringRepository : JpaRepository<MinsideAktivering, Long> {
    fun existsByAktørAndAktivertIsTrue(
        aktør: Aktør,
    ): Boolean

    fun findByAktør(aktør: Aktør): MinsideAktivering?

    fun findAllByAktørInAndAktivertIsTrue(aktører: List<Aktør>): List<MinsideAktivering>
}
