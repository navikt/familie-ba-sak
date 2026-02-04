package no.nav.familie.ba.sak.kjerne.falskidentitet

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.FalskIdentitetPersonInfo
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresser
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.springframework.stereotype.Service

@Service
class FalskIdentitetService(
    val personRepository: PersonRepository,
    val pdlRestKlient: PdlRestKlient,
    val featureToggleService: FeatureToggleService,
) {
    fun hentFalskIdentitet(aktør: Aktør): FalskIdentitetPersonInfo? {
        if (harFalskIdentitet(aktør)) {
            validerTilgangTilHåndteringAvFalskIdentitet()
            val personer = personRepository.findByAktør(aktør)
            val person = personer.firstOrNull { it.personopplysningGrunnlag.aktiv } ?: personer.firstOrNull()

            // Vurdere å bruke mer informasjon fra PdlFalskIdentitet hvis tilgjengelig
            // Henter navn, fødselsdato, kjønn og adresser fra personopplysningene i stedet for PDL
            return FalskIdentitetPersonInfo(
                navn = person?.navn ?: "Ukjent navn",
                fødselsdato = person?.fødselsdato,
                kjønn = person?.kjønn ?: Kjønn.UKJENT,
                adresser = person?.let { Adresser.opprettFra(it) },
            )
        } else {
            return null
        }
    }

    fun harFalskIdentitet(aktør: Aktør): Boolean = pdlRestKlient.hentFalskIdentitet(aktør.aktivFødselsnummer())?.erFalsk ?: false

    private fun validerTilgangTilHåndteringAvFalskIdentitet() {
        if (!featureToggleService.isEnabled(FeatureToggle.SKAL_HÅNDTERE_FALSK_IDENTITET)) {
            throw FunksjonellFeil(KAN_IKKE_HÅNDTERE_FALSK_IDENTITET)
        }
    }

    companion object {
        const val KAN_IKKE_HÅNDTERE_FALSK_IDENTITET = "Feature toggle for håndtering av falsk identitet ('SKAL_HÅNDTERE_FALSK_IDENTITET') er deaktivert, eller e-posten din er ikke lagt inn i listen over e-poster som har tilgang til å håndtere falske identiteter."
    }
}
