package no.nav.familie.ba.sak.config

import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("mock-leader-client")
@Primary
class MockLeaderClientService : LeaderClientService {
    override fun isLeader(): Boolean = true
}
