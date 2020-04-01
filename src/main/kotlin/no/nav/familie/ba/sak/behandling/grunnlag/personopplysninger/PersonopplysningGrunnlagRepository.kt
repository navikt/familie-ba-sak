package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PersonopplysningGrunnlagRepository : JpaRepository<PersonopplysningGrunnlag, Long> {

    @Query("SELECT gr FROM PersonopplysningGrunnlag gr WHERE gr.behandlingId = ?1 AND gr.aktiv = true")
    fun findByBehandlingAndAktiv(behandlingId: Long): PersonopplysningGrunnlag?


    @Query("SELECT gr FROM PersonopplysningGrunnlag gr WHERE gr.behandlingId = ?1")
    fun findByBehandling(behandlingId: Long): PersonopplysningGrunnlag?
}
