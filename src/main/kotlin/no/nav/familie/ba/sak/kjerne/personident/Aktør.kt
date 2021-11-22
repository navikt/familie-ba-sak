package no.nav.familie.ba.sak.kjerne.personident

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.util.Objects
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.validation.constraints.Pattern

/**
 * Id som genereres fra NAV Aktør Register. Denne iden benyttes til interne forhold i Nav og vil ikke endres f.eks. dersom bruker
 * går fra DNR til FNR i Folkeregisteret. Tilsvarende vil den kunne referere personer som har ident fra et utenlandsk system.
 */
@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Aktør")
@Table(name = "AKTOER")
data class Aktør(
    @Id
    @Column(name = "aktoer_id", updatable = false, length = 50)
    @Pattern(regexp = VALID_REGEXP, flags = [Pattern.Flag.CASE_INSENSITIVE])
    // Er ikke kalt id ettersom den refererer til en ekstern id.
    val aktørId: String,

    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "aktør",
        cascade = [CascadeType.ALL]
    )
    val personidenter: MutableSet<Personident> = mutableSetOf()
) : BaseEntitet() {

    init {
        require(VALID.matcher(aktørId).matches()) {
            // skal ikke skje, funksjonelle feilmeldinger håndteres ikke her.
            "Ugyldig aktørId, støtter kun A-Z/0-9/:/-/_ tegn.)"
        }
    }

    override fun toString(): String {
        return """aktørId=$aktørId""".trimMargin()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val entitet: Aktør = other as Aktør
        return Objects.equals(hashCode(), entitet.hashCode())
    }

    override fun hashCode(): Int {
        return Objects.hash(aktørId)
    }

    fun aktivIdent() = personidenter.single { it.aktiv }

    companion object {
        private const val CHARS = "a-z0-9_:-"
        private const val VALID_REGEXP = "^(-?[1-9]|[a-z0])[$CHARS]*$"
        private val VALID = java.util.regex.Pattern.compile(VALID_REGEXP, java.util.regex.Pattern.CASE_INSENSITIVE)
    }
}
