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
- Environment variable: `OLX_API_KEY`
- Purpose: adverts of the authorized OLX partner account
- Authentication: `Authorization: Bearer <access_token>` and `Version: 2.0`

The provided `partner_api.yaml` documents these relevant endpoints:

- `POST /api/open/oauth/token`
- `GET /api/partner/adverts`
- `GET /api/partner/cities/{cityId}`
- `GET /api/partner/cities/{cityId}/districts`

HomeSafe currently reads `OLX_API_KEY` as an already issued bearer access token. A sync request accepts `city`, `district`, `limit`, `offset`, `page`, and `category_ids`.

Important: this partner API does not provide public search over the entire OLX marketplace in the supplied specification. It returns adverts available to the authorized partner account, so OLX is useful here as a second managed source, not as a full public aggregator feed.

Provider paths are stored in the `DataSource.config` JSON field. They can be updated without changing the common ingestion pipeline if an upstream API version changes.

## Processing Pipeline

1. Fetch authorized listings from a provider API.
2. Save the original provider JSON in `ExternalListing`.
3. Normalize fields into the HomeSafe apartment format.
4. Generate a stable address/room/area fingerprint.
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

Set the following variables only after API keys and provider targets are configured:

```env
INGESTION_ENABLED="true"
INGESTION_INTERVAL_MINUTES="60"
INGESTION_TARGETS_JSON='[{"code":"DIM_RIA","query":{"cityId":0,"stateId":0,"limit":20}},{"code":"OLX","query":{"city":"Львів","district":"Франківський","limit":20}}]'
```

The minimum scheduler interval is 15 minutes. Provider quotas and terms must be respected.
