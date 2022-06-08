package no.sikt.nva.scopus.test.utils;

import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import com.google.gson.Gson;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PiaAuthorResponseGenerator {

    private static final String SOURCE_CODE = "SCOPUS";

    public String convertToJson(List<Author> authors) {
        Gson gson = new Gson();
        return gson.toJson(authors);
    }

    public List<Author> generateAuthors() {
        int maxNumberOfAuthors = 20;
        var externalId = randomString();
        var firstname = randomString();
        var surname = randomString();
        var authorName = randomString();
        var cristinId = randomBoolean() ? 0 : randomInteger();
        return IntStream.range(0, randomInteger(maxNumberOfAuthors))
                   .boxed()
                   .map(index ->
                            generateAuthor(externalId,
                                           firstname,
                                           surname,
                                           authorName,
                                           cristinId
                            ))
                   .collect(Collectors.toList());
    }

    private Author generateAuthor(String externalId,
                                  String firstname,
                                  String surname,
                                  String authorName,
                                  int cristinId) {
        var author = new Author();
        author.setExternalId(externalId);
        author.setFirstname(firstname);
        author.setSurname(surname);
        author.setAuthorName(authorName);
        author.setOrcid(generateRandomOrcid());
        author.setCristinId(cristinId);
        author.setSequenceNr(randomInteger());
        author.setPublication(generateRandomPublication());
        return author;
    }

    private String generateRandomOrcid() {
        return randomBoolean() ? randomString() : null;
    }

    private Publication generateRandomPublication() {
        var publication = new Publication();
        publication.setSourceCode(SOURCE_CODE);
        publication.setExternalId(randomString());
        return publication;
    }

    class Author {

        Publication publication;
        String externalId;
        int cristinId;
        int sequenceNr;
        String surname;
        String firstname;
        String authorName;
        String orcid;

        public Publication getPublication() {
            return publication;
        }

        public void setPublication(Publication publication) {
            this.publication = publication;
        }

        public String getExternalId() {
            return externalId;
        }

        public void setExternalId(String externalId) {
            this.externalId = externalId;
        }

        public int getCristinId() {
            return cristinId;
        }

        public void setCristinId(int cristinId) {
            this.cristinId = cristinId;
        }

        public int getSequenceNr() {
            return sequenceNr;
        }

        public void setSequenceNr(int sequenceNr) {
            this.sequenceNr = sequenceNr;
        }

        public String getSurname() {
            return surname;
        }

        public void setSurname(String surname) {
            this.surname = surname;
        }

        public String getFirstname() {
            return firstname;
        }

        public void setFirstname(String firstname) {
            this.firstname = firstname;
        }

        public String getAuthorName() {
            return authorName;
        }

        public void setAuthorName(String authorName) {
            this.authorName = authorName;
        }

        public String getOrcid() {
            return orcid;
        }

        public void setOrcid(String orcid) {
            this.orcid = orcid;
        }
    }

    class Publication {

        String sourceCode;
        String externalId;

        public String getExternalId() {
            return externalId;
        }

        public void setExternalId(String externalId) {
            this.externalId = externalId;
        }

        public String getSourceCode() {
            return sourceCode;
        }

        public void setSourceCode(String sourceCode) {
            this.sourceCode = sourceCode;
        }
    }
}
