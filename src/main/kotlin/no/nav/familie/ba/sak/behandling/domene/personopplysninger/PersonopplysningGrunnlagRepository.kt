package no.nav.familie.ba.sak.behandling.domene.personopplysninger

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface PersonopplysningGrunnlagRepository : JpaRepository<PersonopplysningGrunnlag?, Long?> {
    @Query("SELECT gr FROM PersonopplysningGrunnlag gr WHERE behandlingId = ?1 AND aktiv = true")
    fun findByBehandlingAndAktiv(behandlingId: Long?): PersonopplysningGrunnlag?
}