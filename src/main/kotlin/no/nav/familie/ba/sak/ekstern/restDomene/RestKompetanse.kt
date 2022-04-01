package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseStatus
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.SøkersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util.vurderStatus
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import java.time.YearMonth

data class RestKompetanse(
    val id: Long,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val barnIdenter: List<String>,
    val søkersAktivitet: SøkersAktivitet? = null,
    val annenForeldersAktivitet: AnnenForeldersAktivitet? = null,
    val annenForeldersAktivitetsland: String? = null,
    val barnetsBostedsland: String? = null,
    val resultat: KompetanseResultat? = null,
    val status: KompetanseStatus = KompetanseStatus.IKKE_UTFYLT
)

fun Kompetanse.tilRestKompetanse() = RestKompetanse(
    id = this.id,
    fom = this.fom,
    tom = this.tom,
    barnIdenter = this.barnAktører.map { it.aktivFødselsnummer() },
    søkersAktivitet = this.søkersAktivitet,
    annenForeldersAktivitet = this.annenForeldersAktivitet,
    annenForeldersAktivitetsland = this.annenForeldersAktivitetsland,
    barnetsBostedsland = this.barnetsBostedsland,
    resultat = this.resultat,
    status = this.vurderStatus()
)

fun RestKompetanse.tilKompetanse(barnAktører: List<Aktør>) = Kompetanse(
    fom = this.fom,
    tom = this.tom,
    barnAktører = barnAktører.toSet(),
)
