package no.nav.familie.ba.sak.config

import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ScriptUtils
import javax.annotation.PostConstruct
import javax.sql.DataSource

abstract class AbstractTestWithJdbcTables(private val dataSource: DataSource) {
    @PostConstruct
    fun initJdbcTables(){
        dataSource.connection.use { conn ->
            ScriptUtils.executeSqlScript(conn, ClassPathResource("sql/prosessering_jdbc.sql"))
        }
    }
}