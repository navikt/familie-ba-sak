package no.nav.familie.ba.sak.kjerne.personident

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PersonidentRepository : JpaRepository<Personident, Long> {

    @Query("SELECT p FROM Personident p WHERE p.fødselsnummer = :personIdent")
    fun hentAktivIdent(personIdent: String): Personident?

    @Query("SELECT p FROM Personident p WHERE p.aktørId.id = :aktørId AND p.aktiv = true")
    fun hentAktivIdentForAktørId(aktørId: String): Personident?
}
