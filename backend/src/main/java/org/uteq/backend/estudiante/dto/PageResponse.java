package org.uteq.backend.estudiante.dto;

import org.springframework.data.domain.Page;
import java.io.Serializable;
import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int current_page,
        long total,
        int last_page,
        int size
) implements Serializable {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getSize()
        );
    }
}