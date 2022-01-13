package no.sikt.nva.scopus.factory;

import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.AuthorTp;
import no.scopus.generated.CorrespondenceTp;
import no.scopus.generated.PersonalnameType;
import no.sikt.nva.scopus.model.Author;
import no.sikt.nva.scopus.model.Institution;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class ScopusAuthorFactory {

    @JacocoGenerated
    public ScopusAuthorFactory() {}

    public static List<Author> buildAuthors(List<AuthorGroupTp> authorGroupTps) {
        if (authorGroupTps == null) return new ArrayList<>();

        Map<Integer,Author> authors = new LinkedHashMap<>();

        for (AuthorGroupTp authorGroupTp : authorGroupTps) {

            Institution institution = new Institution(authorGroupTp.getAffiliation());

            authorGroupTp.getAuthorOrCollaboration().stream().filter(AuthorTp.class::isInstance).forEach(authorObject -> {
                AuthorTp author = (AuthorTp)authorObject;

                Integer sequenceNr = Integer.valueOf(author.getSeq());

                authors.putIfAbsent(sequenceNr,new Author(
                        null,
                        author.getAuid(),
                        "FORFATTER",
                        sequenceNr,author.getSurname(),
                        author.getGivenName(),
                        buildAuthorName(author.getIndexedName(), author.getGivenName(), author.getSurname()),
                        author.getPreferredName() != null ?
                        buildAuthorName(author.getPreferredName().getIndexedName(), author.getPreferredName().getGivenName(), author.getPreferredName().getSurname()) : null,
                        author.getOrcid(),
                        null));

                authors.get(sequenceNr).getInstitutions().add(institution);

            });

        }

        return authors.values().stream().collect(Collectors.toList());

    }


    public static Author buildCorrespondingAuthor(CorrespondenceTp correspondenceTp) {

        if (correspondenceTp == null || (correspondenceTp.getPerson() == null && correspondenceTp.getAffiliation() == null)) {
            return null;
        }

        PersonalnameType person = correspondenceTp.getPerson();

        if (person != null) {
            return new Author(null,
                    null,
                    null,
                    null,
                    person.getSurname(),
                    person.getGivenName(),
                    buildAuthorName(person.getIndexedName(), person.getGivenName(), person.getSurname()),
                    null,
                    null,
                    Collections.singletonList(new Institution(correspondenceTp.getAffiliation())));
        } else {
            return new Author(null, null, null, null, null, null, null, null, null, Collections.singletonList(new Institution(correspondenceTp.getAffiliation())));
        }
    }


    public static void tryToFindCorrespondingAuthorExternalIds(Author correspondent, List<Author> authors) {

        if (correspondent == null || (correspondent.getAuthorName() == null && correspondent.getSurname() == null)) {
            return;
        }

        String matchingExternalId = null;
        String matchingOrcid = null;
        Institution matchingInstitution = null;
        int numberOfMatches = 0; // We only want one author to match

        for (Author author : authors) {

            if (numberOfMatches > 1) return; // If we have more than one we stop the loop

            if (correspondent.getAuthorName() != null
                    && author.getAuthorName() != null
                    && correspondent.getAuthorName().equalsIgnoreCase(author.getAuthorName())) {

                matchingInstitution = tryToFindCorrespondingAuthorInstitutionId(correspondent, author.getInstitutions());
                matchingExternalId = author.getExternalId();
                matchingOrcid = author.getOrcid();
                numberOfMatches++;
            } else if (correspondent.getSurname() != null
                    && author.getSurname() != null
                    && correspondent.getSurname().equalsIgnoreCase(author.getSurname())) {

                matchingInstitution = tryToFindCorrespondingAuthorInstitutionId(correspondent, author.getInstitutions());
                matchingExternalId = author.getExternalId();
                matchingOrcid = author.getOrcid();
                numberOfMatches++;
            }
        }

        if (numberOfMatches == 1) { // If we have exactly one match, we use its data
            if (matchingExternalId != null) {
                correspondent.setExternalId(matchingExternalId);
            }
            if (matchingOrcid != null) {
                correspondent.setOrcid(matchingOrcid);
            }
            if (matchingInstitution != null) {
                correspondent.getInstitutions().get(0).setExternalId(matchingInstitution.getExternalId());
                correspondent.getInstitutions().get(0).setUnitId(matchingInstitution.getUnitId());
            }
        }
    }


    private static Institution tryToFindCorrespondingAuthorInstitutionId(Author correspondent, List<Institution> institutions) {

        for (Institution institution : institutions) {
            if (correspondent.getInstitutions().get(0).getInstitutionName() != null
                    && institution.getInstitutionName() != null
                    && correspondent.getInstitutions().get(0).getInstitutionName().equalsIgnoreCase(institution.getInstitutionName())) {

                return institution;
            }
        }

        return null;
    }


    private static String buildAuthorName(String indexedName, String givenName, String surname) {
        if (!StringUtils.isEmpty(indexedName)) {
            return indexedName;
        } else if (givenName != null && surname != null) {
            return surname + ", " + givenName;
        } else {
            return surname;
        }
    }
}
