package no.nav.familie.ba.sak.common

import java.sql.Date
import java.time.YearMonth
import javax.persistence.AttributeConverter

class YearMonthConverter : AttributeConverter<YearMonth, Date> {

    override fun convertToDatabaseColumn(yearMonth: YearMonth?): Date? {
        return yearMonth?.let {
            Date.valueOf(it.toLocalDate())
        }
    }

    override fun convertToEntityAttribute(date: Date?): YearMonth? {
        return date?.toLocalDate()?.toYearMonth()
    }
}
