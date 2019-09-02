package org.icij.datashare.db;


import org.icij.datashare.text.Document;
import org.icij.datashare.text.NamedEntity;
import org.icij.datashare.text.nlp.Pipeline;
import org.icij.datashare.user.User;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.fest.assertions.Assertions.assertThat;
import static org.icij.datashare.text.Language.*;
import static org.icij.datashare.text.NamedEntity.Category.PERSON;
import static org.icij.datashare.text.Project.project;
import static org.icij.datashare.text.Tag.tag;
import static org.icij.datashare.text.nlp.Pipeline.Type.*;

@RunWith(Parameterized.class)
public class JooqRepositoryTest {
    @Rule
    public DbSetupRule dbRule;
    private JooqRepository repository;

    @Parameters
    public static Collection<Object[]> dataSources() {
        return asList(new Object[][]{
                {new DbSetupRule("jdbc:sqlite:file:memorydb.db?mode=memory&cache=shared")},
                {new DbSetupRule("jdbc:postgresql://postgresql/test?user=test&password=test")}
        });
    }

    public JooqRepositoryTest(DbSetupRule rule) {
        dbRule = rule;
        repository = rule.createRepository();
    }

    @Test
    public void test_create_document() throws Exception {
        Document document = new Document("id", project("prj"), Paths.get("/path/to/doc"), "content",
                FRENCH, Charset.defaultCharset(),
                "text/plain", new HashMap<String, Object>() {{
                    put("key 1", "value 1");
                    put("key 2", "value 2");
                }},
                Document.Status.INDEXED, Pipeline.set(CORENLP, OPENNLP), 432L);

        repository.create(document);

        Document actual = repository.getDocument(document.getId());
        assertThat(actual).isEqualTo(document);
        assertThat(actual.getMetadata()).isEqualTo(document.getMetadata());
        assertThat(actual.getNerTags()).isEqualTo(document.getNerTags());
        assertThat(actual.getExtractionDate()).isEqualTo(document.getExtractionDate());
        assertThat(actual.getProject()).isEqualTo(project("prj"));
    }

    @Test
    public void test_get_untagged_documents() throws Exception {
        Document coreAndOpenNlp = new Document("idCore", project("prj"), Paths.get("/path/to/coreAndOpenNlp"), "coreAndOpenNlp",
                FRENCH, Charset.defaultCharset(),
                "text/plain", new HashMap<>(),
                Document.Status.INDEXED, Pipeline.set(CORENLP, OPENNLP), 432L);
        Document ixaPipe = new Document("idIxa", project("prj"), Paths.get("/path/to/ixaPipe"), "ixaPipe",
                FRENCH, Charset.defaultCharset(),
                "text/plain", new HashMap<>(),
                Document.Status.INDEXED, Pipeline.set(IXAPIPE), 234L);
        repository.create(coreAndOpenNlp);
        repository.create(ixaPipe);

        assertThat(repository.getDocumentsNotTaggedWithPipeline(project("prj"), IXAPIPE)).containsExactly(coreAndOpenNlp);
        assertThat(repository.getDocumentsNotTaggedWithPipeline(project("prj"), CORENLP)).containsExactly(ixaPipe);
        assertThat(repository.getDocumentsNotTaggedWithPipeline(project("prj"), MITIE)).containsExactly(coreAndOpenNlp, ixaPipe);
    }

    @Test
    public void test_create_named_entity_list() {
        List<NamedEntity> namedEntities = Arrays.asList(
                NamedEntity.create(PERSON, "mention 1", 123, "doc_id", CORENLP, GERMAN),
                NamedEntity.create(PERSON, "mention 2", 321, "doc_id", CORENLP, ENGLISH));

        repository.create(namedEntities);

        NamedEntity actualNe1 = repository.getNamedEntity(namedEntities.get(0).getId());
        assertThat(actualNe1).isEqualTo(namedEntities.get(0));
        assertThat(actualNe1.getCategory()).isEqualTo(PERSON);
        assertThat(actualNe1.getExtractor()).isEqualTo(CORENLP);
        assertThat(actualNe1.getExtractorLanguage()).isEqualTo(GERMAN);

        assertThat(repository.getNamedEntity(namedEntities.get(1).getId())).isEqualTo(namedEntities.get(1));
    }

    @Test
    public void test_star_unstar_a_document_with_join() {
        Document doc = new Document("id", project("prj"), Paths.get("/path/to/docId"), "my doc",
                        FRENCH, Charset.defaultCharset(),
                        "text/plain", new HashMap<>(),
                        Document.Status.INDEXED, Pipeline.set(CORENLP, OPENNLP), 432L);
        repository.create(doc);
        User user = new User("userid");

        assertThat(repository.star(user, doc.getId())).isTrue();
        assertThat(repository.getStarredDocuments(user)).contains(doc);
        assertThat(repository.star(user, doc.getId())).isFalse();

        assertThat(repository.unstar(user, doc.getId())).isTrue();
        assertThat(repository.getStarredDocuments(user)).isEmpty();
        assertThat(repository.unstar(user, doc.getId())).isFalse();
    }

    @Test
    public void test_star_unstar_a_document_without_documents() {
        User user = new User("userid");

        assertThat(repository.star(project("prj"), user, "doc_id")).isTrue();
        assertThat(repository.getStarredDocuments(project("prj"), user)).contains("doc_id");
        assertThat(repository.getStarredDocuments(project("prj2"), user)).isEmpty();
        assertThat(repository.star(project("prj"), user, "doc_id")).isFalse();

        assertThat(repository.unstar(project("prj"), user,"doc_id")).isTrue();
        assertThat(repository.getStarredDocuments(project("prj"), user)).isEmpty();
        assertThat(repository.unstar(project("prj"), user, "doc_id")).isFalse();
    }

    @Test
    public void test_group_star_unstar_a_document_without_documents() {
        User user = new User("userid");

        assertThat(repository.star(project("prj"), user, asList("id1", "id2", "id3"))).isEqualTo(3);
        assertThat(repository.getStarredDocuments(project("prj"), user)).contains("id1", "id2", "id3");
        assertThat(repository.getStarredDocuments(project("prj2"), user)).isEmpty();

        assertThat(repository.unstar(project("prj"), user,asList("id1", "id2"))).isEqualTo(2);
        assertThat(repository.unstar(project("prj"), user, singletonList("id3"))).isEqualTo(1);
        assertThat(repository.getStarredDocuments(project("prj"), user)).isEmpty();
    }

    @Test
    public void test_tag_untag_a_document_without_documents() {
        assertThat(repository.tag(project("prj"), "doc_id", tag("tag1"), tag("tag2"))).isTrue();
        assertThat(repository.getDocuments(project("prj"), tag("tag1"))).containsExactly("doc_id");
        assertThat(repository.getDocuments(project("prj"), tag("tag1"))).containsExactly("doc_id");
        assertThat(repository.getDocuments(project("prj"), tag("tag1"), tag("tag2"))).containsExactly("doc_id");

        assertThat(repository.getDocuments(project("prj2"), tag("tag1"))).isEmpty();
        assertThat(repository.tag(project("prj"), "doc_id", tag("tag1"))).isFalse();

        assertThat(repository.untag(project("prj"), "doc_id", tag("tag1"), tag("tag2"))).isTrue();
        assertThat(repository.getDocuments(project("prj"), tag("tag1"))).isEmpty();
        assertThat(repository.untag(project("prj"), "doc_id", tag("tag1"))).isFalse();
    }

    @Test
    public void test_delete_all_project() {
        User user = new User("userid");
        repository.star(project("prj"), user, "doc_id");
        repository.tag(project("prj"), "doc_id", tag("tag1"), tag("tag2"));

        assertThat(repository.deleteAll("prj")).isTrue();
        assertThat(repository.deleteAll("prj")).isFalse();

        assertThat(repository.getDocuments(project("prj"), tag("tag1"), tag("tag2"))).isEmpty();
        assertThat(repository.getStarredDocuments(user)).isEmpty();
    }
}
