package no.nav.familie.ba.sak.config

import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ScriptUtils
import javax.sql.DataSource

abstract class AbstractTestWithJdbcTables {
    fun initJdbcTables(dataSource: DataSource){
        dataSource.getConnection().use { conn ->
            ScriptUtils.executeSqlScript(conn, ClassPathResource("sql/prosessering_jdbc.sql"))
        }
    }
}