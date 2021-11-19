package no.nav.familie.ba.sak.kjerne.personident

import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.kjerne.aktørid.AktørId
import no.nav.familie.ba.sak.kjerne.aktørid.AktørIdRepository
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class PersonidentService(
    private val personidentRepository: PersonidentRepository,
    private val aktørIdRepository: AktørIdRepository,
    private val personopplysningerService: PersonopplysningerService
) {
    fun hentOgLagreAktørId(fødselsnummer: String): AktørId =
        personidentRepository.findByIdOrNull(fødselsnummer)?.let { it.aktørId }
            ?: kotlin.run {
                val identerFraPdl = personopplysningerService.hentIdenter(fødselsnummer, false)
                val aktørIdStr = filtrerAktørId(identerFraPdl)
                val fødselsnummerAktiv = filtrerAktivtFødselsnummer(identerFraPdl)

                return aktørIdRepository.findByIdOrNull(aktørIdStr)?.let { opprettPersonIdent(it, fødselsnummerAktiv) }
                    ?: opprettAktørIdOgPersonident(aktørIdStr, fødselsnummerAktiv)
            }

    private fun opprettAktørIdOgPersonident(aktørIdStr: String, fødselsnummer: String): AktørId =
        aktørIdRepository.save(
            AktørId(aktørId = aktørIdStr).also {
                it.personidenter.add(
                    Personident(fødselsnummer = fødselsnummer, aktørId = it)
                )
            }
        )

    private fun opprettPersonIdent(aktørId: AktørId, fødselsnummer: String): AktørId {
        aktørId.personidenter.filter { it.aktiv }.forEach {
            it.aktiv = false
            personidentRepository.save(it)
        }

        return personidentRepository.save(
            Personident(
                fødselsnummer = fødselsnummer,
                aktørId = aktørId
            )
        ).aktørId
    }

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

    fun hentOgLagreAktørIder(barnasFødselsnummer: List<String>): List<AktørId> {
        return barnasFødselsnummer.map { hentOgLagreAktørId(it) }
    }

    companion object {
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
