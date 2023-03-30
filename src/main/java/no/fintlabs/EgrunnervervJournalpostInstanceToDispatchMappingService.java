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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.fintlabs.links.ResourceLinkUtil.getFirstLink;
import static no.fintlabs.links.ResourceLinkUtil.getFirstSelfLink;

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
        ArkivressursResource saksansvarligArkivressurs = arkivressursResourceCache.get(
                getFirstLink(sakResource::getSaksansvarlig, sakResource, "Saksansvarlig")
        );
        PersonalressursResource saksansvarligPersonalressursResource = personalressursResourceCache.get(
                getFirstLink(saksansvarligArkivressurs::getPersonalressurs, saksansvarligArkivressurs, "Personalressurs")
        );

        PersonResource saksansvarligPersonResource = personResourceCache.get(
                getFirstLink(saksansvarligPersonalressursResource::getPerson, saksansvarligPersonalressursResource, "Person")
        );

        JournalpostResource journalpostResource = sakResource.getJournalpost()
                .stream()
                .filter(journalpost -> Objects.equals(journalpost.getJournalPostnummer(), journalpostNummer))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No journalpost with journalpostNummer=" + journalpostNummer));

        List<DokumentTypeResource> dokumentTypeResources = journalpostResource.getDokumentbeskrivelse()
                .stream()
                .map(dokumentBeskrivelsesResource -> dokumentTypeResourceCache.get(
                        getFirstLink(dokumentBeskrivelsesResource::getDokumentType, dokumentBeskrivelsesResource, "Dokumenttype"))
                )
                .toList();

        AdministrativEnhetResource administrativEnhetResource = administrativEnhetResourceCache.get(
                getFirstSelfLink(journalpostResource)
        );

        JournalStatusResource journalStatusResource = journalStatusResourceCache.get(
                getFirstLink(journalpostResource::getJournalstatus, journalpostResource, "Journalstatus")
        );

        JournalpostTypeResource journalpostTypeResource = journalpostTypeResourceCache.get(
                getFirstLink(journalpostResource::getJournalposttype, journalpostResource, "Journalposttype")
        );

        return EgrunnervervJournalpostInstanceToDispatch
                .builder()
                .statusId(journalStatusResource.getSystemId().getIdentifikatorverdi())
                .dokumentTypeNavn(journalpostTypeResource.getNavn())
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
                .saksansvarligBrukernavn(saksansvarligPersonalressursResource.getBrukernavn().getIdentifikatorverdi())
                .saksansvarligNavn(Stream.of(
                                        saksansvarligPersonResource.getNavn().getFornavn(),
                                        saksansvarligPersonResource.getNavn().getMellomnavn(),
                                        saksansvarligPersonResource.getNavn().getEtternavn()
                                ).filter(Objects::nonNull)
                                .collect(Collectors.joining(" "))
                )
                .adminEnhetKortnavn(administrativEnhetResource.getSystemId().getIdentifikatorverdi())
                .adminEnhetNavn(administrativEnhetResource.getNavn())
                .build();
    }

}
