package no.nav.familie.ba.sak.config

import no.nav.familie.sikkerhet.AuthorizationFilter
import no.nav.familie.sikkerhet.AcceptedClient
import no.nav.familie.sikkerhet.OIDCUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@EnableConfigurationProperties(AcceptedClientPropList::class)
@Configuration
class AuthorizationConfig(
        private val oidcUtil: OIDCUtil,
        private val environment: Environment
) {
    @Autowired
    lateinit var acceptedClientPropList: AcceptedClientPropList

    @Bean
    fun authorizationFilter(): AuthorizationFilter {
        return AuthorizationFilter(oidcUtil = oidcUtil,
                                   acceptedClients = acceptedClientPropList.toAcceptedClientList(),
                                   disabled = environment.activeProfiles.any {
                                       listOf("e2e", "postgres")
                                               .contains(it.trim(' '))
                                   })
    }
}

@ConfigurationProperties(prefix = "accepted-clients")
class AcceptedClientPropList {
    var entries: List<AcceptedClientProps> = listOf()

    fun toAcceptedClientList(): List<AcceptedClient> {
        return entries.map {
            if (it.acceptedPaths.isEmpty()) {
                AcceptedClient(it.clientId, listOf("/"))
            } else {
                AcceptedClient(it.clientId, it.acceptedPaths)
            }
        }
    }
}

class AcceptedClientProps {
    var clientId: String = ""
    var acceptedPaths: List<String> = listOf()
}