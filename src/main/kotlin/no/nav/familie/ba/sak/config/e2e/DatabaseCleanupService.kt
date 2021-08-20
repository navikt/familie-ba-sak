package no.nav.familie.ba.sak.config.e2e

import org.reflections.Reflections
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.persistence.Table
import javax.persistence.metamodel.Metamodel
import javax.transaction.Transactional
import kotlin.reflect.full.findAnnotation
import org.springframework.data.relational.core.mapping.Table as JdbcTable


/**
 * Test utility service that allows to truncate all tables in the test database.
 * Inspired by: http://www.greggbolinger.com/truncate-all-tables-in-spring-boot-jpa-app/
 * @author Sebastien Dubois
 */
@Service
@Profile("dev", "e2e", "postgres")
class DatabaseCleanupService(
    private val entityManager: EntityManager,
    private val environment: Environment
) : InitializingBean {

    private val logger = LoggerFactory.getLogger(DatabaseCleanupService::class.java)
    private lateinit var tableNames: List<String>

    /**
     * Uses the JPA metamodel to find all managed types then try to get the [Table] annotation's from each (if present) to discover the table name.
     * If the [Table] annotation is not defined then we skip that entity (oops :p)
     */
    override fun afterPropertiesSet() {
        val metaModel: Metamodel = entityManager.metamodel
        tableNames = metaModel.managedTypes
            .filter {
                it.javaType.kotlin.findAnnotation<Table>() != null || it.javaType.kotlin.findAnnotation<JdbcTable>() != null
            }
            .map {
                val tableAnnotation: Table? = it.javaType.kotlin.findAnnotation()
                val jdbcTableAnnotation: JdbcTable? = it.javaType.kotlin.findAnnotation()
                tableAnnotation?.name ?: jdbcTableAnnotation?.value ?: throw IllegalStateException("should never get here")
            } + getJdbcTableNames()

    }

    private fun getJdbcTableNames(): List<String> {
        val ref = Reflections("no.nav.familie")
        return ref.getTypesAnnotatedWith(JdbcTable::class.java).map {
            it.getAnnotation(JdbcTable::class.java).value
        }
    }

    /**
     * Utility method that truncates all identified tables
     */
    @Transactional
    fun truncate() {
        logger.info("Truncating tables: $tableNames")
        entityManager.flush()
        if (environment.activeProfiles.contains("e2e") || environment.activeProfiles.contains("postgres")) {
            tableNames.forEach { tableName ->
                entityManager.createNativeQuery("TRUNCATE TABLE $tableName CASCADE").executeUpdate()
            }
        } else {
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TO FALSE").executeUpdate()
            tableNames.forEach { tableName ->
                entityManager.createNativeQuery("TRUNCATE TABLE $tableName").executeUpdate()
            }
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TO TRUE").executeUpdate()
        }
    }
}