package no.nav.familie.ba.sak.fake

import no.nav.familie.ba.sak.config.LeaderClientService

class FakeLeaderClientService : LeaderClientService {
    override fun isLeader(): Boolean = true
}
