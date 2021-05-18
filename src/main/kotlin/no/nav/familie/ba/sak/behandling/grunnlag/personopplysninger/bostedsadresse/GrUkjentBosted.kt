package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.bostedsadresse

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted
import javax.persistence.Column
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrUkjentBosted")
@DiscriminatorValue("ukjentBosted")
data class GrUkjentBosted(
        @Column(name = "bostedskommune")
        val bostedskommune: String,



        @JsonIgnore
        @ManyToOne(optional = false)
        @JoinColumn(name = "fk_po_person_id", nullable = false, updatable = false)
        override val person: Person,

        ) : GrBostedsadresse() {

    fun toSecureString(): String {
        return """UkjentadresseDao(bostedskommune=$bostedskommune""".trimMargin()
    }

    override fun toString(): String {
        return "UkjentBostedAdresse(detaljer skjult)"
    }

    companion object {
        fun fraUkjentBosted(ukjentBosted: UkjentBosted): GrUkjentBosted =
                GrUkjentBosted(bostedskommune = ukjentBosted.bostedskommune)
    }
}
