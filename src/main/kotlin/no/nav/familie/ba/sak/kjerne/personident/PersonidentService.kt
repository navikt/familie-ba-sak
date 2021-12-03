package no.nav.familie.ba.sak.kjerne.personident

import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PersonidentService(
    private val personidentRepository: PersonidentRepository,
    private val aktørIdRepository: AktørIdRepository,
    private val personopplysningerService: PersonopplysningerService
) {
    // TODO: robustgjøring dnr/fnr skriv test for endringen i logikken i denne metoden. 
    fun hentAlleFødselsnummerForEnAktør(aktør: Aktør) =
        personopplysningerService.hentIdenter(Ident(aktør.aktivIdent())).filter { it.gruppe == "FOLKEREGISTERIDENT" }
            .map { it.ident }

    fun hentOgLagreAktør(ident: String): Aktør =
        aktørIdRepository.findByIdOrNull(ident)
            ?: personidentRepository.findByIdOrNull(ident)?.aktør
            ?: kotlin.run {
                val identerFraPdl = personopplysningerService.hentIdenter(ident, false)
                val aktørIdStr = filtrerAktørId(identerFraPdl)
                val fødselsnummerAktiv = filtrerAktivtFødselsnummer(identerFraPdl)

                return aktørIdRepository.findByIdOrNull(aktørIdStr)?.let { opprettPersonIdent(it, fødselsnummerAktiv) }
                    ?: opprettAktørIdOgPersonident(aktørIdStr, fødselsnummerAktiv)
            }

    fun hentOgLagreAktørIder(barnasFødselsnummer: List<String>): List<Aktør> {
        return barnasFødselsnummer.map { hentOgLagreAktør(it) }
    }

    fun hentGjeldendeFødselsnummerForTidspunkt(aktør: Aktør, tidspunkt: LocalDateTime): String {
        val alleIdenter = personidentRepository.hentAlleIdenterForAktørid(aktør.aktørId)
        if (alleIdenter.size == 1) return alleIdenter.first().fødselsnummer
        return alleIdenter.filter { it.gjelderTil?.isAfter(tidspunkt) ?: false }
            .minByOrNull { it.gjelderTil!! }?.fødselsnummer
            ?: alleIdenter.first { it.aktiv }.fødselsnummer
    }

    private fun opprettAktørIdOgPersonident(aktørIdStr: String, fødselsnummer: String): Aktør =
        aktørIdRepository.save(
            Aktør(aktørId = aktørIdStr).also {
                it.personidenter.add(
                    Personident(fødselsnummer = fødselsnummer, aktør = it)
                )
            }
        )

    private fun opprettPersonIdent(aktør: Aktør, fødselsnummer: String): Aktør {
        aktør.personidenter.filter { it.aktiv }.forEach {
            it.aktiv = false
            personidentRepository.save(it)
        }

        return personidentRepository.save(
            Personident(
                fødselsnummer = fødselsnummer,
                aktør = aktør
            )
        ).aktør
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

    companion object {
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
