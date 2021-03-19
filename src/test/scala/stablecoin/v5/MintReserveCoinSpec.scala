package stablecoin.v5

import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo.{DhtData, KioskBox, KioskLong}
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, ErgoToken, HttpClientTesting}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import Contracts._

class MintReserveCoinSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {
  val ergoClient = createMockedErgoClient(MockData(Nil, Nil))

  val changeAddress = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"
  val dummyTxId = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val dummyScript = "{sigmaProp(1 < 2)}"

  val INF = 100000000000L

  property("Mock Mint ReserveCoin") {
    ergoClient.execute { implicit ctx: BlockchainContext =>
      // dummy custom input box for funding various transactions
      val dummyFundingBox = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(100000000000L)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .build()
        .convertToInputWith(dummyTxId, 0)

      def getRateBox(rate: Long) = {
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(10000000L)
          .tokens(new ErgoToken(oraclePoolNFT, 1))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
          .registers(KioskLong(rate).getErgoValue)
          .build()
          .convertToInputWith(dummyTxId, 0)
      }

      def getReceiptBox(delta: Long, bcDelta: Long, optTokenId: Option[String]) = KioskBox(
        changeAddress,
        value = minStorageRent,
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

      /////////////////////////////////////////////////
      // Mint RC
      /////////////////////////////////////////////////
      val nanoErgsPerUsd = 2667918809L
      val rcMintDelta = 50L

      val scTokensIn = 9999999999998L
      val rcTokensIn = 9999999969020L
      val bankNanoErgsIn = 3339L

      val scCircIn = 2L
      val rcCircIn = 30980

      val fakeInputBankBox = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(bankNanoErgsIn)
        .tokens(
          new ErgoToken(scToken, scTokensIn),
          new ErgoToken(rcToken, rcTokensIn),
          new ErgoToken(bankNFT, 1)
        )
        .contract(ctx.newContract(ScalaErgoConverters.getAddressFromString(BankContract.address).script))
        .registers(KioskLong(scCircIn).getErgoValue, KioskLong(rcCircIn).getErgoValue)
        .build()
        .convertToInputWith(dummyTxId, 0)

      val bankBoxIn = KioskBox(
        BankContract.address,
        value = bankNanoErgsIn,
        registers = Array(KioskLong(scCircIn), KioskLong(rcCircIn)),
        tokens = Array((scToken, scTokensIn), (rcToken, rcTokensIn), (bankNFT, 1L))
      )

      val bcReserveIn = bankNanoErgsIn - minStorageRent

      val bcDelta = StableCoin.getDeltaBcForDeltaRC(
        bcReserveIn,
        scCircIn,
        rcCircIn,
        rcMintDelta,
        nanoErgsPerUsd / 100
      )

      val nanoErgsDelta = bcDelta

      val (bankBoxOut, receiptBox) = getOutBoxes(
        bankBoxIn,
        scCircDelta = 0,
        rcCircDelta = rcMintDelta,
        bcReserveDelta = nanoErgsDelta,
        Some(rcToken)
      )

      println(s"bankNanoErgsIn $bankNanoErgsIn")
      println(s"bcReserveIn $bcReserveIn")
      println(s"bcDelta $bcDelta")
      println(s"bankNanoErgsIn ${bankBoxIn.value}")
      println(s"bankNanoErgsOut ${bankBoxOut.value}")
      println(s"rcMintDelta $rcMintDelta")
      println(s"r5receipt ${receiptBox.registers(1).asInstanceOf[KioskLong].value}")

      val exchangeRcTx = kiosk.tx.TxUtil.createTx(
        inputBoxes = Array(fakeInputBankBox, dummyFundingBox),
        dataInputs = Array(getRateBox(nanoErgsPerUsd)),
        boxesToCreate = Array(bankBoxOut, receiptBox),
        2000000,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )

      println(exchangeRcTx.toJson(false))
    }
  }
}
