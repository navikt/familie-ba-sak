package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene

import com.fasterxml.jackson.annotation.JsonValue
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.validation.constraints.Pattern

/**
 * Id som genereres fra NAV Aktør Register. Denne iden benyttes til interne forhold i Nav og vil ikke endres f.eks. dersom bruker
 * går fra DNR til FNR i Folkeregisteret. Tilsvarende vil den kunne referere personer som har ident fra et utenlandsk system.
 */
@Embeddable
data class AktørId(
        @JsonValue
        @Column(name = "aktoer_id", updatable = false, length = 50)
        @Pattern(regexp = VALID_REGEXP, flags = [Pattern.Flag.CASE_INSENSITIVE])
        val id: String
) {

    init {
        require(VALID.matcher(id).matches()) {
            // skal ikke skje, funksjonelle feilmeldinger håndteres ikke her.
            "Ugyldig aktørId, støtter kun A-Z/0-9/:/-/_ tegn.)"
        }
    }

    companion object {
        private const val CHARS = "a-z0-9_:-"
        private const val VALID_REGEXP = "^(-?[1-9]|[a-z0])[$CHARS]*$"
        private val VALID = java.util.regex.Pattern.compile(VALID_REGEXP, java.util.regex.Pattern.CASE_INSENSITIVE)
    }
}
