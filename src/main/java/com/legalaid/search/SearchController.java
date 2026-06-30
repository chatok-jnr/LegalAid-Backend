package com.legalaid.search;

import com.legalaid.common.response.ApiResponse;
import com.legalaid.search.dto.SearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    // ── GET /api/search ───────────────────────────────────────
    // Public — no auth required
    //
    // Query params:
    //   q           — search term (matched against category/title)
    //   type        — ALL (default) | LAWYER | SERVICE
    //   category    — practice area or service category
    //   location    — city name
    //   price_min   — minimum service price (SERVICE only)
    //   price_max   — maximum service price (SERVICE only)
    //   rating      — minimum lawyer rating
    //   deliveryDays — max delivery days (SERVICE only)
    //   sort        — relevance (default) | rating | price_asc | price_desc
    //   page        — default 0
    //   size        — default 12, max 50
    @GetMapping
    public ResponseEntity<ApiResponse<SearchResponse>> search(
            @RequestParam(required = false)                  String     q,
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @RequestParam(required = false)                  String     category,
            @RequestParam(required = false)                  String     location,
            @RequestParam(required = false)                  BigDecimal price_min,
            @RequestParam(required = false)                  BigDecimal price_max,
            @RequestParam(required = false)                  Double     rating,
            @RequestParam(required = false)                  Integer    deliveryDays,
            @RequestParam(required = false)                  String     sort,
            @RequestParam(defaultValue = "0")                int        page,
            @RequestParam(defaultValue = "12")               int        size) {

        ApiResponse<SearchResponse> body = ApiResponse.success(
                searchService.search(
                        q, type, category, location,
                        price_min, price_max, rating,
                        deliveryDays, sort, page, size)
        );
        return ResponseEntity.ok(body);
    }
}