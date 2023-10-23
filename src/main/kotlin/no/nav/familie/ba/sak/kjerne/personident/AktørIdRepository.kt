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
     */
    @Modifying
    @Query("update andel_tilkjent_ytelse set fk_aktoer_id = :nyAktørId where fk_aktoer_id = :gammelAktørId and fk_behandling_id = :behandlingId", nativeQuery = true)
    fun patchAndelTilkjentYteleseMedNyAktør(gammelAktørId: String, nyAktørId: String, behandlingId: Long)

    /*
    Patchemetodene bør kun brukes ved absolutt nødvendighet. Som ved merge av identer. Patchingen bruker sql og ikke JPA
    for å fortsatt beholde jpa metodene som immutable og ikke oppdaterbar.
     */
    @Modifying
    @Query("update po_person set fk_aktoer_id = :nyAktørId where id = :personId and fk_aktoer_id = :gammelAktørId", nativeQuery = true)
    fun patchPersonMedNyAktør(gammelAktørId: String, nyAktørId: String, personId: Long)

    /*
    Patchemetodene bør kun brukes ved absolutt nødvendighet. Som ved merge av identer. Patchingen bruker sql og ikke JPA
    for å fortsatt beholde jpa metodene som immutable og ikke oppdaterbar.
     */
    @Modifying
    @Query("update person_resultat set fk_aktoer_id = :nyAktørId where person_resultat.fk_vilkaarsvurdering_id = :vilkårsvurderingId and fk_aktoer_id = :gammelAktørId", nativeQuery = true)
    fun patchPersonResultatMedNyAktør(gammelAktørId: String, nyAktørId: String, vilkårsvurderingId: Long)

    /*
    Patchemetodene bør kun brukes ved absolutt nødvendighet. Som ved merge av identer. Patchingen bruker sql og ikke JPA
    for å fortsatt beholde jpa metodene som immutable og ikke oppdaterbar.
     */
    @Modifying
    @Query("update gr_periode_overgangsstonad set fk_aktoer_id = :nyAktørId where fk_behandling_id = :behandlingId and fk_aktoer_id = :gammelAktørId", nativeQuery = true)
    fun patchPeriodeOvergangstønadtMedNyAktør(gammelAktørId: String, nyAktørId: String, behandlingId: Long)
}
