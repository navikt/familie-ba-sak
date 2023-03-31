package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingId
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingIdConverter
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity
@Table(name = "GR_PERSONOPPLYSNINGER")
data class PersonopplysningGrunnlag(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GR_PERSONOPPLYSNINGER_SEQ_GENERATOR")
    @SequenceGenerator(
        name = "GR_PERSONOPPLYSNINGER_SEQ_GENERATOR",
        sequenceName = "GR_PERSONOPPLYSNINGER_SEQ",
        allocationSize = 50
    )
    val id: Long = 0,

    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    @Convert(converter = BehandlingIdConverter::class)
    val behandlingId: BehandlingId,

    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "personopplysningGrunnlag",
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH]
    )
    val personer: MutableSet<Person> = mutableSetOf(),

    @Column(name = "aktiv", nullable = false)
    var aktiv: Boolean = true

) : BaseEntitet() {

    val barna: List<Person>
        get() = personer.filter { it.type == PersonType.BARN }

    val yngsteBarnSinFødselsdato: LocalDate
        get() = barna.maxOf { it.fødselsdato }

    val søker: Person
        get() = personer.singleOrNull { it.type == PersonType.SØKER }
            // Vil returnere barnet på EM-saker, som da i prinsippet også er søkeren. Vil også returnere barnet på inst. saker
            ?: personer.singleOrNull()?.takeIf { it.type == PersonType.BARN }
            ?: error("Persongrunnlag mangler søker eller det finnes flere personer i grunnlaget med type=SØKER")

    val annenForelder: Person?
        get() = personer.singleOrNull { it.type == PersonType.ANNENPART }

    val søkerOgBarn: List<Person>
        get() = personer.filter { it.type == PersonType.SØKER || it.type == PersonType.BARN }

    fun harBarnMedSeksårsdagPåFom(fom: LocalDate?) = personer.any { person ->
        person
            .hentSeksårsdag()
            .toYearMonth() == (fom?.toYearMonth() ?: TIDENES_ENDE.toYearMonth())
    }

    override fun toString(): String {
        val sb = StringBuilder("PersonopplysningGrunnlagEntitet{")
        sb.append("id=").append(id)
        sb.append(", personer=").append(personer.toString())
        sb.append(", aktiv=").append(aktiv)
        sb.append('}')
        return sb.toString()
    }
}
