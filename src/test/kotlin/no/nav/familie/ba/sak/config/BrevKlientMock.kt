package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.kjerne.dokument.BrevKlient
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityBegrunnelse
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

        override fun hentSanityBegrunnelse(): List<SanityBegrunnelse> {
                return navnTilNedtrekksmenyMock
        }
}

val navnTilNedtrekksmenyMock: List<SanityBegrunnelse> = listOf(
        SanityBegrunnelse(
                apiNavn = "avslagIkkeAvtaleOmDeltBosted",
                navnISystem = "Ikke avtale om delt bosted"
        ),
        SanityBegrunnelse(
                apiNavn = "avslagLovligOppholdEosBorger",
                navnISystem = "EØS-borger uten oppholdsrett"
        ),
        SanityBegrunnelse(
                apiNavn = "avslagUregistrertBarn",
                navnISystem = "Barn uten fødselsnummer"
        ),
        SanityBegrunnelse(
                apiNavn = "innvilgelseEnighetOmAtAvtalenOmDeltBostedErOpphort",
                navnISystem = "Enighet om at avtalen om delt bosted er opphørt"
        ),
        SanityBegrunnelse(
                apiNavn = "opphorFlyttetFraNorge",
                navnISystem = "Flyttet fra Norge"
        ),
        SanityBegrunnelse(
                apiNavn = "fortsattInnvilgetMedlemIFolketrygden",
                navnISystem = "Medlem i folketrygden"
        ),
        SanityBegrunnelse(
                apiNavn = "opphorFlereBarnErDode",
                navnISystem = "Flere barn er døde"
        ),
        SanityBegrunnelse(
                apiNavn = "avslagSaerkullsbarn",
                navnISystem = "Ektefelle eller samboers særkullsbarn"
        ),
        SanityBegrunnelse(
                apiNavn = "opphorDeltBostedOpphortUenighet",
                navnISystem = "Uenighet om opphør av avtale om delt bosted"
        ),
        SanityBegrunnelse(
                apiNavn = "fortsattInnvilgetFastOmsorg",
                navnISystem = "Fortsatt fast omsorg for barn"
        ),
        SanityBegrunnelse(
                apiNavn = "opphorBarnBorIkkeMedSoker",
                navnISystem = "Barn bor ikke med søker"
        ),
        SanityBegrunnelse(
                apiNavn = "avslagOmsorgForBarn",
                navnISystem = "Adopsjon, surrogati, beredskapshjem, vurdering av fast bosted\t"
        ),
        SanityBegrunnelse(
                apiNavn = "fortsattInnvilgetBorMedSoker",
                navnISystem = "Barn bosatt med søker"
        ),
        SanityBegrunnelse(
                apiNavn = "fortsattInnvilgetBarnOgSokerLovligOppholdOppholdstillatelse",
                navnISystem = "Tredjelandsborger søker og barn fortsatt lovlig opphold i Norge"
        ),
        SanityBegrunnelse(
                apiNavn = "fortsattInnvilgetDeltBostedPraktiseresFortsatt",
                navnISystem = "Delt bosted praktiseres fortsatt"
        ),
        SanityBegrunnelse(
                apiNavn = "innvilgelseUenighetOmOpphorAvAvtaleOmDeltBosted",
                navnISystem = "Uenighet om opphør av avtale om delt bosted"
        ),
        SanityBegrunnelse(
                apiNavn = "avslagMedlemIFolketrygden",
                navnISystem = "Unntatt medlemskap i Folketrygden"
        ),
        SanityBegrunnelse(
                apiNavn = "fortsattInnvilgetBarnLovligOppholdOppholdstillatelse",
                navnISystem = "Tredjelandsborger barn fortsatt lovlig opphold i Norge"
        ),
        SanityBegrunnelse(
                apiNavn = "innvilgetFodselshendelseNyfodtBarnForste",
                navnISystem = "Nyfødt barn - første barn"
        ),
        SanityBegrunnelse(
                apiNavn = "opphorUnder18Aar",
                navnISystem = "Barn 18 år"
        ),
        SanityBegrunnelse(
                apiNavn = "avslagUnder18Aar",
                navnISystem = "Barn over 18 år"
        ),
        SanityBegrunnelse(
                apiNavn = "opphorSokerHarIkkeFastOmsorg",
                navnISystem = "Søker har ikke lenger fast omsorg for barn (beredskapshjem, vurdering av fast bosted)"
        ),
        SanityBegrunnelse(
                apiNavn = "avslagForeldreneBorSammen",
                navnISystem = "Foreldrene bor sammen"
        ),
        SanityBegrunnelse(
                apiNavn = "fortsattInnvilgetLovligOppholdEOS",
                navnISystem = "EØS-borger: Søker har oppholdsrett"
        ),
        SanityBegrunnelse(
                apiNavn = "fortsattInnvilgetLovligOppholdTredjelandsborger",
                navnISystem = "Tålt opphold tredjelandsborger"
        ),
        SanityBegrunnelse(
                apiNavn = "avslagUgyldigAvtaleOmDeltBosted",
                navnISystem = "Ugyldig avtale om delt bosted"
        ),
        SanityBegrunnelse(
                apiNavn = "opphorHarIkkeOppholdstillatelse",
                navnISystem = "Oppholdstillatelse utløpt\t"
        ),
        SanityBegrunnelse(
                apiNavn = "avslagLovligOppholdTredjelandsborger",
                navnISystem = "Tredjelandsborger uten lovlig opphold i Norge"
        ),
        SanityBegrunnelse(
                apiNavn = "innvilgetSatsendring",
                navnISystem = "Autotekst ved satsendring\t"
        ),
        SanityBegrunnelse(
                apiNavn = "opphorIkkeMottattOpplysninger",
                navnISystem = "Du ikke har sendt oss de opplysningene vi ba om.  "
        ),
        SanityBegrunnelse(
                apiNavn = "innvilgetOmsorgForBarn",
                navnISystem = "Adopsjon, surrogati: Omsorgen for barn"
        ),
        SanityBegrunnelse(
                apiNavn = "opphorDeltBostedOpphortEnighet",
                navnISystem = "Enighet om opphør av avtale om delt bosted"
        ),
        SanityBegrunnelse(
                apiNavn = "opphorEndretMottaker",
                navnISystem = "Foreldrene bor sammen, endret mottaker\t"
        ),
        SanityBegrunnelse(
                apiNavn = "fortsattInnvilgetSokerBosattIRiket",
                navnISystem = "Søker oppholder seg i Norge"
        ),
        SanityBegrunnelse(
                apiNavn = "reduksjonUnder18Aar",
                navnISystem = "Barn 18 år"
        ),
        SanityBegrunnelse(
                apiNavn = "reduksjonUnder6Aar",
                navnISystem = "Barn 6 år"
        ),
        SanityBegrunnelse(
                apiNavn = "reduksjonBarnDod",
                navnISystem = "Barn død"
        ),
        SanityBegrunnelse(
                apiNavn = "reduksjonBosattIRiket",
                navnISystem = "Barn har flyttet fra Norge"
        ),
        SanityBegrunnelse(
                apiNavn = "reduksjonFlyttetBarn",
                navnISystem = "Barn har flyttet fra søker (flytting mellom foreldre, andre omsorgspersoner)"
        ),
        SanityBegrunnelse(
                apiNavn = "innvilgetBorHosSoker",
                navnISystem = "Barn har flyttet til søker (flytting mellom foreldre, andre omsorgspersoner)"
        ),
        SanityBegrunnelse(
                apiNavn = "reduksjonLovligOppholdOppholdstillatelseBarn",
                navnISystem = "Barn har ikke oppholdstillatelse"
        ),
        SanityBegrunnelse(
                apiNavn = "fortsattInnvilgetSokerLovligOppholdOppholdstillatelse",
                navnISystem = "Tredjelandsborger søker fortsatt lovlig opphold i Norge"
        ),
        SanityBegrunnelse(
                apiNavn = "avslagBorHosSoker",
                navnISystem = "Barn bor ikke med søker\t"
        ),
        SanityBegrunnelse(
                apiNavn = "fortsattInnvilgetUendretTrygd",
                navnISystem = "Har barnetrygden det er søkt om"
        ),
        SanityBegrunnelse(
                apiNavn = "avslagLovligOppholdSkjonnsmessigVurderingTredjelandsborger",
                navnISystem = "Skjønnsmessig vurdering opphold tredjelandsborger"
        ),
        SanityBegrunnelse(
                apiNavn = "fortsattInnvilgetBarnBosattIRiket",
                navnISystem = "Barn oppholder seg i Norge"
        ),
        SanityBegrunnelse(
                apiNavn = "avslagOpplysningsplikt",
                navnISystem = "Ikke mottatt opplysninger"
        ),
        SanityBegrunnelse(
                apiNavn = "fortsattInnvilgetSokerOgBarnBosattIRiket",
                navnISystem = "Søker og barn oppholder seg i Norge"
        ),
        SanityBegrunnelse(
                apiNavn = "opphorEtBarnErDodt",
                navnISystem = "Et barn er dødt"
        ),
        SanityBegrunnelse(
                apiNavn = "innvilgetFodselshendelseNyfodtBarn",
                navnISystem = "Nyfødt barn - har barn fra før"
        ),
        SanityBegrunnelse(
                apiNavn = "reduksjonDeltBostedEnighet",
                navnISystem = "Enighet om opphør av avtale om delt bosted"
        ),
        SanityBegrunnelse(
                apiNavn = "innvilgetLovligOppholdEOSBorgerSkjonnsmessigVurdering",
                navnISystem = "EØS-borger: Skjønnsmessig vurdering av oppholdsrett."
        ),
        SanityBegrunnelse(
                apiNavn = "innvilgetLovligOppholdEOSBorger",
                navnISystem = "EØS-borger: Søker har oppholdsrett"
        ),
        SanityBegrunnelse(
                apiNavn = "etterbetaling3Aar",
                navnISystem = "Etterbetaling 3 år"
        ),
        SanityBegrunnelse(
                apiNavn = "avslagBosattIRiket",
                navnISystem = "Ikke bosatt i Norge"
        ),
        SanityBegrunnelse(
                apiNavn = "fortsattInnvilgetAnnenForelderIkkeSokt",
                navnISystem = "Annen forelder ikke søkt om delt barnetrygd"
        ),
        SanityBegrunnelse(
                apiNavn = "innvilgetBarnBorSammenMedMottaker",
                navnISystem = "Foreldrene bor sammen, endret mottaker"
        ),
        SanityBegrunnelse(
                apiNavn = "reduksjonEndretMottaker",
                navnISystem = "Foreldrene bor sammen, endret mottaker"
        ),
        SanityBegrunnelse(
                apiNavn = "reduksjonManglendeOpplysninger",
                navnISystem = "Ikke mottatt opplysninger"
        ),
        SanityBegrunnelse(
                apiNavn = "innvilgetMedlemIFolketrygden",
                navnISystem = "Medlem i Folketrygden"
        ),
        SanityBegrunnelse(
                apiNavn = "innvilgetBosattIRiket",
                navnISystem = "Norsk, nordisk bosatt i Norge"
        ),
        SanityBegrunnelse(
                apiNavn = "innvilgetNyfodtBarnForste",
                navnISystem = "Nyfødt barn - første barn"
        ),
        SanityBegrunnelse(
                apiNavn = "innvilgetNyfodtBarn",
                navnISystem = "Nyfødt barn - har barn fra før"
        ),
        SanityBegrunnelse(
                apiNavn = "reduksjonSatsendring",
                navnISystem = "Satsendring"
        ),
        SanityBegrunnelse(
                apiNavn = "innvilgetBorHosSokerSkjonnsmessig",
                navnISystem = "Skjønnsmessig vurdering - Barn har flyttet til søker (flytting mellom foreldre, andre omsorgspersoner)"
        ),
        SanityBegrunnelse(
                apiNavn = "innvilgetLovligOppholdSkjonnsmessigVurderingTredjelandsborger",
                navnISystem = "Skjønnsmessig vurdering tålt opphold tredjelandsborger"
        ),
        SanityBegrunnelse(
                apiNavn = "innvilgetFastOmsorgForBarn",
                navnISystem = "Søker har fast omsorg for barn (beredskapshjem, vurdering av fast bosted)"
        ),
        SanityBegrunnelse(
                apiNavn = "reduksjonFastOmsorgForBarn",
                navnISystem = "Søker har ikke lenger fast omsorg for barn (beredskapshjem, vurdering av fast bosted)"
        ),
        SanityBegrunnelse(
                apiNavn = "innvilgetLovligOppholdOppholdstillatelse",
                navnISystem = "Tredjelandsborger bosatt før lovlig opphold i Norge"
        ),
        SanityBegrunnelse(
                apiNavn = "innvilgetBosattIRiketLovligOpphold",
                navnISystem = "Tredjelandsborger med lovlig opphold samtidig som bosatt i Norge"
        ),
        SanityBegrunnelse(
                apiNavn = "reduksjonDeltBostedUenighet",
                navnISystem = "Uenighet om opphør av avtale om delt bosted"
        )
)