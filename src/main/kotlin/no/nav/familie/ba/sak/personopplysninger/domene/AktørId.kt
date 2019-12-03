package no.nav.familie.ba.sak.personopplysninger.domene

import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.util.*
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.validation.constraints.Pattern

/**
 * Id som genereres fra NAV Aktør Register. Denne iden benyttes til interne forhold i Nav og vil ikke endres f.eks. dersom bruker går fra
 * DNR til FNR i Folkeregisteret. Tilsvarende vil den kunne referere personer som har ident fra et utenlandsk system.
 */
@Embeddable
class AktørId : Serializable, Comparable<AktørId> {
    @JsonValue
    @Column(name = "aktoer_id", updatable = false, length = 50)
    @Pattern(regexp = VALID_REGEXP, flags = [Pattern.Flag.CASE_INSENSITIVE])
    var id : String? = null
    protected constructor() { // for hibernate
    }

    constructor(aktørId: Long) {
        Objects.requireNonNull(aktørId, "aktørId")
        id = aktørId.toString()
    }

    constructor(aktørId: String) {
        Objects.requireNonNull(aktørId, "aktørId")
        require(VALID.matcher(aktørId).matches()) { // skal ikke skje, funksjonelle feilmeldinger håndteres ikke her.
            "Ugyldig aktørId, støtter kun A-Z/0-9/:/-/_ tegn.)"
        }
        id = aktørId
    }

    override fun equals(obj: Any?): Boolean {
        if (obj === this) {
            return true
        } else if (obj == null || javaClass != obj.javaClass) {
            return false
        }
        val other = obj as AktørId
        return id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }

    override fun toString(): String {
        return javaClass.simpleName + "<" + id + ">"
    }

    override fun compareTo(o: AktørId): Int { // TODO: Burde ikke finnes
        return id!!.compareTo(o.id!!)
    }

    companion object {
        private const val CHARS = "a-z0-9_:-"
        private const val VALID_REGEXP = "^(-?[1-9]|[a-z0])[$CHARS]*$"
        private const val INVALID_REGEXP = "[^$CHARS]+"
        private val VALID =
                java.util.regex.Pattern.compile(VALID_REGEXP, java.util.regex.Pattern.CASE_INSENSITIVE)
        private val INVALID = java.util.regex.Pattern.compile(INVALID_REGEXP,
                                                              java.util.regex.Pattern.DOTALL or java.util.regex.Pattern.CASE_INSENSITIVE)
    }
}