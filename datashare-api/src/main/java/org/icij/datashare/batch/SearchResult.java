package org.icij.datashare.batch;

import java.nio.file.Path;
import java.util.Date;
import java.util.Objects;

public class SearchResult {
    public final String query;
    public final String documentId;
    public final String rootId;
    public final Path documentPath;
    public final Date creationDate;
    public final int documentNumber;

    public SearchResult(String query, String documentId, String rootId, Path documentPath, Date creationDate, int documentNumber) {
        this.query = query;
        this.documentId = documentId;
        this.rootId = rootId;
        this.documentPath = documentPath;
        this.creationDate = creationDate;
        this.documentNumber = documentNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchResult that = (SearchResult) o;
        return query.equals(that.query) &&
                documentId.equals(that.documentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, documentId);
    }

    @Override
    public String toString() {
        return "SearchResult{" +
                "query='" + query + '\'' +
                ", documentId='" + documentId + '\'' +
                '}';
    }
}
