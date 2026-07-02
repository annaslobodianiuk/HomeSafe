package ua.homesafe.integration;

import ua.homesafe.model.DataSourceEntity;

import java.util.List;

public interface ProviderClient {
    boolean supports(String providerCode);
    List<NormalizedListing> fetchListings(DataSourceEntity source, ImportQuery query);
}
