package no.sikt.nva.scopus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.scopus.generated.*;
import no.sikt.nva.scopus.factory.ScopusAuthorFactory;
import no.sikt.nva.scopus.factory.ScopusChannelFactory;
import no.sikt.nva.scopus.factory.ScopusLanguageFactory;
import no.sikt.nva.scopus.model.Author;
import no.sikt.nva.scopus.model.Channel;
import no.sikt.nva.scopus.model.Language;
import nva.commons.core.JacocoGenerated;

public class ScopusPublication {

    private Integer pubId;
    private final String sourceName;
    private final String externalId;
    private final String externalCategory;
    private Integer varbeidlopenr;
    private final Short yearPublished;
    private Date dateOpenAccess;
    private String statusOpenAccess;
    private String licenseOpenAccess;
    private List<Author> authors;
    private final Author correspondingAuthor;
    private Map<String, String> alternativeIds;
    private Map<String, String> copyrights;
    private List<Language> languages;
    private final Channel channel;
    private Date dateReceivedFromSource;

    public ScopusPublication(DocTp docTp) {
        authors = new ArrayList<>();
        alternativeIds = new HashMap<>();
        languages = new ArrayList<>();
        copyrights = new HashMap<>();
        sourceName = "SCOPUS";
        externalCategory = buildCategoryCode(docTp.getItem().getItem().getBibrecord().getHead().getCitationInfo().getCitationType());
        externalId = docTp.getMeta().getEid();
        yearPublished = docTp.getMeta().getPubYear() != null ? Short.valueOf(docTp.getMeta().getPubYear()) : null;
        buildOpenAccessData(docTp.getMeta().getOpenAccess());
        authors = ScopusAuthorFactory.buildAuthors(docTp.getItem().getItem().getBibrecord().getHead().getAuthorGroup());
        var correspondence = docTp.getItem().getItem().getBibrecord().getHead().getCorrespondence();
        correspondingAuthor = ScopusAuthorFactory.buildCorrespondingAuthor(correspondence.size() >= 1? correspondence.get(0): null);
        ScopusAuthorFactory.tryToFindCorrespondingAuthorExternalIds(correspondingAuthor, authors);
        alternativeIds = buildAlternativeIds(docTp.getItem().getItem().getBibrecord().getItemInfo().getItemidlist());
        copyrights = buildCopyrights(docTp.getItem().getItem().getBibrecord().getItemInfo().getCopyright());
        languages = ScopusLanguageFactory.buildLanguages(docTp);
        channel = ScopusChannelFactory.buildChannel(docTp);
    }

    private String buildCategoryCode(List<CitationTypeTp> citationTypeTps) {

        if (citationTypeTps != null && citationTypeTps.size() > 0) {
            return citationTypeTps.get(0).getCode().value();
        }
        return null;
    }

    private void buildOpenAccessData(OpenAccessType openAccessType) {

        if (openAccessType == null) return;

        try {
            if (openAccessType.getOaAccessEffectiveDate() != null) {
                dateOpenAccess = new SimpleDateFormat("yyyy-MM-dd").parse(openAccessType.getOaAccessEffectiveDate());
            }
        } catch (Exception ex) {}

        licenseOpenAccess = openAccessType.getOaUserLicense();

        if (openAccessType.getOaArticleStatus() != null) {
            statusOpenAccess = openAccessType.getOaArticleStatus().getValue();
        }
    }


    private Map<String, String> buildAlternativeIds(ItemidlistTp itemidlistTps) {

        if (itemidlistTps == null) return null;

        Map<String, String> alternativeIds = new HashMap<>();

        if (itemidlistTps.getDoi() != null) {
            alternativeIds.put("DOI",itemidlistTps.getDoi());
        }
        if (itemidlistTps.getErn() != null) {
            alternativeIds.put("ERN",itemidlistTps.getErn());
        }
        if (itemidlistTps.getPii() != null) {
            alternativeIds.put("PII",itemidlistTps.getPii());
        }

        itemidlistTps.getItemid().stream().forEach(itemidTp -> {
            if (itemidTp.getIdtype() != null && itemidTp.getValue() != null) {
                alternativeIds.put(itemidTp.getIdtype(),itemidTp.getValue());
            }
        });

        return alternativeIds;
    }


    private Map<String,String> buildCopyrights(List<CopyrightTp> copyrightTps) {

        if (copyrightTps == null || copyrightTps.isEmpty()) return null;

        Map<String,String> copyrights = new HashMap<>();

        copyrightTps.stream().forEach(copyrightTp -> {
            if (copyrightTp.getType() != null) {
                copyrights.putIfAbsent(copyrightTp.getType(),copyrightTp.getContent());
            }
        });

        return copyrights;
    }

    @JacocoGenerated
    public Integer getPubId() {
        return pubId;
    }


    @JacocoGenerated
    public String getSourceName() {
        return sourceName;
    }


    @JacocoGenerated
    public String getExternalId() {
        return externalId;
    }


    @JacocoGenerated
    public String getExternalCategory() {
        return externalCategory;
    }

    @JacocoGenerated
    public Integer getVarbeidlopenr() { return varbeidlopenr; }

    @JacocoGenerated
    public Short getYearPublished() {
        return yearPublished;
    }


    @JacocoGenerated
    public Date getDateOpenAccess() {
        return dateOpenAccess;
    }

    @JacocoGenerated
    public String getStatusOpenAccess() {
        return statusOpenAccess;
    }

    @JacocoGenerated
    public String getLicenseOpenAccess() {
        return licenseOpenAccess;
    }


    @JacocoGenerated
    public List<Author> getAuthors() {
        return authors;
    }

    @JacocoGenerated
    public Author getCorrespondingAuthor() {
        return correspondingAuthor;
    }


    @JacocoGenerated
    public Map<String, String> getAlternativeIds() {
        return alternativeIds;
    }

    @JacocoGenerated
    public Map<String, String> getCopyrights() {
        return copyrights;
    }

    @JacocoGenerated
    public List<Language> getLanguages() {
        return languages;
    }

    @JacocoGenerated
    public Channel getChannel() {
        return channel;
    }

    @JacocoGenerated
    public Date getDateReceivedFromSource() { return dateReceivedFromSource; }


}
