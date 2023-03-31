package no.fintlabs;

import no.fint.model.felles.basisklasser.Begrep;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.resource.administrasjon.personal.PersonalressursResource;
import no.fint.model.resource.arkiv.kodeverk.DokumentTypeResource;
import no.fint.model.resource.arkiv.kodeverk.JournalStatusResource;
import no.fint.model.resource.arkiv.kodeverk.JournalpostTypeResource;
import no.fint.model.resource.arkiv.noark.AdministrativEnhetResource;
import no.fint.model.resource.arkiv.noark.ArkivressursResource;
import no.fint.model.resource.arkiv.noark.JournalpostResource;
import no.fint.model.resource.arkiv.noark.SakResource;
import no.fint.model.resource.felles.PersonResource;
import no.fintlabs.cache.FintCache;
import no.fintlabs.model.EgrunnervervJournalpostInstanceToDispatch;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.fintlabs.links.ResourceLinkUtil.getOptionalFirstLink;

@Service
public class EgrunnervervJournalpostInstanceToDispatchMappingService {

    private final FintCache<String, AdministrativEnhetResource> administrativEnhetResourceCache;
    private final FintCache<String, ArkivressursResource> arkivressursResourceCache;
    private final FintCache<String, DokumentTypeResource> dokumentTypeResourceCache;
    private final FintCache<String, JournalStatusResource> journalStatusResourceCache;
    private final FintCache<String, JournalpostTypeResource> journalpostTypeResourceCache;
    private final FintCache<String, PersonalressursResource> personalressursResourceCache;
    private final FintCache<String, PersonResource> personResourceCache;

    public EgrunnervervJournalpostInstanceToDispatchMappingService(
            FintCache<String, AdministrativEnhetResource> administrativEnhetResourceCache,
            FintCache<String, ArkivressursResource> arkivressursResourceCache,
            FintCache<String, DokumentTypeResource> dokumentTypeResourceCache,
            FintCache<String, JournalStatusResource> journalStatusResourceCache,
            FintCache<String, JournalpostTypeResource> journalpostTypeResourceCache,
            FintCache<String, PersonalressursResource> personalressursResourceCache,
            FintCache<String, PersonResource> personResourceCache
    ) {
        this.administrativEnhetResourceCache = administrativEnhetResourceCache;
        this.arkivressursResourceCache = arkivressursResourceCache;
        this.dokumentTypeResourceCache = dokumentTypeResourceCache;
        this.journalStatusResourceCache = journalStatusResourceCache;
        this.journalpostTypeResourceCache = journalpostTypeResourceCache;
        this.personalressursResourceCache = personalressursResourceCache;
        this.personResourceCache = personResourceCache;
    }


    public EgrunnervervJournalpostInstanceToDispatch map(SakResource sakResource, Long journalpostNummer) {

        Optional<PersonalressursResource> saksansvarligPersonalressursResource =
                getOptionalFirstLink(sakResource::getSaksansvarlig)
                        .flatMap(arkivressursResourceCache::getOptional)
                        .flatMap(arkivressurs -> getOptionalFirstLink(arkivressurs::getPersonalressurs))
                        .flatMap(personalressursResourceCache::getOptional);

        Optional<PersonResource> saksansvarligPersonResource =
                saksansvarligPersonalressursResource
                        .flatMap(personalressursResource -> getOptionalFirstLink(personalressursResource::getPerson))
                        .flatMap(personResourceCache::getOptional);

        JournalpostResource journalpostResource = sakResource.getJournalpost()
                .stream()
                .filter(journalpost -> Objects.equals(journalpost.getJournalPostnummer(), journalpostNummer))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No journalpost with journalpostNummer=" + journalpostNummer));

        List<DokumentTypeResource> dokumentTypeResources = journalpostResource.getDokumentbeskrivelse()
                .stream()
                .map(dokumentBeskrivelsesResource -> getOptionalFirstLink(dokumentBeskrivelsesResource::getDokumentType)
                        .flatMap(dokumentTypeResourceCache::getOptional)
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        Optional<AdministrativEnhetResource> administrativEnhetResource =
                getOptionalFirstLink(journalpostResource::getAdministrativEnhet)
                        .flatMap(administrativEnhetResourceCache::getOptional);

        Optional<JournalStatusResource> journalStatusResource =
                getOptionalFirstLink(journalpostResource::getJournalstatus)
                        .flatMap(journalStatusResourceCache::getOptional);

        Optional<JournalpostTypeResource> journalpostTypeResource =
                getOptionalFirstLink(journalpostResource::getJournalposttype)
                        .flatMap(journalpostTypeResourceCache::getOptional);

        return EgrunnervervJournalpostInstanceToDispatch
                .builder()
                .statusId(journalStatusResource.map(Begrep::getSystemId).map(Identifikator::getIdentifikatorverdi).orElse(""))
                .dokumentTypeNavn(journalpostTypeResource.map(Begrep::getNavn).orElse(""))
                .dokumentkategoriId(dokumentTypeResources
                        .stream()
                        .map(Begrep::getSystemId)
                        .map(Identifikator::getIdentifikatorverdi)
                        .collect(Collectors.joining(", "))
                )
                .dokumentkategoriNavn(dokumentTypeResources
                        .stream()
                        .map(Begrep::getNavn)
                        .collect(Collectors.joining(", "))
                )
                .saksansvarligBrukernavn(
                        saksansvarligPersonalressursResource
                                .map(PersonalressursResource::getBrukernavn)
                                .map(Identifikator::getIdentifikatorverdi)
                                .orElse("")
                )
                .saksansvarligNavn(saksansvarligPersonResource.map(resource -> Stream.of(
                                                resource.getNavn().getFornavn(),
                                                resource.getNavn().getMellomnavn(),
                                                resource.getNavn().getEtternavn()
                                        ).filter(Objects::nonNull)
                                        .collect(Collectors.joining(" "))
                        ).orElse("")
                )
                .adminEnhetKortnavn(
                        administrativEnhetResource
                                .map(AdministrativEnhetResource::getSystemId)
                                .map(Identifikator::getIdentifikatorverdi)
                                .orElse("")
                )
                .adminEnhetNavn(administrativEnhetResource.map(AdministrativEnhetResource::getNavn).orElse(""))
                .build();
    }

}
