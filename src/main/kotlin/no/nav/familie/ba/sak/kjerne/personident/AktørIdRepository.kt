package no.nav.familie.ba.sak.kjerne.personident

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface AktørIdRepository : JpaRepository<Aktør, String> {
    @Query("SELECT a FROM Aktør a WHERE a.aktørId = :aktørId")
    fun findByAktørIdOrNull(aktørId: String): Aktør?

    /*
    Patchemetodene bør kun brukes ved absolutt nødvendighet. Som ved merge av identer. Patchingen bruker sql og ikke JPA
    for å fortsatt beholde jpa metodene som immutable og ikke oppdaterbar.

    Denne er avhengig av at cascade on update queryene i V212__aktoerId_splitt_update_cascade.sql er lagt til i databasen
     */
    @Modifying
    @Query("update aktoer set aktoer_id = :nyAktørId where aktoer_id = :gammelAktørId", nativeQuery = true)
    fun patchAktørMedNyAktørId(
        gammelAktørId: String,
        nyAktørId: String,
    )

    @Query(
        """
            SELECT a.aktoer_id
            FROM aktoer a
            LEFT JOIN po_person p ON p.fk_aktoer_id = a.aktoer_id
            LEFT JOIN fagsak f ON f.fk_aktoer_id = a.aktoer_id
            WHERE p.fk_aktoer_id IS NULL 
            AND f.fk_aktoer_id IS NULL
            LIMIT :limit""",
        nativeQuery = true,
    )
    fun finnAktørerSomKanSlettes(limit: Long): List<String>
}
