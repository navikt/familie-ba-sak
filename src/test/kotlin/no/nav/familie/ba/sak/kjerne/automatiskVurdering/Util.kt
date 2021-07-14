package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import io.mockk.every
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.DødsfallData
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingIntegrationTest
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.GrBostedsadresseperiode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.junit.Assert
import java.time.LocalDate


fun mockPersonopplysning(personnr: String, personInfo: PersonInfo, personopplysningerService: PersonopplysningerService) {
    every { personopplysningerService.hentPersoninfo(personnr) } returns personInfo
    every { personopplysningerService.hentIdenter(Ident(personnr)) } returns listOf(IdentInformasjon(personnr,
                                                                                                     false,
                                                                                                     gruppe = ""))
    every { personopplysningerService.hentPersoninfoMedRelasjoner(personnr) } returns personInfo
    every { personopplysningerService.hentAktivAktørId(Ident(personnr)) } returns AktørId(personnr)
    every { personopplysningerService.hentStatsborgerskap(Ident(personnr)) } returns
            listOf(Statsborgerskap("NOK",
                                   personInfo.fødselsdato,
                                   LocalDate.now()
                                           .plusYears(
                                                   30)))
    every {
        personopplysningerService.hentBostedsadresseperioder(personnr)
    } answers {
        listOf(GrBostedsadresseperiode(
                periode = DatoIntervallEntitet(
                        fom = LocalDate.of(2002, 1, 4),
                        tom = LocalDate.of(2022, 1, 5)
                )))
    }
    every {
        personopplysningerService.hentOpphold(personnr)
    } answers {

        personInfo.opphold!!
    }
    /*
    listOf(Opphold(type = OPPHOLDSTILLATELSE.PERMANENT,
                       oppholdFra = personInfo.fødselsdato,
                       oppholdTil = LocalDate.of(2499, 1, 1)))*/
    every { personopplysningerService.hentDødsfall(Ident(personnr)) } returns DødsfallData(false, null)

}

fun mockIntegrasjonsClient(personNr: String, integrasjonClient: IntegrasjonClient) {
    every { integrasjonClient.hentBehandlendeEnhet(personNr) } returns listOf(
            Arbeidsfordelingsenhet(enhetId = ArbeidsfordelingIntegrationTest.IKKE_FORTROLIG_ENHET,
                                   enhetNavn = "vanlig enhet"))
    every { integrasjonClient.hentLand(any()) } returns "NOK"
}

fun behandlingOgFagsakErÅpen(behanding: Behandling, fagsak: Fagsak) {
    Assert.assertEquals(BehandlingStatus.UTREDES, behanding.status)
    Assert.assertEquals(BehandlingÅrsak.FØDSELSHENDELSE, behanding.opprettetÅrsak)
    Assert.assertEquals(StegType.VILKÅRSVURDERING, behanding.steg)
    Assert.assertEquals(FagsakStatus.LØPENDE, fagsak?.status)
}
