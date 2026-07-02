package ua.homesafe.integration;

public record ImportQuery(
    String city,
    String district,
    String state,
    Integer offset,
    String categoryIds,
    Integer cityId,
    Integer stateId,
    Integer limit,
    Integer page
) {
    public int safeLimit() {
        return limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
    }

    public int safePage() {
        return page == null || page < 0 ? 0 : page;
    }

    public int safeOffset() {
        return offset == null || offset < 0 ? safePage() * safeLimit() : offset;
    }
}
