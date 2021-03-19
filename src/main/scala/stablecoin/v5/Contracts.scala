package stablecoin.v5

private[v5] object Contracts {
  val oraclePoolNFT = "0fb1eca4646950743bc5a8c341c16871a0ad9b4077e3b276bf93855d51a042d1"

  val updateNFT = "77d14a018507949d1a88a631f76663e8e5101f57305dd5ebd319a41028d80456" // quantity 1
  val ballotTokenId = "dd26438230986cfe7305ad958451b69e55ad5ac37c8a355bfb08d810edd7a20f" // quantity 1000

  val bankNFT = "7bd873b8a886daa7a8bfacdad11d36aeee36c248aaf5779bcd8d41a13e4c1604" // quantity 1
  val scToken = "a908bf2be7e199014b45e421dc4adb846d8de95e37da87c7f97ac6fb8e863fa2" // quantity 10000000000000
  val rcToken = "b240daba6b5f9f9b6d4e6d7fc8b7c0423f1dfa28a883ec626a18b69be6c7590e" // quantity 10000000000000

  val rcDefaultPrice = 1000000

  val minStorageRent = 10000000L

  val feePercent = 1

  val coolingOffHeight: Int = 377770
  val INF = 1000000000L

  val longMax = Long.MaxValue

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
       |  sigmaProp(isExchange || INPUTS(0).tokens(0)._1 == updateNFT && CONTEXT.dataInputs.size == 0)
       |}
       |""".stripMargin

  val minVotes: Int = 5

  val ballotScript =
    s"""{ // This box (ballot box):
       |  // R4 the group element of the owner of the ballot token [GroupElement]
       |  // R5 dummy Int due to AOTC non-lazy evaluation (since bank box has Long at R5). Due to the line marked ****
       |  // R6 the box id of the update box [Coll[Byte]]
       |  // R7 the value voted for [Coll[Byte]]
       |  
       |  val pubKey = SELF.R4[GroupElement].get
       |  
       |  val index = INPUTS.indexOf(SELF, 0)
       |  
       |  val output = OUTPUTS(index)
       |  
       |  val isBasicCopy = output.R4[GroupElement].get == pubKey && 
       |                    output.propositionBytes == SELF.propositionBytes &&
       |                    output.tokens == SELF.tokens && 
       |                    output.value >= $minStorageRent
       |  
       |  sigmaProp(
       |    isBasicCopy && (
       |      proveDlog(pubKey) || (
       |         INPUTS(0).tokens(0)._1 == updateNFT && 
       |         output.value >= SELF.value
       |      )
       |    )
       |  )
       |}
       |""".stripMargin

  val updateScript =
    s"""{ // This box (update box):
       |  // Registers empty 
       |  // 
       |  // ballot boxes (Inputs)
       |  // R4 the pub key of voter [GroupElement] (not used here)
       |  // R5 dummy int due to AOTC non-lazy evaluation (from the line marked ****)
       |  // R6 the box id of this box [Coll[Byte]]
       |  // R7 the value voted for [Coll[Byte]]
       |
       |  // collect and update in one step
       |  val updateBoxOut = OUTPUTS(0) // copy of this box is the 1st output
       |  val validUpdateIn = SELF.id == INPUTS(0).id // this is 1st input
       |
       |  val bankBoxIn = INPUTS(1) // bank box is 2nd input
       |  val bankBoxOut = OUTPUTS(1) // copy of bank box is the 2nd output
       |  
       |  // compute the hash of the bank output box. This should be the value voted for
       |  val bankBoxOutHash = blake2b256(bankBoxOut.propositionBytes)
       |  
       |  val validBankIn = bankBoxIn.tokens.size == 3 && bankBoxIn.tokens(2)._1 == bankNFT
       |  val validBankOut = bankBoxIn.tokens == bankBoxOut.tokens &&
       |                     bankBoxIn.value == bankBoxOut.value &&
       |                     bankBoxIn.R4[Long].get == bankBoxOut.R4[Long].get &&
       |                     bankBoxIn.R5[Long].get == bankBoxOut.R5[Long].get 
       |
       |  
       |  val validUpdateOut = SELF.tokens == updateBoxOut.tokens && 
       |                       SELF.propositionBytes == updateBoxOut.propositionBytes &&
       |                       SELF.value >= updateBoxOut.value
       |
       |  def isValidBallot(b:Box) = {
       |    b.tokens.size > 0 && 
       |    b.tokens(0)._1 == ballotTokenId &&
       |    b.R6[Coll[Byte]].get == SELF.id && // ensure vote corresponds to this box ****
       |    b.R7[Coll[Byte]].get == bankBoxOutHash // check value voted for
       |  }
       |  
       |  val ballotBoxes = INPUTS.filter(isValidBallot)
       |  
       |  val votesCount = ballotBoxes.fold(0L, {(accum: Long, b: Box) => accum + b.tokens(0)._2})
       |  
       |  sigmaProp(validBankIn && validBankOut && validUpdateIn && validUpdateOut && votesCount >= $minVotes)
       |}
       |""".stripMargin

}
