package no.nav.familie.ba.sak.kjerne.personident

import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PersonidentService(
    private val personidentRepository: PersonidentRepository,
    private val personopplysningerService: PersonopplysningerService
) {
    fun hentOgLagreAktivIdentMedAktørId(fødselsnummer: String): String {
        val identerFraPdl = personopplysningerService.hentIdenter(fødselsnummer, false)

        val aktørId: String =
            identerFraPdl.singleOrNull { it.gruppe == IdentGruppe.AKTOERID.name }?.ident
                ?: throw Error("Finner ikke aktørId i Pdl")

        val fødselsnummerAktiv = identerFraPdl.singleOrNull { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT.name }?.ident
            ?: throw Error("Finner ikke aktiv ident i Pdl")

        val aktivIdentPersistert = personidentRepository.hentAktivIdentForAktørId(aktørId)

        if (aktivIdentPersistert == null) {
            personidentRepository.save(Personident(aktørId = aktørId, fødselsnummer = fødselsnummerAktiv, aktiv = true))
        } else if (aktivIdentPersistert.fødselsnummer != fødselsnummerAktiv) {
            personidentRepository.save(
                aktivIdentPersistert.also {
                    it.aktiv = false
                    it.gjelderTil = LocalDateTime.now()
                }
            )
            personidentRepository.save(Personident(aktørId = aktørId, fødselsnummer = fødselsnummerAktiv, aktiv = true))
        }
        return fødselsnummerAktiv
    }

    fun hentOgLagreAktiveIdenterMedAktørId(barnasFødselsnummer: List<String>): List<String> {
        return barnasFødselsnummer.map { hentOgLagreAktivIdentMedAktørId(it) }
    }

    companion object {
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
