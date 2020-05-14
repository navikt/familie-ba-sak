package no.nav.familie.ba.sak.behandling.fagsak

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FagsakPersonRepository : JpaRepository<FagsakPerson, Long>
