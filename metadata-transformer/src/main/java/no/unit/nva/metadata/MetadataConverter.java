package no.unit.nva.metadata;

import no.unit.nva.api.CreatePublicationRequest;
import no.unit.nva.doi.fetch.language.LanguageMapper;
import no.unit.nva.metadata.domain.Metadata;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.exceptions.MalformedContributorException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static nva.commons.core.attempt.Try.attempt;

public class MetadataConverter implements PublicationConverter {

    public static final String VALUE = "value";

    private static final Logger logger = LoggerFactory.getLogger(MetadataConverter.class);

    @Override
    public CreatePublicationRequest getCreatePublicationRequest(Metadata metadata) {

        EntityDescription entityDescription = new EntityDescription.Builder()
                .withMainTitle(getMainTitle(metadata))
                .withContributors(getContributors(metadata))
                .withDescription(getDescription(metadata))
                .withTags(getTags(metadata))
                .withLanguage(getLanguage(metadata))
                .build();

        CreatePublicationRequest request = new CreatePublicationRequest();
        request.setEntityDescription(entityDescription);

        return request;
    }

    private URI getLanguage(Metadata metadata) {
        String language = Optional.ofNullable(metadata.getDescription().get(VALUE)).orElse(null);
        return LanguageMapper.getUriFromIsoAsOptional(language).orElse(null);
    }

    private List<String> getTags(Metadata metadata) {
        return metadata.getSubjects().stream()
                .map(map -> map.get(VALUE))
                .collect(Collectors.toList());
    }

    private String getDescription(Metadata metadata) {
        return Optional.ofNullable(metadata.getDescription().get(VALUE)).orElse(null);
    }

    private List<Contributor> getContributors(Metadata metadata) {
        List<Contributor> contributors = Collections.emptyList();
        List<String> list = metadata.getCreators().stream()
                .map(map -> map.get(VALUE))
                .collect(Collectors.toList());
        List<Try<Contributor>> contributorMappings =
                IntStream.range(0, list.size())
                        .boxed()
                        .map(attempt(index -> toContributor(list.get(index), index + 1)))
                        .collect(Collectors.toList());

        reportFailures(contributorMappings);
        contributors = successfulMappings(contributorMappings);
        return contributors;
    }

    private List<Contributor> successfulMappings(List<Try<Contributor>> contributorMappings) {
        return contributorMappings.stream()
                .filter(Try::isSuccess)
                .map(Try::get)
                .collect(Collectors.toList());
    }

    @JacocoGenerated
    private void reportFailures(List<Try<Contributor>> contributors) {
        contributors.stream().filter(Try::isFailure)
                .map(Try::getException)
                .forEach(e -> logger.error(e.getMessage(), e));
    }

    private Contributor toContributor(String name, Integer sequence) {
        Contributor contributor = null;
        try {
            Identity identity = new Identity.Builder()
                    .withName(name)
                    .build();
            contributor = new Contributor.Builder()
                    .withIdentity(identity)
                    .withSequence(sequence)
                    .build();
        } catch (MalformedContributorException e) {
            logger.warn(e.getMessage());
        }
        return contributor;
    }

    private String getMainTitle(Metadata metadata) {
        return Optional.ofNullable(metadata.getTitle().get(VALUE)).orElse(null);
    }

}
