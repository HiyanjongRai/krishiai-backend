package com.krishiai.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursorPageResponse<T> {
    private List<T> items;
    private Long nextCursor;
    private boolean hasMore;
    private int limit;
}
