package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.erStrengtFortrolig
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FiltreringsreglerFaktaSøknad
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresser
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.iUkraina
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.FiltrerAutomatiskBehandlingData
import no.nav.familie.ba.sak.kjerne.søknad.SøknadService
import no.nav.familie.tidslinje.utvidelser.verdiPåTidspunkt
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.YearMonth

@Component
class FaktaOppretter(
    private val personopplysningerService: PersonopplysningerService,
    private val personidentService: PersonidentService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
    private val søknadService: SøknadService,
    private val clockProvider: ClockProvider,
    private val vilkårvurderer: Vilkårvurderer,
) {
    fun opprettFakta(
        filtrerAutomatiskBehandlingData: FiltrerAutomatiskBehandlingData,
        behandling: Behandling,
    ): FiltreringsreglerFaktaSøknad {
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
        if (personopplysningGrunnlag == null) {
            throw Feil("Fant ikke personopplysninggrunnlag for behandling ${behandling.id}")
        }

        val søknad = søknadService.finnDigitalSøknad(behandling.id)
        if (søknad == null) {
            throw Feil("Fant ikke digital søknad for behandling ${behandling.id}")
        }

        val aktørSøker = personidentService.hentAktør(filtrerAutomatiskBehandlingData.søkersIdent)
        val aktørBarna = personidentService.hentAktørIder(søknad.barn.map { it.fnr })

        val barnaFraSøknad = personopplysningGrunnlag.barna.filter { aktørBarna.contains(it.aktør) }

        val personInfo = personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør = aktørSøker, relevanteAktører = aktørBarna.toSet())

        val forelderBarnRelasjonerForSøknadsbarna = personInfo.forelderBarnRelasjon.filter { it.aktør in aktørBarna }

        return FiltreringsreglerFaktaSøknad(
            søker = personopplysningGrunnlag.søker,
            søkerMottarLøpendeUtvidet = behandling.underkategori == BehandlingUnderkategori.UTVIDET,
            søkerOppfyllerVilkårForUtvidetBarnetrygd = vilkårvurderer.oppfyllerSøkerVilkårForUtvidetBarnetrygd(behandling, barnaFraSøknad),
            søkerMottarEøsBarnetrygd = behandling.kategori == BehandlingKategori.EØS,
            barnaSomSkalVurderes = barnaFraSøknad,
            søkerLever = !personopplysningGrunnlag.søker.erDød(),
            barnaLever = barnaFraSøknad.none { it.erDød() },
            søkerHarVerge = personopplysningerService.harVerge(aktørSøker).harVerge,
            utbetalesBarnetrygdForBarnetTilAnnenMottakerIInneværendeMåned = tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(behandling, barnaFraSøknad, YearMonth.now(clockProvider.get())),
            søkerHarKryssetPåEøsSpørsmålISøknaden = søknad.harKryssetPåEøsSpørsmål,
            søkerHarKryssetForDeltBostedISøknaden = søknad.harKryssetForDeltBostedForMinstEttBarn(),
            søkerHarKryssetForFosterhjemEllerBeredskapshjemISøknaden = søknad.harKryssetForFosterhjemEllerBeredskapshjemForMinstEttBarn(),
            søknadenInneholderVedlegg = søknad.inneholderVedlegg,
            søkerHarIkkeLøpendeUtbetalingOgHarAldriHattUtbetaling = false, // TODO Fix me
            søkerHarAdressebeskyttelseGradering6Eller19 = personInfo.adressebeskyttelseGradering.erStrengtFortrolig(),
            barnHarAdressebeskyttelseGradering6Eller19 = forelderBarnRelasjonerForSøknadsbarna.any { it.adressebeskyttelseGradering.erStrengtFortrolig() },
            søkerOgBarnHarForelderBarnRelasjon = barnaFraSøknad.all { barnFraSøknad -> forelderBarnRelasjonerForSøknadsbarna.any { it.aktør == barnFraSøknad.aktør } } && barnaFraSøknad.isNotEmpty(),
            søkerHarAktivNorskBostedsadresse = harAktivNorskBostedsadresse(listOf(personopplysningGrunnlag.søker), LocalDate.now(clockProvider.get())),
            barnHarAktivNorskBostedsadresse = harAktivNorskBostedsadresse(barnaFraSøknad, LocalDate.now(clockProvider.get())),
            søkerHarUkrainskStatsborgerskap = personopplysningGrunnlag.søker.statsborgerskap.iUkraina(),
            barnHarUkrainskStatsborgerskap = barnaFraSøknad.any { it.statsborgerskap.iUkraina() },
        )
    }

    private fun harAktivNorskBostedsadresse(
        personer: List<Person>,
        tidspunkt: LocalDate,
    ): Boolean =
        personer.all { person ->
            val tidslinje = Adresser.opprettFra(person = person).lagErBosattINorgeTidslinje()
            tidslinje.verdiPåTidspunkt(tidspunkt) == true
        } && personer.isNotEmpty()
}
