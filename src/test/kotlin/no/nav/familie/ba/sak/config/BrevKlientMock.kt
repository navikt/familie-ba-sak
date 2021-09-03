package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.kjerne.dokument.BrevKlient
import no.nav.familie.ba.sak.kjerne.dokument.domene.NavnTilNedtrekksmeny
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Brev
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
@Profile("mock-brev-klient")
@Primary
class BrevKlientMock : BrevKlient(
        familieBrevUri = "brev_uri_mock",
        restTemplate = RestTemplate()
) {

    override fun genererBrev(målform: String, brev: Brev): ByteArray {
        return TEST_PDF
    }

    override fun hentNavnTilNedtrekksmeny(): List<NavnTilNedtrekksmeny> {
        return navnTilNedtrekksmenyMock
    }
}

val navnTilNedtrekksmenyMock: List<NavnTilNedtrekksmeny> = listOf(
        NavnTilNedtrekksmeny(
                apiNavn = "avslagIkkeAvtaleOmDeltBosted",
                navnISystem = "Ikke avtale om delt bosted"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "avslagLovligOppholdEosBorger",
                navnISystem = "EØS-borger uten oppholdsrett"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "avslagUregistrertBarn",
                navnISystem = "Barn uten fødselsnummer"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "innvilgelseEnighetOmAtAvtalenOmDeltBostedErOpphort",
                navnISystem = "Enighet om at avtalen om delt bosted er opphørt"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "opphorFlyttetFraNorge",
                navnISystem = "Flyttet fra Norge"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "fortsattInnvilgetMedlemIFolketrygden",
                navnISystem = "Medlem i folketrygden"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "opphorFlereBarnErDode",
                navnISystem = "Flere barn er døde"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "avslagSaerkullsbarn",
                navnISystem = "Ektefelle eller samboers særkullsbarn"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "opphorDeltBostedOpphortUenighet",
                navnISystem = "Uenighet om opphør av avtale om delt bosted"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "fortsattInnvilgetFastOmsorg",
                navnISystem = "Fortsatt fast omsorg for barn"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "opphorBarnBorIkkeMedSoker",
                navnISystem = "Barn bor ikke med søker"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "avslagOmsorgForBarn",
                navnISystem = "Adopsjon, surrogati, beredskapshjem, vurdering av fast bosted\t"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "fortsattInnvilgetBorMedSoker",
                navnISystem = "Barn bosatt med søker"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "fortsattInnvilgetBarnOgSokerLovligOppholdOppholdstillatelse",
                navnISystem = "Tredjelandsborger søker og barn fortsatt lovlig opphold i Norge"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "fortsattInnvilgetDeltBostedPraktiseresFortsatt",
                navnISystem = "Delt bosted praktiseres fortsatt"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "innvilgelseUenighetOmOpphorAvAvtaleOmDeltBosted",
                navnISystem = "Uenighet om opphør av avtale om delt bosted"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "avslagMedlemIFolketrygden",
                navnISystem = "Unntatt medlemskap i Folketrygden"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "fortsattInnvilgetBarnLovligOppholdOppholdstillatelse",
                navnISystem = "Tredjelandsborger barn fortsatt lovlig opphold i Norge"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "innvilgetFodselshendelseNyfodtBarnForste",
                navnISystem = "Nyfødt barn - første barn"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "opphorUnder18Aar",
                navnISystem = "Barn 18 år"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "avslagUnder18Aar",
                navnISystem = "Barn over 18 år"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "opphorSokerHarIkkeFastOmsorg",
                navnISystem = "Søker har ikke lenger fast omsorg for barn (beredskapshjem, vurdering av fast bosted)"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "avslagForeldreneBorSammen",
                navnISystem = "Foreldrene bor sammen"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "fortsattInnvilgetLovligOppholdEOS",
                navnISystem = "EØS-borger: Søker har oppholdsrett"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "fortsattInnvilgetLovligOppholdTredjelandsborger",
                navnISystem = "Tålt opphold tredjelandsborger"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "avslagUgyldigAvtaleOmDeltBosted",
                navnISystem = "Ugyldig avtale om delt bosted"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "opphorHarIkkeOppholdstillatelse",
                navnISystem = "Oppholdstillatelse utløpt\t"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "avslagLovligOppholdTredjelandsborger",
                navnISystem = "Tredjelandsborger uten lovlig opphold i Norge"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "innvilgetSatsendring",
                navnISystem = "Autotekst ved satsendring\t"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "opphorIkkeMottattOpplysninger",
                navnISystem = "Du ikke har sendt oss de opplysningene vi ba om.  "
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "innvilgetOmsorgForBarn",
                navnISystem = "Adopsjon, surrogati: Omsorgen for barn"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "opphorDeltBostedOpphortEnighet",
                navnISystem = "Enighet om opphør av avtale om delt bosted"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "opphorEndretMottaker",
                navnISystem = "Foreldrene bor sammen, endret mottaker\t"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "fortsattInnvilgetSokerBosattIRiket",
                navnISystem = "Søker oppholder seg i Norge"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "reduksjonUnder18Aar",
                navnISystem = "Barn 18 år"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "reduksjonUnder6Aar",
                navnISystem = "Barn 6 år"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "reduksjonBarnDod",
                navnISystem = "Barn død"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "reduksjonBosattIRiket",
                navnISystem = "Barn har flyttet fra Norge"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "reduksjonFlyttetBarn",
                navnISystem = "Barn har flyttet fra søker (flytting mellom foreldre, andre omsorgspersoner)"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "innvilgetBorHosSoker",
                navnISystem = "Barn har flyttet til søker (flytting mellom foreldre, andre omsorgspersoner)"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "reduksjonLovligOppholdOppholdstillatelseBarn",
                navnISystem = "Barn har ikke oppholdstillatelse"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "fortsattInnvilgetSokerLovligOppholdOppholdstillatelse",
                navnISystem = "Tredjelandsborger søker fortsatt lovlig opphold i Norge"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "avslagBorHosSoker",
                navnISystem = "Barn bor ikke med søker\t"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "fortsattInnvilgetUendretTrygd",
                navnISystem = "Har barnetrygden det er søkt om"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "avslagLovligOppholdSkjonnsmessigVurderingTredjelandsborger",
                navnISystem = "Skjønnsmessig vurdering opphold tredjelandsborger"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "fortsattInnvilgetBarnBosattIRiket",
                navnISystem = "Barn oppholder seg i Norge"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "avslagOpplysningsplikt",
                navnISystem = "Ikke mottatt opplysninger"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "fortsattInnvilgetSokerOgBarnBosattIRiket",
                navnISystem = "Søker og barn oppholder seg i Norge"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "opphorEtBarnErDodt",
                navnISystem = "Et barn er dødt"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "innvilgetFodselshendelseNyfodtBarn",
                navnISystem = "Nyfødt barn - har barn fra før"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "reduksjonDeltBostedEnighet",
                navnISystem = "Enighet om opphør av avtale om delt bosted"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "innvilgetLovligOppholdEOSBorgerSkjonnsmessigVurdering",
                navnISystem = "EØS-borger: Skjønnsmessig vurdering av oppholdsrett."
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "innvilgetLovligOppholdEOSBorger",
                navnISystem = "EØS-borger: Søker har oppholdsrett"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "etterbetaling3Aar",
                navnISystem = "Etterbetaling 3 år"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "avslagBosattIRiket",
                navnISystem = "Ikke bosatt i Norge"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "fortsattInnvilgetAnnenForelderIkkeSokt",
                navnISystem = "Annen forelder ikke søkt om delt barnetrygd"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "innvilgetBarnBorSammenMedMottaker",
                navnISystem = "Foreldrene bor sammen, endret mottaker"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "reduksjonEndretMottaker",
                navnISystem = "Foreldrene bor sammen, endret mottaker"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "reduksjonManglendeOpplysninger",
                navnISystem = "Ikke mottatt opplysninger"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "innvilgetMedlemIFolketrygden",
                navnISystem = "Medlem i Folketrygden"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "innvilgetBosattIRiket",
                navnISystem = "Norsk, nordisk bosatt i Norge"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "innvilgetNyfodtBarnForste",
                navnISystem = "Nyfødt barn - første barn"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "innvilgetNyfodtBarn",
                navnISystem = "Nyfødt barn - har barn fra før"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "reduksjonSatsendring",
                navnISystem = "Satsendring"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "innvilgetBorHosSokerSkjonnsmessig",
                navnISystem = "Skjønnsmessig vurdering - Barn har flyttet til søker (flytting mellom foreldre, andre omsorgspersoner)"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "innvilgetLovligOppholdSkjonnsmessigVurderingTredjelandsborger",
                navnISystem = "Skjønnsmessig vurdering tålt opphold tredjelandsborger"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "innvilgetFastOmsorgForBarn",
                navnISystem = "Søker har fast omsorg for barn (beredskapshjem, vurdering av fast bosted)"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "reduksjonFastOmsorgForBarn",
                navnISystem = "Søker har ikke lenger fast omsorg for barn (beredskapshjem, vurdering av fast bosted)"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "innvilgetLovligOppholdOppholdstillatelse",
                navnISystem = "Tredjelandsborger bosatt før lovlig opphold i Norge"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "innvilgetBosattIRiketLovligOpphold",
                navnISystem = "Tredjelandsborger med lovlig opphold samtidig som bosatt i Norge"
        ),
        NavnTilNedtrekksmeny(
                apiNavn = "reduksjonDeltBostedUenighet",
                navnISystem = "Uenighet om opphør av avtale om delt bosted"
        )
)