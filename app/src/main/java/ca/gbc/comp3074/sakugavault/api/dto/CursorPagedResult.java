package ca.gbc.comp3074.sakugavault.api.dto;

import java.util.List;

/** A page of results plus a cursor for the next page (Contracts/Common). */
public class CursorPagedResult<T> {
    public List<T> items;
    public String nextCursor;
    public int pageSize;
    public boolean hasMore;
}
