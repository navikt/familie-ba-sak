package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.kjerne.personident.Aktør
import java.time.LocalDate

data class PersonPåBehandling(
    val type: PersonType,
    val aktør: Aktør,
    val fødselsdato: LocalDate,
    val dødsfallDato: LocalDate?,
    val målform: Målform,
)

// Vil returnere barnet på EM-saker, som da i prinsippet også er søkeren. Vil også returnere barnet på inst. saker
fun Collection<PersonPåBehandling>.søker() = this.singleOrNull { it.type == PersonType.SØKER }
    ?: this.singleOrNull()?.takeIf { it.type == PersonType.BARN }
    ?: error("Persongrunnlag mangler søker eller det finnes flere personer i grunnlaget med type=SØKER")

fun Collection<PersonPåBehandling>.barn(): List<PersonPåBehandling> = this.filter { it.type == PersonType.BARN }
