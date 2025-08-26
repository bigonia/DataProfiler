package com.dataprofiler.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Simplified pagination response wrapper
 * Provides clean pagination metadata without Spring's complex structure
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimplePaginationResponse<T> {
    
    /**
     * The actual content/data for current page
     */
    private List<T> content;
    
    /**
     * Current page number (0-based)
     */
    private int page;
    
    /**
     * Number of items per page
     */
    private int pageSize;
    
    /**
     * Total number of elements across all pages
     */
    private long totalElements;
    
    /**
     * Total number of pages
     */
    private int totalPages;
    
    /**
     * Whether this is the first page
     */
    private boolean first;
    
    /**
     * Whether this is the last page
     */
    private boolean last;
    
    /**
     * Number of elements in current page
     */
    private int numberOfElements;
    
    /**
     * Whether the current page is empty
     */
    private boolean empty;
    
    /**
     * Create SimplePaginationResponse from Spring Page object
     * 
     * @param page Spring Page object
     * @param <T> Content type
     * @return SimplePaginationResponse instance
     */
    public static <T> SimplePaginationResponse<T> from(org.springframework.data.domain.Page<T> page) {
        SimplePaginationResponse<T> response = new SimplePaginationResponse<>();
        response.setContent(page.getContent());
        response.setPage(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setFirst(page.isFirst());
        response.setLast(page.isLast());
        response.setNumberOfElements(page.getNumberOfElements());
        response.setEmpty(page.isEmpty());
        return response;
    }
}