package stablecoin.v5

import org.scalatest.PropSpec

class BankContractSpec extends PropSpec {

  property("Bank Address check") {
    println("BankAddress: " + BankContract.address)
    assert(
      BankContract.address == "29irJ65SHH5VxgQaXubC1z9eHzutUWV6BB2QGCbA9eQ53msbyzXdh6bSXm64WMwkiBNRgeXZy1ySSvVuKtEE4ifUbdXiV4PtdqsdxWxrhmJoqnDHSimsCz2zLsUGFm9XybyQ3QdV9jmhdFfM1PAWN3sLSj9uPqdAEtrPLYuCuFXYvW73mXDUGTYwebgG4HUPTbsr3B9Pp95HXQ7H6mb8yN5oqfjLk8fJhmKmq4o3fhirnpipht9ceon4mAzD9u8gkQUsEThRqRqZPB4tomoBmFY2rxe6KQPbtc9aS3VC6A8hnvRWnBWsnKCz6rpW3frg6jdB2pygJDPMubqpn9zy4iu6wDtB7YyTXDFkUShr5gsT4Q3v7ht7xzhykXLdaJitDhFockmfkQPWnyYUg7xtZAAj2rMTRHVXdXKvkVXMoFBSC9dX57aqe2Y3yLkP8jxxf8oSo98PFA5zRuZhQYgFmH6kSFGruuxLAw8FJYC4ffyzB3VRSsjzSQ85FkZwshC29CnF6Lwxj4Rq4cr9UKbsRJdYxKVKjuUBp28AgqEDNEqm3Lk22xo44pY7Jn3QhXS3CLtwqi2woDHAVXv8rjYsmWh5tnD1e52tFoA1YyXoEn3T2sZVGzvcZWJ4Y2Hb2jSH91NXHRjmM4zAcXiA24mGmXBL34PHzpnFKAoSe5yavYbW879wDdefeeX42ZVETsQHe7iHHw8T2BytSG5v5xdo1yxzaxwZP92E3p4M1Em2LDQDMr8zfWdEzJNrSiDY7MngqHa3Ds1NPNSL4xeo4ni7Ejy7LMFAdzugJDiNhqo35cKTqB7k31gbJ7Tab1w9aVcbQAQf1e3KPEEHG4Go797MN8s5oHGkTDkzVqSz5me3SUnoany1V3XXzNcApxuJMNxZosYgFJAR2huTSUcQPpCWp3ucbFTQQNcC1M3RXTctN1pnkCNpvjhoGBTtncsHSqcmtb31eQp2bX1JcGCRXSvcav6B5BqYw5jQ2WAX6Gu3xYLwS")
  }

}
