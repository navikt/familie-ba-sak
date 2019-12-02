package no.nav.familie.ba.sak.personopplysninger.domene

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*
import javax.persistence.Column
import javax.persistence.Embeddable

/**
 * Denne mapper p.t Norsk person ident (fødselsnummer, inkl F-nr, D-nr eller FDAT)
 *
 *  * F-nr: http://lovdata.no/forskrift/2007-11-09-1268/%C2%A72-2 (F-nr)
 *
 *  * D-nr: http://lovdata.no/forskrift/2007-11-09-1268/%C2%A72-5 (D-nr), samt hvem som kan utstede
 * (http://lovdata.no/forskrift/2007-11-09-1268/%C2%A72-6)
 *
 *  * FDAT: Personer uten FNR. Disse har fødselsdato + 00000 (normalt) eller fødselsdato + 00001 (dødfødt).
 *
 */
@Embeddable
class PersonIdent : Comparable<PersonIdent> {
    @JsonProperty("id")
    @Column(name = "person_ident", updatable = false, length = 50)
    var ident: String? = null

    constructor() {}
    constructor(ident: String) {
        Objects.requireNonNull(ident, "ident kan ikke være null")
        this.ident = ident
    }

    override fun compareTo(o: PersonIdent): Int {
        return ident!!.compareTo(o.ident!!)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        } else if (other == null || this.javaClass != other.javaClass) {
            return false
        }
        val  otherObject = other as PersonIdent
        return ident == otherObject.ident
    }

    fun erDnr(): Boolean {
        val n = Character.digit(ident!![0], 10)
        return n > 3 && n <= 7
    }

    override fun hashCode(): Int {
        return Objects.hash(ident)
    }

    /**
     * Hvorvidt dette er et Fdat Nummer (dvs. gjelder person uten tildelt fødselsnummer).
     */
    fun erFdatNummer(): Boolean {
        return isFdatNummer(getPersonnummer(ident))
    }

    companion object {
        private val CHECKSUM_EN_VECTOR = intArrayOf(3, 7, 6, 1, 8, 9, 4, 5, 2)
        private val CHECKSUM_TO_VECTOR = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)
        private const val FNR_LENGDE = 11
        private const val PERSONNR_LENGDE = 5
        /**
         * @return true hvis angitt str er et fødselsnummer (F-Nr eller D-Nr). False hvis ikke, eller er FDAT nummer.
         */
        fun erGyldigFnr(str: String?): Boolean {
            if (str == null) {
                return false
            }
            val s = str.trim { it <= ' ' }
            return s.length == FNR_LENGDE && !isFdatNummer(getPersonnummer(
                    s)) && validerFnrStruktur(s)
        }

        private fun getPersonnummer(str: String?): String? {
            return if (str == null || str.length < PERSONNR_LENGDE) null else str.substring(str.length - PERSONNR_LENGDE)
        }

        private fun isFdatNummer(personnummer: String?): Boolean {
            return personnummer != null && personnummer.length == PERSONNR_LENGDE && personnummer.startsWith(
                    "0000")
        }

        private fun sum(foedselsnummer: String, vararg faktors: Int): Int {
            var sum = 0
            var i = 0
            val l = faktors.size
            while (i < l) {
                sum += Character.digit(foedselsnummer[i], 10) * faktors[i]
                ++i
            }
            return sum
        }

        private fun validerFnrStruktur(foedselsnummer: String): Boolean {
            if (foedselsnummer.length != FNR_LENGDE) {
                return false
            }
            var checksumEn = FNR_LENGDE - sum(foedselsnummer,
                                              *CHECKSUM_EN_VECTOR) % FNR_LENGDE
            if (checksumEn == FNR_LENGDE) {
                checksumEn = 0
            }
            var checksumTo = FNR_LENGDE - sum(foedselsnummer,
                                              *CHECKSUM_TO_VECTOR) % FNR_LENGDE
            if (checksumTo == FNR_LENGDE) {
                checksumTo = 0
            }
            return (checksumEn == Character.digit(foedselsnummer[FNR_LENGDE - 2], 10)
                    && checksumTo == Character.digit(foedselsnummer[FNR_LENGDE - 1], 10))
        }

        fun fra(ident: String?): PersonIdent? {
            return ident?.let { PersonIdent(it) }
        }
    }
}