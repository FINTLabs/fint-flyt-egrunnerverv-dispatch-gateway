package no.fintlabs.links;

import no.fint.model.resource.FintLinks;
import no.fint.model.resource.Link;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceLinkUtil {

    public static List<String> getSelfLinks(FintLinks resource) {
        return resource.getSelfLinks()
                .stream()
                .map(Link::getHref)
                .collect(Collectors.toList());
    }

    public static String getFirstLink(Supplier<List<Link>> linkProducer, FintLinks resource, String linkedResourceName) {
        return Optional.ofNullable(linkProducer.get())
                .map(Collection::stream)
                .flatMap(Stream::findFirst)
                .map(Link::getHref)
                .orElseThrow(() -> NoSuchLinkException.noLink(resource, linkedResourceName));
    }

}
