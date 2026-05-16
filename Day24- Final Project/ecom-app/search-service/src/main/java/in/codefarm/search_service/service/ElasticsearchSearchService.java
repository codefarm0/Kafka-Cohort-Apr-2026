package in.codefarm.search_service.service;

import in.codefarm.search_service.dto.SearchHitDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only search against Elasticsearch product index (HLD 3.3).
 * <p>
 * Documents from Debezium → Kafka Connect are <strong>envelopes</strong>: row columns live under
 * {@code after}, not at the root of {@code _source}. Queries and parsing support {@code after.*}
 * with fallback to flat documents (e.g. reindexed or alias-only pipelines).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchSearchService {

    /** ES field paths when row data is nested under Debezium's {@code after} object. */
    private static final List<String> SEARCH_FIELDS_AFTER = List.of(
        "after.product_name^2",
        "after.description",
        "after.product_id",
        "after.sku"
    );

    /** Same fields for flat {@code _source} (no envelope). */
    private static final List<String> SEARCH_FIELDS_FLAT = List.of(
        "product_name^2",
        "description",
        "product_id",
        "sku"
    );

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${search.elasticsearch.url:http://localhost:9200}")
    private String elasticsearchUrl;

    @Value("${search.elasticsearch.index:mysql-product-server.product_db.products}")
    private String indexName;

    /** Prefer Debezium shape ({@code after.*}); set false only if the index has flat documents. */
    @Value("${search.elasticsearch.debezium-envelope:true}")
    private boolean debeziumEnvelope;

    public List<SearchHitDto> search(String q, String category, int from, int size) {
        String url = elasticsearchUrl.replaceAll("/$", "") + "/" + indexName + "/_search";
        Map<String, Object> body = new HashMap<>();
        body.put("from", from);
        body.put("size", size);

        List<String> fields = debeziumEnvelope ? SEARCH_FIELDS_AFTER : SEARCH_FIELDS_FLAT;
        String categoryField = debeziumEnvelope ? "after.category_id" : "category_id";

        Map<String, Object> bool = new HashMap<>();
        List<Map<String, Object>> must = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            Map<String, Object> mm = new HashMap<>();
            mm.put("query", q);
            mm.put("fields", fields);
            mm.put("type", "best_fields");
            must.add(Map.of("multi_match", mm));
        } else {
            must.add(Map.of("match_all", Map.of()));
        }
        bool.put("must", must);
        if (category != null && !category.isBlank()) {
            List<Map<String, Object>> filter = new ArrayList<>();
            filter.add(Map.of("match", Map.of(categoryField, category)));
            bool.put("filter", filter);
        }
        body.put("query", Map.of("bool", bool));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String json;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.debug("ES query: {}", json);

        try {
            String response = restTemplate.postForObject(url, new HttpEntity<>(json, headers), String.class);
            log.debug("ES response: {}", response);
            return parseHits(response);
        } catch (Exception e) {
            log.error("Elasticsearch search failed: {}", e.getMessage());
            throw new RuntimeException("Search failed: " + e.getMessage(), e);
        }
    }

    private List<SearchHitDto> parseHits(String responseJson) throws Exception {
        List<SearchHitDto> out = new ArrayList<>();
        if (responseJson == null) {
            return out;
        }
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode hits = root.path("hits").path("hits");
        if (!hits.isArray()) {
            return out;
        }
        for (JsonNode h : hits) {
            JsonNode source = h.path("_source");
            if (source.isMissingNode()) {
                continue;
            }
            JsonNode row = rowPayload(source);
            String productId = text(row, "product_id");
            String name = text(row, "product_name");
            String desc = text(row, "description");
            SearchHitDto dto = SearchHitDto.builder()
                .productId(productId)
                .sku(text(row, "sku"))
                .name(name)
                .description(desc)
                .categoryId(text(row, "category_id"))
                .price(safeDecimal(row, "price"))
                .availableQuantity(intVal(row, "available_quantity"))
                .snippet(desc != null && desc.length() > 120 ? desc.substring(0, 117) + "..." : desc)
                .build();
            out.add(dto);
        }
        log.debug("ES result after parsing: {} hits", out.size());
        return out;
    }

    /**
     * Debezium CDC envelopes put the table row under {@code after}; flattened docs use root.
     */
    private static JsonNode rowPayload(JsonNode source) {
        JsonNode after = source.get("after");
        if (after != null && after.isObject() && !after.isEmpty()) {
            return after;
        }
        return source;
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static BigDecimal safeDecimal(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        if (v.isNumber()) {
            return BigDecimal.valueOf(v.asDouble());
        }
        String s = v.asText();
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            // Debezium may emit DECIMAL as non-decimal string when encoded; omit rather than fail search
            log.trace("Skip non-numeric price field {}: {}", field, s);
            return null;
        }
    }

    private static Integer intVal(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        if (v.isIntegralNumber()) {
            return v.asInt();
        }
        try {
            return Integer.parseInt(v.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
