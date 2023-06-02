package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PersonopplysningGrunnlagRepository : JpaRepository<PersonopplysningGrunnlag, Long> {

    @Query("SELECT gr FROM PersonopplysningGrunnlag gr WHERE gr.behandlingId = :behandlingId AND gr.aktiv = true")
    fun findByBehandlingAndAktiv(behandlingId: Long): PersonopplysningGrunnlag?

    @Query(
        """
        SELECT new no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonPåBehandling(p.type, a, p.fødselsdato, d.dødsfallDato)
        FROM Person p
        JOIN p.personopplysningGrunnlag gr
        JOIN p.aktør a
        LEFT JOIN p.dødsfall d
        WHERE gr.behandlingId = :behandlingId 
        AND gr.aktiv = true
        AND p.type IN ('SØKER', 'BARN')
        """,
    )
    fun finnSøkerOgBarnAktørerTilAktiv(behandlingId: Long): List<PersonPåBehandling>
}
