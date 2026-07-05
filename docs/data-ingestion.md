# HomeSafe Data Ingestion

## Supported Providers

### DIM.RIA

- Provider code: `DIM_RIA`
- Developer portal: `https://developers.ria.com/`
- Environment variable: `DIM_RIA_API_KEY`
- Purpose: Ukrainian rental listings
- Authentication: API key query parameter

DIM.RIA location identifiers must be taken from the official locations API. A sync request accepts `cityId`, `stateId`, `limit`, and `page`.

### OLX

- Provider code: `OLX`
- Base URL: `https://www.olx.ua`
- Purpose: public long-term apartment rental listings imported through page parsing
- Authentication: not used in the current parser flow

The current OLX integration does not use the partner-account adverts API. Instead, it uses:

- the OLX public search page for long-term apartment rent by city;
- public listing detail pages;
- embedded structured data (`application/ld+json`) and visible page parameters from the HTML.

An OLX sync request accepts `city`, `district`, `limit`, and `page`. The source config maps supported cities to OLX path slugs, for example `Київ -> kiev`.

Important: the official OLX partner API does not provide public marketplace search in the supplied specification, so HomeSafe uses a separate public parser flow for OLX instead of the partner-account adverts API.

Provider paths are stored in the `DataSource.config` JSON field. They can be updated without changing the common ingestion pipeline if an upstream page structure changes.

## Processing Pipeline

1. Fetch listings from a provider API or public page source.
2. Save the original provider payload in `ExternalListing`.
3. Normalize fields into the HomeSafe apartment format.
4. Generate a stable address, room, and area fingerprint.
5. Detect cross-provider duplicates.
6. Calculate the market median for the same city, district, room count, and currency.
7. Calculate Trust, Value, Comfort, and overall Quality scores.
8. Assign `PUBLISHED`, `REVIEW`, or `REJECTED`.
9. Save price changes in `PriceHistory`.
10. Store counters and errors in `ImportRun`.

Only `PUBLISHED` apartments are available through the public apartment and favorites APIs.

## Scoring Rules

Trust Score considers:

- source reliability;
- confirmed owner status;
- source URL;
- photo count;
- description completeness;
- geographic coordinates;
- cross-source duplicates;
- suspicious difference from the segment median.

Value Score compares the listing price with the median of matching apartments. It never compares different currencies or unrelated cities.

Comfort Score uses infrastructure distances when available. Without infrastructure enrichment, the score remains conservative and does not claim unavailable evidence.

The overall quality score uses the weights:

- Trust: 50%
- Value: 30%
- Comfort: 20%

## Administrator API

- `GET /api/admin/sources`
- `PATCH /api/admin/sources/:code`
- `POST /api/admin/sources/:code/sync`
- `GET /api/admin/imports`
- `GET /api/admin/listings/review`
- `PATCH /api/admin/listings/:id/status`

All endpoints require an approved administrator session.

## Automatic Synchronization

If scheduled ingestion is enabled in a future iteration, the targets should follow the same normalized query structure, for example:

```env
INGESTION_ENABLED="true"
INGESTION_INTERVAL_MINUTES="60"
INGESTION_TARGETS_JSON='[{"code":"DIM_RIA","query":{"cityId":10,"stateId":10,"limit":6}},{"code":"OLX","query":{"city":"Київ","limit":6}}]'
```

The minimum scheduler interval should remain conservative, and provider quotas and terms must be respected.
