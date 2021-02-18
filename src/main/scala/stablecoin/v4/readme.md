# Updatable AgeUSD Protocol Informal Specification v4.0

This is an informal specification of the AgeUSD protocol v4.0, which is an enhancement to the [v3.0 specification](../v3/readme.md).

## Differences From v2.0
- No update protocol (updated moved to oracle-pool)
- New oracle Token NFT (oracle-pool contract is now updatable)
- Additional check in exchange for r4, r5 of bank output box to be > 0
- Test deployment parameters (new token Ids for SC and RC, new max tokens) 

## Deployment Params

Initial token Ids and amounts: (obtained via JDE)

```json
{
  "dataInputBoxIds" : [ "9f5a1cbbaba1fa972aafecd8ad36196cb53dc1c857ab29ab39dcf05dd3de5a1c" ],
  "inputBoxIds" : [ "93829e6ae50b13454c29c8e1d6a4307845b6b6d3f5996574f7d1c52060591469" ],
  "inputNanoErgs" : 1000000,
  "inputTokens" : [ [ "1c648386f0529cc44c367cf941ede13533223f97b4560a0b31fa3ef668c3be62", 1 ], [ "22d27990f062c259d2fabdbf689c5e2721ebbbd789b84b65c5f44b546915dab6", 100000000000 ], [ "bf1fa3e4aeb92eb48b3a2d37646c28ebc774df9eee8061696c5c63bf6c4a2264", 9999999999700 ] ],
  "outputs" : [ ],
  "returned" : [ {
    "name" : "scToken",
    "value" : "bf1fa3e4aeb92eb48b3a2d37646c28ebc774df9eee8061696c5c63bf6c4a2264"
  }, {
    "name" : "rcToken",
    "value" : "22d27990f062c259d2fabdbf689c5e2721ebbbd789b84b65c5f44b546915dab6"
  }, {
    "name" : "bankNFT",
    "value" : "1c648386f0529cc44c367cf941ede13533223f97b4560a0b31fa3ef668c3be62"
  }, {
    "name" : "scTokenIn",
    "value" : "9999999999700"
  }, {
    "name" : "rcTokenIn",
    "value" : "100000000000"
  }, {
    "name" : "scCircIn",
    "value" : "0"
  }, {
    "name" : "rcCircIn",
    "value" : "0"
  }, {
    "name" : "bcReserveIn",
    "value" : "1000000"
  }, {
    "name" : "scCircDelta",
    "value" : "10"
  }, {
    "name" : "bcDeltaWithFee",
    "value" : "46976736"
  }, {
    "name" : "scNominalPrice",
    "value" : "4651162"
  }, {
    "name" : "rate",
    "value" : "4651162"
  } ]
```

Symbols used for compilation (for ErgoScriptCompiler)

```json
{
  "symbols":[
    {
      "name":"minReserveRatioPercent",
      "type":"Long",
      "value":"400"
    },
    {
      "name":"defaultMaxReserveRatioPercent",
      "type":"Long",
      "value":"800"
    },
    {
      "name":"coolingOffHeight",
      "type":"Int",
      "value":"430000"
    },
    {
      "name":"rcDefaultPrice",
      "type":"Long",
      "value":"1000000"
    },
    {
      "name":"feePercent",
      "type":"Int",
      "value":"1"
    },
    {
      "name":"INF",
      "type":"Long",
      "value":"1000000000"
    },
    {
      "name":"longMax",
      "type":"Long",
      "value":"9223372036854775807"
    },
    {
      "name":"minStorageRent",
      "type":"Long",
      "value":"10000000"
    },
    {
      "name":"oraclePoolNFT",
      "type":"CollByte",
      "value":"54acaa0c6d5d3bc66b88364a423b5f156ed763f7236d437adb44d70787bc0f95"
    }
  ]
}
```