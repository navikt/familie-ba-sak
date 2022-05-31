package no.nav.familie.ba.sak.kjerne.eøs.felles

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface PeriodeOgBarnSkjemaRepository<T : PeriodeOgBarnSkjemaEntitet<T>> : JpaRepository<T, Long> {

    fun findByBehandlingId(behandlingId: Long): Collection<T>
}
