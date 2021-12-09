package no.nav.familie.ba.sak.kjerne.fagsak

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FagsakPersonRepository : JpaRepository<FagsakPerson, Long>
