package no.nav.familie.ba.sak.kjerne.personident

import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.AktørId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PersonidentService(
    private val personidentRepository: PersonidentRepository,
    private val personopplysningerService: PersonopplysningerService
) {
    fun hentOgLagreAktivIdentMedAktørId(fødselsnummer: String): String {
        // TODO: Når lytting på endring av ident meldinger er implementert skal denne metoden returnere hvis 
        // iden allerede er persistert ellers så skal den hente og persistere den aktive identen.
        val identerFraPdl = personopplysningerService.hentIdenter(fødselsnummer, false)

        val aktørId = filtrerAktørId(identerFraPdl)
        val fødselsnummerAktiv = filtrerAktivtFødselsnummer(identerFraPdl)

        val aktivIdentPersistert = personidentRepository.hentAktivIdentForAktørId(aktørId)

        if (aktivIdentErIkkePersistert(aktivIdentPersistert, fødselsnummerAktiv)) {
            personidentRepository.save(
                Personident(
                    aktørId = AktørId(aktørId),
                    fødselsnummer = fødselsnummerAktiv,
                    aktiv = true
                )
            )
        }

        if (identHarBlittInnaktivSidenSistPersistering(aktivIdentPersistert, fødselsnummerAktiv)) {
            personidentRepository.save(
                aktivIdentPersistert!!.also {
                    it.aktiv = false
                    it.gjelderTil = LocalDateTime.now()
                }
            )
        }

        return fødselsnummerAktiv
    }

    private fun identHarBlittInnaktivSidenSistPersistering(
        aktivIdentPersistert: Personident?,
        fødselsnummerAktiv: String
    ) = aktivIdentPersistert != null && aktivIdentPersistert.fødselsnummer != fødselsnummerAktiv

    private fun aktivIdentErIkkePersistert(
        aktivIdentPersistert: Personident?,
        fødselsnummerAktiv: String
    ) = aktivIdentPersistert == null || aktivIdentPersistert.fødselsnummer != fødselsnummerAktiv

    private fun filtrerAktivtFødselsnummer(identerFraPdl: List<IdentInformasjon>) =
        (
            identerFraPdl.singleOrNull { it.gruppe == "FOLKEREGISTERIDENT" }?.ident
                ?: throw Error("Finner ikke aktiv ident i Pdl")
            )

    private fun filtrerAktørId(identerFraPdl: List<IdentInformasjon>) =
        (
            identerFraPdl.singleOrNull { it.gruppe == "AKTORID" }?.ident
                ?: throw Error("Finner ikke aktørId i Pdl")
            )

    fun hentOgLagreAktiveIdenterMedAktørId(barnasFødselsnummer: List<String>): List<String> {
        return barnasFødselsnummer.map { hentOgLagreAktivIdentMedAktørId(it) }
    }

    companion object {
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
