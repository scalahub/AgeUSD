package stablecoin.v4

private[v4] object Contracts {
  val oraclePoolNFT = "54acaa0c6d5d3bc66b88364a423b5f156ed763f7236d437adb44d70787bc0f95"

  val bankNFT = "7bd873b8a886daa7a8bfacdad11d36aeee36c248aaf5779bcd8d41a13e4c1604" // quantity 1
  val scToken = "bf1fa3e4aeb92eb48b3a2d37646c28ebc774df9eee8061696c5c63bf6c4a2264" // quantity 9999999999700
  val rcToken = "22d27990f062c259d2fabdbf689c5e2721ebbbd789b84b65c5f44b546915dab6" // quantity 100000000000

  val rcDefaultPrice = 1000000L

  val minStorageRent = 10000000L

  val feePercent = 1

  val coolingOffHeight: Int = 430000
  val INF = 1000000000L

  val longMax = 9223372036854775807L

  val bankScript =
    s"""{ // this box
       |  // R4: Number of stable-coins in circulation
       |  // R5: Number of reserve-coins in circulation
       |
       |  val isExchange = if (CONTEXT.dataInputs.size > 0) {
       |
       |    val dataInput = CONTEXT.dataInputs(0)
       |    val validDataInput = dataInput.tokens(0)._1 == oraclePoolNFT
       |
       |    val bankBoxIn = SELF
       |    val bankBoxOut = OUTPUTS(0)
       |
       |    val rateBox = dataInput
       |    val receiptBox = OUTPUTS(1)
       |
       |    val rate = rateBox.R4[Long].get / 100
       |
       |    val scCircIn = bankBoxIn.R4[Long].get
       |    val rcCircIn = bankBoxIn.R5[Long].get
       |    val bcReserveIn = bankBoxIn.value
       |
       |    val scTokensIn = bankBoxIn.tokens(0)._2
       |    val rcTokensIn = bankBoxIn.tokens(1)._2
       |
       |    val scCircOut = bankBoxOut.R4[Long].get
       |    val rcCircOut = bankBoxOut.R5[Long].get
       |    val bcReserveOut = bankBoxOut.value
       |
       |    val scTokensOut = bankBoxOut.tokens(0)._2
       |    val rcTokensOut = bankBoxOut.tokens(1)._2
       |
       |    val totalScIn = scTokensIn + scCircIn
       |    val totalScOut = scTokensOut + scCircOut
       |
       |    val totalRcIn = rcTokensIn + rcCircIn
       |    val totalRcOut = rcTokensOut + rcCircOut
       |
       |    val rcExchange = rcTokensIn != rcTokensOut
       |    val scExchange = scTokensIn != scTokensOut
       |
       |    val rcExchangeXorScExchange = (rcExchange || scExchange) && !(rcExchange && scExchange)
       |
       |    val circDelta = receiptBox.R4[Long].get
       |    val bcReserveDelta = receiptBox.R5[Long].get
       |
       |    val rcCircDelta = if (rcExchange) circDelta else 0L
       |    val scCircDelta = if (rcExchange) 0L else circDelta
       |
       |    val validDeltas = (scCircIn + scCircDelta == scCircOut) &&
       |                      (rcCircIn + rcCircDelta == rcCircOut) &&
       |                      (bcReserveIn + bcReserveDelta == bcReserveOut) &&
       |                      scCircOut >= 0 && rcCircOut >= 0
       |
       |    val coinsConserved = totalRcIn == totalRcOut && totalScIn == totalScOut
       |
       |    val tokenIdsConserved = bankBoxOut.tokens(0)._1 == bankBoxIn.tokens(0)._1 && // also ensures that at least one token exists
       |                            bankBoxOut.tokens(1)._1 == bankBoxIn.tokens(1)._1 && // also ensures that at least one token exists
       |                            bankBoxOut.tokens(2)._1 == bankBoxIn.tokens(2)._1    // also ensures that at least one token exists
       |
       |    val mandatoryRateConditions = rateBox.tokens(0)._1 == oraclePoolNFT
       |    val mandatoryBankConditions = bankBoxOut.value >= $minStorageRent &&
       |                                  bankBoxOut.propositionBytes == bankBoxIn.propositionBytes &&
       |                                  rcExchangeXorScExchange &&
       |                                  coinsConserved &&
       |                                  validDeltas &&
       |                                  tokenIdsConserved
       |
       |    // exchange equations
       |    val bcReserveNeededOut = scCircOut * rate
       |    val bcReserveNeededIn = scCircIn * rate
       |    val liabilitiesIn = max(min(bcReserveIn, bcReserveNeededIn), 0)
       |
       |    val maxReserveRatioPercent = if (HEIGHT > $coolingOffHeight) defaultMaxReserveRatioPercent else ${INF}L
       |
       |    val reserveRatioPercentOut = if (bcReserveNeededOut == 0) maxReserveRatioPercent else bcReserveOut * 100 / bcReserveNeededOut
       |
       |    val validReserveRatio = if (scExchange) {
       |      if (scCircDelta > 0) {
       |        reserveRatioPercentOut >= minReserveRatioPercent
       |      } else true
       |    } else {
       |      if (rcCircDelta > 0) {
       |        reserveRatioPercentOut <= maxReserveRatioPercent
       |      } else {
       |        reserveRatioPercentOut >= minReserveRatioPercent
       |      }
       |    }
       |
       |    val brDeltaExpected = if (scExchange) { // sc
       |      val liableRate = if (scCircIn == 0) ${longMax}L else liabilitiesIn / scCircIn
       |      val scNominalPrice = min(rate, liableRate)
       |      scNominalPrice * scCircDelta
       |    } else { // rc
       |      val equityIn = bcReserveIn - liabilitiesIn
       |      val equityRate = if (rcCircIn == 0) ${rcDefaultPrice}L else equityIn / rcCircIn
       |      val rcNominalPrice = if (equityIn == 0) ${rcDefaultPrice}L else equityRate
       |      rcNominalPrice * rcCircDelta
       |    }
       |
       |    val fee = brDeltaExpected * $feePercent / 100
       |
       |    val actualFee = if (fee < 0) {fee * -1} else fee
       |    // actualFee is always positive, irrespective of brDeltaExpected
       |
       |    val brDeltaExpectedWithFee = brDeltaExpected + actualFee
       |
       |    mandatoryRateConditions &&
       |    mandatoryBankConditions &&
       |    bcReserveDelta == brDeltaExpectedWithFee &&
       |    validReserveRatio &&
       |    validDataInput
       |  } else false
       |
       |  sigmaProp(isExchange)
       |}
       |""".stripMargin

}
