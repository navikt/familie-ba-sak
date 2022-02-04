package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegisteropplysning
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlKontaktinformasjonForDødsboAdresse
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Dødsfall")
@Table(name = "po_doedsfall")
data class Dødsfall(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_doedsfall_seq_generator")
    @SequenceGenerator(name = "po_doedsfall_seq_generator", sequenceName = "po_doedsfall_seq", allocationSize = 50)
    val id: Long = 0,

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "fk_po_person_id", referencedColumnName = "id", nullable = false)
    val person: Person,

    @Column(name = "doedsfall_dato", nullable = false)
    val dødsfallDato: LocalDate,

    @Column(name = "doedsfall_adresse", nullable = true)
    val dødsfallAdresse: String?,

    @Column(name = "doedsfall_postnummer", nullable = true)
    val dødsfallPostnummer: String?,

    @Column(name = "doedsfall_poststed", nullable = true)
    val dødsfallPoststed: String?,
) : BaseEntitet() {
    fun hentAdresseToString(): String { return """$dødsfallAdresse, $dødsfallPostnummer $dødsfallPoststed""" }

    fun tilRestRegisteropplysning() = RestRegisteropplysning(
        fom = this.dødsfallDato,
        tom = null,
        verdi = if (dødsfallAdresse == null) "-" else hentAdresseToString()
    )
}

fun lagDødsfall(person: Person, dødsfallDatoFraPdl: String?, dødsfallAdresseFraPdl: PdlKontaktinformasjonForDødsboAdresse?): Dødsfall? {
    if (dødsfallDatoFraPdl == null || dødsfallDatoFraPdl == "") {
        return null
    }
    return Dødsfall(
        person = person,
        dødsfallDato = LocalDate.parse(dødsfallDatoFraPdl),
        dødsfallAdresse = dødsfallAdresseFraPdl?.adresselinje1,
        dødsfallPostnummer = dødsfallAdresseFraPdl?.postnummer,
        dødsfallPoststed = dødsfallAdresseFraPdl?.poststedsnavn
    )
}
