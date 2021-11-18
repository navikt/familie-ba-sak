package no.nav.familie.ba.sak.kjerne.aktørid

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.personident.Personident
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
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
@Entity(name = "AktørId")
@Table(name = "AKTOER_ID")
data class AktørId(
    @Id
    @Column(name = "aktoer_id", updatable = false, length = 50)
    @Pattern(regexp = VALID_REGEXP, flags = [Pattern.Flag.CASE_INSENSITIVE])
    val aktørId: String,

    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "aktørId",
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

    fun aktivIdent() = personidenter.single { it.aktiv }

    companion object {
        private const val CHARS = "a-z0-9_:-"
        private const val VALID_REGEXP = "^(-?[1-9]|[a-z0])[$CHARS]*$"
        private val VALID = java.util.regex.Pattern.compile(VALID_REGEXP, java.util.regex.Pattern.CASE_INSENSITIVE)
    }
}
