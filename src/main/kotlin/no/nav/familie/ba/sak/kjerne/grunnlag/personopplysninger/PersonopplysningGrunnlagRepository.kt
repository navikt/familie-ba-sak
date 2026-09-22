package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface PersonopplysningGrunnlagRepository : JpaRepository<PersonopplysningGrunnlag, Long> {
    @Query("SELECT gr FROM PersonopplysningGrunnlag gr WHERE gr.behandlingId = :behandlingId AND gr.aktiv = true")
    fun findByBehandlingAndAktiv(behandlingId: Long): PersonopplysningGrunnlag?

    @Query("SELECT gr FROM PersonopplysningGrunnlag gr WHERE gr.behandlingId in :behandlingIder AND gr.aktiv = true")
    fun hentAktivForBehandlinger(behandlingIder: Collection<Long>): List<PersonopplysningGrunnlag>

    @Query(
        """
        SELECT new no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonEnkel(p.type, a, p.fødselsdato, d.dødsfallDato, p.målform)
        FROM Person p
        JOIN p.personopplysningGrunnlag gr
        JOIN p.aktør a
        LEFT JOIN p.dødsfall d
        WHERE gr.behandlingId = :behandlingId 
        AND gr.aktiv = true
        AND p.type IN ('SØKER', 'BARN')
        """,
    )
    fun finnSøkerOgBarnAktørerTilAktiv(behandlingId: Long): List<PersonEnkel>

    @Query(
        """
        SELECT new no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonEnkel(p.type, a, p.fødselsdato, d.dødsfallDato, p.målform)
        FROM Person p
        JOIN p.personopplysningGrunnlag gr
        JOIN p.aktør a
        JOIN Behandling b ON b.id = gr.behandlingId
        LEFT JOIN p.dødsfall d
        WHERE b.fagsak.id = :fagsakId 
        AND gr.aktiv = true
        AND p.type IN ('SØKER', 'BARN')
        """,
    )
    fun finnSøkerOgBarnAktørerTilFagsak(fagsakId: Long): Set<PersonEnkel>

    @Query(
        """
        SELECT DISTINCT relevantPerson.aktør
        FROM Person relevantPerson
        JOIN relevantPerson.personopplysningGrunnlag relevantGrunnlag
        JOIN Behandling relevantBehandling ON relevantBehandling.id = relevantGrunnlag.behandlingId
        WHERE relevantGrunnlag.aktiv = true
        AND relevantPerson.type IN ('SØKER', 'BARN')
        AND relevantBehandling.fagsak.arkivert = false
        AND EXISTS (
            SELECT 1
            FROM Person person
            JOIN person.personopplysningGrunnlag personGrunnlag
            JOIN Behandling behandling ON behandling.id = personGrunnlag.behandlingId
            WHERE person.aktør = :aktør
            AND personGrunnlag.aktiv = true
            AND behandling.fagsak.id = relevantBehandling.fagsak.id
        )
        """,
    )
    fun finnSøkerOgBarnPåFagsakerHvorAktørInngår(aktør: Aktør): Set<Aktør>

    /**
     * Inaktive grunnlag som trygt kan slettes: behandlingen har også et aktivt grunnlag.
     * [etterId] er en id-markør slik at allerede gjennomgåtte rader ikke skannes på nytt innenfor én kjøring.
     */
    @Query(
        """
        SELECT gr.id FROM PersonopplysningGrunnlag gr
        WHERE gr.aktiv = false
        AND gr.id > :etterId
        AND EXISTS (
            SELECT 1 FROM PersonopplysningGrunnlag aktivtGrunnlag
            WHERE aktivtGrunnlag.behandlingId = gr.behandlingId
            AND aktivtGrunnlag.aktiv = true
        )
        ORDER BY gr.id
        """,
    )
    fun finnIderForInaktiveGrunnlagMedAktivtGrunnlagPåSammeBehandling(
        etterId: Long,
        pageable: Pageable,
    ): List<Long>

    /**
     * Inaktive grunnlag på behandlinger som ikke har et aktivt grunnlag. Skal være 0.
     * Er den > 0 er invarianten "nøyaktig ett aktivt grunnlag per behandling" brutt,
     * og radene slettes bevisst ikke – da ville behandlingen stått uten persongrunnlag.
     */
    @Query(
        """
        SELECT count(gr) FROM PersonopplysningGrunnlag gr
        WHERE gr.aktiv = false
        AND NOT EXISTS (
            SELECT 1 FROM PersonopplysningGrunnlag aktivtGrunnlag
            WHERE aktivtGrunnlag.behandlingId = gr.behandlingId
            AND aktivtGrunnlag.aktiv = true
        )
        """,
    )
    fun tellInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling(): Long

    /**
     * Bulk-sletting. po_person og de åtte po_*-tabellene ryddes av ON DELETE CASCADE i databasen
     */
    @Modifying
    @Query("DELETE FROM PersonopplysningGrunnlag gr WHERE gr.id IN :grunnlagIder")
    fun slettPersonopplysningsgrunnlag(grunnlagIder: List<Long>): Int
}
