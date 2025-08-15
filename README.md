# DeFi Yield Aggregator
target: 2-week MVP. backend=Java Spring Boot; frontend=React.

## API

### GET /portfolio/{address}
Response:
{
  "address": "0x...",
  "totalUsd": 12345.67,
  "positions": [
    {"protocol":"Aave","network":"ethereum","asset":"DAI","amount":1000,"usdValue":1000,"apr":0.045,"riskStatus":"OK"}
  ],
  "lastUpdatedIso": "2025-08-14T08:00:00Z"
}

### GET /alerts/{address}
Response:
{
  "address": "0x...",
  "alerts": [
    {"type":"HEALTH_FACTOR_LOW","message":"...","protocol":"Aave","createdIso":"2025-08-14T08:00:00Z"}
  ]
}
