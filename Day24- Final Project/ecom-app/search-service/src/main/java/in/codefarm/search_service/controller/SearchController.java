package in.codefarm.search_service.controller;

import in.codefarm.search_service.dto.SearchHitDto;
import in.codefarm.search_service.service.ElasticsearchSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final ElasticsearchSearchService searchService;

    @GetMapping
    public ResponseEntity<List<SearchHitDto>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safeFrom = Math.max(from, 0);
        return ResponseEntity.ok(searchService.search(q, category, safeFrom, safeSize));
    }
}
