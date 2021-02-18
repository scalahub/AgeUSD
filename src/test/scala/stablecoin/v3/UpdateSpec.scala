package stablecoin.v3

import kiosk.encoding.ScalaErgoConverters
import kiosk.script.ScriptUtil._
import kiosk.ergo._
import kiosk.ergo._
import org.ergoplatform.appkit._
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scorex.crypto.hash.Blake2b256
import stablecoin.v3.Contracts._

class UpdateSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {
  /*
  In v0.2, the bank box has two spending paths
  1. Exchange
  2. Update

  This class tests the Update path
   */
  val changeAddress = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"
  val dummyTxId = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val dummyScript = "sigmaProp(true)"

  val ergoClient = createMockedErgoClient(MockData(Nil, Nil))

  val fee = 1500000

  property("Update") {
    ergoClient.execute { implicit ctx: BlockchainContext =>
      val scTokensIn = 99999999999L
      val rcTokensIn = 99995620000L
      val bcReserveIn = 2518217000L

      val scCircIn = 1L
      val rcCircIn = 4380000L

      // dummy custom input box for funding various transactions
      val dummyFundingBox = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(fee)
        .registers(KioskCollByte(Array(1)).getErgoValue)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .build()
        .convertToInputWith(dummyTxId, 0)

      // old bank box
      val bankBoxIn = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(bcReserveIn)
        .tokens(
          new ErgoToken(scToken, scTokensIn),
          new ErgoToken(rcToken, rcTokensIn),
          new ErgoToken(bankNFT, 1)
        )
        .contract(ctx.newContract(ScalaErgoConverters.getAddressFromString(BankContract.address).script))
        .registers(KioskLong(scCircIn).getErgoValue, KioskLong(rcCircIn).getErgoValue)
        .build()
        .convertToInputWith(dummyTxId, 0)

      // new bank box
      val bankBoxOut = KioskBox(
        NewBankContract.address,
        value = bankBoxIn.getValue,
        registers = Array(KioskLong(scCircIn), KioskLong(rcCircIn)),
        tokens = Array((scToken, scTokensIn), (rcToken, rcTokensIn), (bankNFT, 1L))
      )

      val updatedBankScriptHash = KioskCollByte(Blake2b256(NewBankContract.ergoTree.bytes))

      // old update box
      val updateBoxIn = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(10000000)
        .tokens(new ErgoToken(updateNFT, 1))
        .contract(ctx.newContract(ScalaErgoConverters.getAddressFromString(UpdateContract.address).script))
        .registers(updatedBankScriptHash.getErgoValue)
        .build()
        .convertToInputWith(dummyTxId, 0)

      // new update box
      val updateBoxOut = KioskBox(
        UpdateContract.address,
        value = updateBoxIn.getValue,
        registers = Array(updatedBankScriptHash),
        tokens = Array((updateNFT, 1L))
      )

      kiosk.tx.TxUtil.createTx(
        inputBoxes = Array(updateBoxIn, bankBoxIn, dummyFundingBox),
        dataInputs = Array(),
        boxesToCreate = Array(updateBoxOut, bankBoxOut),
        fee,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )
    }
  }
}
