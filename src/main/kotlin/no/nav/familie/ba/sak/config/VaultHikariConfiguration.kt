package no.nav.familie.ba.sak.config

import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.vault.config.databases.VaultDatabaseProperties
import org.springframework.context.annotation.Configuration
import org.springframework.vault.core.lease.SecretLeaseContainer
import org.springframework.vault.core.lease.domain.RequestedSecret
import org.springframework.vault.core.lease.event.SecretLeaseCreatedEvent

@Configuration
@ConditionalOnProperty(name = ["spring.cloud.vault.enabled"])
class VaultHikariConfig(private val container: SecretLeaseContainer,
                        private val hikariDataSource: HikariDataSource,
                        private val props: VaultDatabaseProperties) : InitializingBean {

    override fun afterPropertiesSet() {
        val secret = RequestedSecret.rotating(props.backend + "/creds/" + props.role)
        container.addLeaseListener { leaseEvent ->
            if (leaseEvent.source === secret && leaseEvent is SecretLeaseCreatedEvent) {
                LOGGER.info("Rotating creds for path: " + leaseEvent.getSource().path)
                hikariDataSource.username = leaseEvent.secrets["username"].toString()
                hikariDataSource.password = leaseEvent.secrets["password"].toString()
                hikariDataSource.hikariPoolMXBean.softEvictConnections()
            }
        }
        container.addRequestedSecret(secret)
    }

    override fun toString(): String {
        return "${javaClass.simpleName} [container=$container, hikariDataSource=$hikariDataSource, props=$props]"
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(VaultHikariConfig::class.java)
    }
}
