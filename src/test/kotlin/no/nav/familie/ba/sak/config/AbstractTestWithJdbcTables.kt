package no.nav.familie.ba.sak.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.springframework.test.context.ActiveProfiles
import javax.sql.DataSource

@TestConfiguration
@ActiveProfiles("!preprod", "!prod")
class AbstractTestWithJdbcTables(dataSource: DataSource) {
    init {
        dataSource.connection.use { conn ->
            ScriptUtils.executeSqlScript(conn, ClassPathResource("sql/prosessering_jdbc.sql"))
        }
    }
}
