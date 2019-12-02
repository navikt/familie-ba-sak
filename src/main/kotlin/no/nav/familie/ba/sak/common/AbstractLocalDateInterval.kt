package no.nav.familie.ba.sak.common

import java.io.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.chrono.ChronoLocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

abstract class AbstractLocalDateInterval : Comparable<AbstractLocalDateInterval>, Serializable {
    companion object {
        var TIDENES_ENDE: LocalDate? = null
        var FORMATTER: DateTimeFormatter? = null
        private var TIDENES_BEGYNNELSE: LocalDate? = null
        protected fun finnTomDato(fom: LocalDate, antallArbeidsdager: Int): LocalDate {
            return if (antallArbeidsdager < 1) {
                throw IllegalArgumentException("Antall arbeidsdager må være 1 eller større.")
            } else {
                var tom = fom
                var antallArbeidsdagerTmp = antallArbeidsdager
                while (antallArbeidsdagerTmp > 0) {
                    require(antallArbeidsdagerTmp <= antallArbeidsdager) { "Antall arbeidsdager beregnes feil." }
                    if (erArbeidsdag(tom)) {
                        --antallArbeidsdagerTmp
                    }
                    if (antallArbeidsdagerTmp > 0) {
                        tom = tom.plusDays(1L)
                    }
                }
                tom
            }
        }

        protected fun finnFomDato(tom: LocalDate, antallArbeidsdager: Int): LocalDate {
            return if (antallArbeidsdager < 1) {
                throw IllegalArgumentException("Antall arbeidsdager må være 1 eller større.")
            } else {
                var fom = tom
                var antallArbeidsdagerTmp = antallArbeidsdager
                while (antallArbeidsdagerTmp > 0) {
                    require(antallArbeidsdagerTmp <= antallArbeidsdager) { "Antall arbeidsdager beregnes feil." }
                    if (erArbeidsdag(fom)) {
                        --antallArbeidsdagerTmp
                    }
                    if (antallArbeidsdagerTmp > 0) {
                        fom = fom.minusDays(1L)
                    }
                }
                fom
            }
        }

        fun forrigeArbeidsdag(dato: LocalDate?): LocalDate? {
            if (dato !== TIDENES_BEGYNNELSE && dato !== TIDENES_ENDE) {
                when (dato!!.dayOfWeek) {
                    DayOfWeek.SATURDAY -> return dato.minusDays(1L)
                    DayOfWeek.SUNDAY -> return dato.minusDays(2L)
                }
            }
            return dato
        }

        fun nesteArbeidsdag(dato: LocalDate?): LocalDate? {
            if (dato !== TIDENES_BEGYNNELSE && dato !== TIDENES_ENDE) {
                when (dato!!.dayOfWeek) {
                    DayOfWeek.SATURDAY -> return dato.plusDays(2L)
                    DayOfWeek.SUNDAY -> return dato.plusDays(1L)
                }
            }
            return dato
        }

        private fun listArbeidsdager(fomDato: LocalDate?, tomDato: LocalDate?): List<LocalDate?> {
            val arbeidsdager: MutableList<LocalDate?> = ArrayList()
            var dato = fomDato
            while (!dato!!.isAfter(tomDato)) {
                if (erArbeidsdag(dato)) {
                    arbeidsdager.add(dato)
                }
                dato = dato.plusDays(1L)
            }
            return arbeidsdager
        }

        protected fun erArbeidsdag(dato: LocalDate?): Boolean {
            return dato!!.dayOfWeek != DayOfWeek.SATURDAY && dato.dayOfWeek != DayOfWeek.SUNDAY
        }

        init {
            TIDENES_BEGYNNELSE = Tid.TIDENES_BEGYNNELSE
            TIDENES_ENDE = Tid.TIDENES_ENDE
            FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        }
    }

    abstract val fomDato: LocalDate?
    abstract val tomDato: LocalDate?
    protected abstract fun lagNyPeriode(var1: LocalDate?, var2: LocalDate?): AbstractLocalDateInterval
    fun erFørEllerLikPeriodeslutt(dato: ChronoLocalDate?): Boolean {
        return tomDato == null || tomDato!!.isAfter(dato) || tomDato!!.isEqual(dato)
    }

    fun erEtterEllerLikPeriodestart(dato: ChronoLocalDate?): Boolean {
        return fomDato!!.isBefore(dato) || fomDato!!.isEqual(dato)
    }

    fun inkluderer(dato: ChronoLocalDate?): Boolean {
        return erEtterEllerLikPeriodestart(dato) && erFørEllerLikPeriodeslutt(dato)
    }

    fun inkludererArbeidsdag(dato: LocalDate?): Boolean {
        return erEtterEllerLikPeriodestart(nesteArbeidsdag(dato)) && erFørEllerLikPeriodeslutt(
                forrigeArbeidsdag(dato))
    }

    fun antallDager(): Long {
        return ChronoUnit.DAYS.between(fomDato, tomDato)
    }

    fun antallArbeidsdager(): Int {
        return if (tomDato!!.isEqual(TIDENES_ENDE)) {
            throw IllegalStateException("Både fra og med og til og med dato må være satt for å regne ut arbeidsdager.")
        } else {
            arbeidsdager().size
        }
    }

    fun maksAntallArbeidsdager(): Int {
        return if (tomDato!!.isEqual(TIDENES_ENDE)) {
            throw IllegalStateException("Både fra og med og til og med dato må være satt for å regne ut arbeidsdager.")
        } else {
            val månedsstart = fomDato!!.minusDays(fomDato!!.dayOfMonth.toLong() - 1L)
            val månedsslutt = tomDato!!.minusDays(tomDato!!.dayOfMonth.toLong() - 1L)
                    .plusDays(tomDato!!.lengthOfMonth().toLong() - 1L)
            listArbeidsdager(månedsstart, månedsslutt).size
        }
    }

    fun arbeidsdager(): List<LocalDate?> {
        return listArbeidsdager(fomDato, tomDato)
    }

    fun grenserTil(periode2: AbstractLocalDateInterval): Boolean {
        return tomDato == periode2.fomDato!!.minusDays(1L) || periode2.tomDato == fomDato!!.minusDays(
                1L)
    }

    fun splittVedMånedsgrenser(): List<AbstractLocalDateInterval?> {
        val perioder: MutableList<AbstractLocalDateInterval?> = ArrayList()
        var dato = fomDato!!.minusDays(fomDato!!.dayOfMonth.toLong() - 1L)
        var periodeFomDato = fomDato
        while (dato!!.isBefore(tomDato)) {
            val dagerIMåned = dato.lengthOfMonth()
            val sisteDagIMåneden = dato.plusDays(dagerIMåned.toLong() - 1L)
            val harMånedsslutt = inkluderer(sisteDagIMåneden)
            if (harMånedsslutt) {
                perioder.add(lagNyPeriode(periodeFomDato, sisteDagIMåneden))
                dato = sisteDagIMåneden.plusDays(1L)
                periodeFomDato = dato
            } else {
                perioder.add(lagNyPeriode(periodeFomDato, tomDato))
                dato = tomDato
            }
        }
        return perioder
    }

    fun finnMånedeskvantum(): Double {
        val perioder: Collection<AbstractLocalDateInterval?> = splittVedMånedsgrenser()
        var kvantum = 0.0
        val var4: Iterator<*> = perioder.iterator()
        while (var4.hasNext()) {
            val periode = var4.next() as AbstractLocalDateInterval
            val antallArbeidsdager = periode.antallArbeidsdager()
            if (antallArbeidsdager != 0) {
                val diff = periode.maksAntallArbeidsdager() - antallArbeidsdager
                kvantum += if (diff == 0) 1.0 else diff.toDouble() / periode.maksAntallArbeidsdager().toDouble()
            }
        }
        return kvantum
    }

    fun splittPeriodePåDatoer(vararg datoer: LocalDate?): List<AbstractLocalDateInterval?> {
        val datoListe = Arrays.asList<LocalDate>(*datoer)
        datoListe.sort()
        val perioder: MutableList<AbstractLocalDateInterval?> = ArrayList()
        var periode = this
        val var5: Iterator<*> = datoListe.iterator()
        while (var5.hasNext()) {
            val dato = var5.next() as LocalDate
            if (periode.inkluderer(dato) && dato.isAfter(periode.fomDato)) {
                perioder.add(lagNyPeriode(periode.fomDato, dato.minusDays(1L)))
                periode = lagNyPeriode(dato, periode.tomDato)
            }
        }
        perioder.add(periode)
        return perioder
    }

    fun splittPeriodePåDatoerAvgrensTilArbeidsdager(vararg datoer: LocalDate?): List<AbstractLocalDateInterval?> {
        val datoListe = Arrays.asList<LocalDate>(*datoer)
        datoListe.sort()
        val perioder: MutableList<AbstractLocalDateInterval?> = ArrayList()
        var periode = avgrensTilArbeidsdager()
        val var5: Iterator<*> = datoListe.iterator()
        while (var5.hasNext()) {
            val dato = var5.next() as LocalDate
            if (periode.inkluderer(dato) && dato.isAfter(periode.fomDato)) {
                perioder.add(lagNyPeriode(periode.fomDato, dato.minusDays(1L)).avgrensTilArbeidsdager())
                periode = lagNyPeriode(dato, periode.tomDato).avgrensTilArbeidsdager()
            }
        }
        perioder.add(periode)
        return perioder
    }

    fun avgrensTilArbeidsdager(): AbstractLocalDateInterval {
        val nyFomDato = nesteArbeidsdag(fomDato)
        val nyTomDato = forrigeArbeidsdag(tomDato)
        return if (nyFomDato == fomDato && nyTomDato == tomDato) this else lagNyPeriode(nyFomDato,
                                                                                        nyTomDato)
    }

    fun kuttPeriodePåGrenseneTil(periode: AbstractLocalDateInterval): AbstractLocalDateInterval {
        val nyFomDato =
                if (fomDato!!.isBefore(periode.fomDato)) periode.fomDato else fomDato
        val nyTomDato =
                if (tomDato!!.isAfter(periode.tomDato)) periode.tomDato else tomDato
        return lagNyPeriode(nyFomDato, nyTomDato)
    }

    override fun compareTo(periode: AbstractLocalDateInterval): Int {
        return fomDato!!.compareTo(periode.fomDato)
    }

    override fun equals(`object`: Any?): Boolean {
        return if (`object` === this) {
            true
        } else if (`object` !is AbstractLocalDateInterval) {
            false
        } else {
            val annen = `object`
            likFom(annen) && likTom(annen)
        }
    }

    private fun likFom(annen: AbstractLocalDateInterval): Boolean {
        val likFom = fomDato == annen.fomDato
        return if (fomDato != null && annen.fomDato != null) {
            likFom || nesteArbeidsdag(fomDato) == nesteArbeidsdag(annen.fomDato)
        } else {
            likFom
        }
    }

    private fun likTom(annen: AbstractLocalDateInterval): Boolean {
        val likTom = tomDato == annen.tomDato
        return if (tomDato != null && annen.tomDato != null) {
            likTom || forrigeArbeidsdag(tomDato) == forrigeArbeidsdag(annen.tomDato)
        } else {
            likTom
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(*arrayOf<Any?>(fomDato, tomDato))
    }

    override fun toString(): String {
        return String.format("Periode: %s - %s",
                             fomDato!!.format(FORMATTER),
                             tomDato!!.format(FORMATTER))
    }
}