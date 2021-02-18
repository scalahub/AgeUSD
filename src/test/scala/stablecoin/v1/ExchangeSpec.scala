package stablecoin.v1


import kiosk.ergo.{DhtData, KioskBox, _}
import org.ergoplatform.appkit._
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import stablecoin.v1.BankContract.minStorageRent

class ExchangeSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {
  val changeAddress = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"
  val dummyTxId = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val dummyScript = "{sigmaProp(1 < 2)}"

  val scToken = "12caaacb51c89646fac9a3786eb98d0113bd57d68223ccc11754a4f67281daed"
  val rcToken = "22caaacb51c89646fac9a3786eb98d0113bd57d68223ccc11754a4f67281daed"
  val bankNFT = "32caaacb51c89646fac9a3786eb98d0113bd57d68223ccc11754a4f67281daed"

  val ergoClient = createMockedErgoClient(MockData(Nil, Nil))

  val fee = 1500000

  val largeValue = 100000000000L

  property("One complete exchange") {
    ergoClient.execute { implicit ctx: BlockchainContext =>
      val zerothBankBox = KioskBox(
        BankContract.address,
        value = BankContract.minStorageRent,
        registers = Array(KioskLong(0L), KioskLong(0L)),
        tokens = Array((scToken, largeValue), (rcToken, largeValue), (bankNFT, 1L))
      )

      // dummy custom input box for funding various transactions
      val dummyFundingBox = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(largeValue)
        .tokens(new ErgoToken(scToken, largeValue), new ErgoToken(rcToken, largeValue), new ErgoToken(bankNFT, 1))
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .build()
        .convertToInputWith(dummyTxId, 0)

      def getRateBox(rate: Long) = {
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(10000000000000L)
          .tokens(new ErgoToken(BankContract.oraclePoolNFT, 1))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
          .registers(KioskLong(rate).getErgoValue)
          .build()
          .convertToInputWith(dummyTxId, 0)
      }

      def getReceiptBox(delta: Long, bcDelta: Long, optTokenId: Option[String]) = KioskBox(
        changeAddress,
        value = BankContract.minStorageRent,
        registers = Array(KioskLong(delta), KioskLong(bcDelta)),
        tokens = optTokenId.map(tokenId => (tokenId, delta)).toArray
      )

      def getBankBoxOut(bankBoxIn: KioskBox, scCircDelta: Long, rcCircDelta: Long, bcReserveDelta: Long) = KioskBox(
        bankBoxIn.address,
        value = bankBoxIn.value + bcReserveDelta,
        registers = Array(
          KioskLong(bankBoxIn.registers(0).asInstanceOf[KioskLong].value + scCircDelta),
          KioskLong(bankBoxIn.registers(1).asInstanceOf[KioskLong].value + rcCircDelta)
        ),
        tokens = Array(
          (bankBoxIn.tokens(0)._1, bankBoxIn.tokens(0)._2 - scCircDelta),
          (bankBoxIn.tokens(1)._1, bankBoxIn.tokens(1)._2 - rcCircDelta),
          (bankBoxIn.tokens(2)._1, bankBoxIn.tokens(2)._2)
        )
      )

      def getOutBoxes(bankBoxIn: KioskBox, scCircDelta: Long, rcCircDelta: Long, bcReserveDelta: Long, optTokenId: Option[String]) =
        (
          getBankBoxOut(bankBoxIn, scCircDelta, rcCircDelta, bcReserveDelta),
          getReceiptBox(scCircDelta + rcCircDelta, bcReserveDelta, optTokenId),
        )

      // bootstrap the system; create the first bank box
      val bootstrapTx: SignedTransaction = kiosk.tx.TxUtil.createTx(
        inputBoxes = Array(dummyFundingBox),
        dataInputs = Array[InputBox](),
        boxesToCreate = Array(zerothBankBox),
        fee,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )

      /////////////////////////////////////////////////
      // Mint RC
      /////////////////////////////////////////////////
      val ergToUsdEpoch1 = 1000
      val rcMintDelta = 1000

      val (firstBankBox, firstReceiptBox) = getOutBoxes(
        zerothBankBox,
        scCircDelta = 0,
        rcCircDelta = rcMintDelta,
        StableCoin.getDeltaBcForDeltaRC(
          zerothBankBox.value - minStorageRent,
          zerothBankBox.registers(0).asInstanceOf[KioskLong].value,
          zerothBankBox.registers(1).asInstanceOf[KioskLong].value,
          rcMintDelta,
          ergToUsdEpoch1
        ),
        Some(rcToken)
      )

      val exchangeRcTx = kiosk.tx.TxUtil.createTx(
        inputBoxes = Array(bootstrapTx.getOutputsToSpend.get(0), dummyFundingBox),
        dataInputs = Array(getRateBox(ergToUsdEpoch1)),
        boxesToCreate = Array(firstBankBox, firstReceiptBox),
        fee,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )

      /////////////////////////////////////////////////
      // Mint SC
      /////////////////////////////////////////////////
      val ergToUsdEpoch2 = 1100
      val scMintDelta = 100

      val (secondBankBox, secondReceiptBox) = getOutBoxes(
        firstBankBox,
        scCircDelta = scMintDelta,
        rcCircDelta = 0,
        StableCoin.getDeltaBcForDeltaSC(
          firstBankBox.value - minStorageRent,
          firstBankBox.registers(0).asInstanceOf[KioskLong].value,
          scMintDelta,
          ergToUsdEpoch2
        ),
        Some(scToken)
      )

      val exchangeScTx = kiosk.tx.TxUtil.createTx(
        inputBoxes = Array(exchangeRcTx.getOutputsToSpend.get(0), dummyFundingBox),
        dataInputs = Array(getRateBox(ergToUsdEpoch2)),
        boxesToCreate = Array(secondBankBox, secondReceiptBox),
        fee,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )

      /////////////////////////////////////////////////
      // Redeem RC
      /////////////////////////////////////////////////
      val ergToUsdEpoch3 = 900
      val rcRedeemDelta = -10

      val (thirdBankBox, thirdReceiptBox) = getOutBoxes(
        secondBankBox,
        scCircDelta = 0,
        rcCircDelta = rcRedeemDelta,
        StableCoin.getDeltaBcForDeltaRC(
          secondBankBox.value - minStorageRent,
          secondBankBox.registers(0).asInstanceOf[KioskLong].value,
          secondBankBox.registers(1).asInstanceOf[KioskLong].value,
          rcRedeemDelta,
          ergToUsdEpoch3
        ),
        None
      )

      val redeemRcTx = kiosk.tx.TxUtil.createTx(
        inputBoxes = Array(exchangeScTx.getOutputsToSpend.get(0), dummyFundingBox),
        dataInputs = Array(getRateBox(ergToUsdEpoch3)),
        boxesToCreate = Array(thirdBankBox, thirdReceiptBox),
        fee,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )

      /////////////////////////////////////////////////
      // Redeem SC
      /////////////////////////////////////////////////
      val ergToUsdEpoch4 = 1100
      val scRedeemDelta = -100

      val (fourthBankBox, fourthReceiptBox) = getOutBoxes(
        thirdBankBox,
        scCircDelta = scRedeemDelta,
        rcCircDelta = 0,
        StableCoin.getDeltaBcForDeltaSC(
          thirdBankBox.value - minStorageRent,
          thirdBankBox.registers(0).asInstanceOf[KioskLong].value,
          scRedeemDelta,
          ergToUsdEpoch4
        ),
        None
      )

      kiosk.tx.TxUtil.createTx(
        inputBoxes = Array(redeemRcTx.getOutputsToSpend.get(0), dummyFundingBox),
        dataInputs = Array(getRateBox(ergToUsdEpoch4)),
        boxesToCreate = Array(fourthBankBox, fourthReceiptBox),
        fee,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )
    }
  }
}
