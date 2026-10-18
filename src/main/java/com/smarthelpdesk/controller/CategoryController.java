package com.smarthelpdesk.controller;

import com.smarthelpdesk.dto.CategoryResponse;
import com.smarthelpdesk.model.Category;
import com.smarthelpdesk.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for retrieving available ticket categories.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final TicketService ticketService;

    public CategoryController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> listCategories() {
        List<Category> categories = ticketService.listCategories();
        List<CategoryResponse> response = categories.stream()
                .map(CategoryResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
