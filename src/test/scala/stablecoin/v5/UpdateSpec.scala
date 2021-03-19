package stablecoin.v5

import kiosk.ErgoUtil
import kiosk.encoding.ScalaErgoConverters
import kiosk.encoding.ScalaErgoConverters.stringToGroupElement
import kiosk.script.ScriptUtil._
import kiosk.ergo._
import kiosk.ergo._
import kiosk.tx.TxUtil
import org.ergoplatform.appkit._
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scorex.crypto.hash.Blake2b256
import stablecoin.v5.Contracts._

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

      require(BankContract.address != NewBankContract.address)

      // dummy custom input box for funding various transactions
      val dummyFundingBox = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(100000000000L)
        .registers(KioskCollByte(Array(1)).getErgoValue)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .build()
        .convertToInputWith(dummyTxId, 0)

      object Voters {
        // define voters
        val addresses = Seq(
          "9eiuh5bJtw9oWDVcfJnwTm1EHfK5949MEm5DStc2sD1TLwDSrpx", // private key is 37cc5cb5b54f98f92faef749a53b5ce4e9921890d9fb902b4456957d50791bd0
          "9f9q6Hs7vXZSQwhbrptQZLkTx15ApjbEkQwWXJqD2NpaouiigJQ", // private key is 5878ae48fe2d26aa999ed44437cffd2d4ba1543788cff48d490419aef7fc149d
          "9fGp73EsRQMpFC7xaYD5JFy2abZeKCUffhDBNbQVtBtQyw61Vym", // private key is 3ffaffa96b2fd6542914d3953d05256cd505d4beb6174a2601a4e014c3b5a78e
        ).toArray

        val privateKey0 = "37cc5cb5b54f98f92faef749a53b5ce4e9921890d9fb902b4456957d50791bd0"
        val privateKey1 = "5878ae48fe2d26aa999ed44437cffd2d4ba1543788cff48d490419aef7fc149d"
        val privateKey2 = "3ffaffa96b2fd6542914d3953d05256cd505d4beb6174a2601a4e014c3b5a78e"

        val r4voter0 = KioskGroupElement(stringToGroupElement(ErgoUtil.addressToGroupElement(addresses(0))))
        val r4voter1 = KioskGroupElement(stringToGroupElement(ErgoUtil.addressToGroupElement(addresses(1))))
        val r4voter2 = KioskGroupElement(stringToGroupElement(ErgoUtil.addressToGroupElement(addresses(2))))

        val ballot0Box = KioskBox(BallotContract.address, value = 200000000, registers = Array(r4voter0), tokens = Array((Contracts.ballotTokenId, 2L)))
        val ballot1Box = KioskBox(BallotContract.address, value = 200000000, registers = Array(r4voter1), tokens = Array((Contracts.ballotTokenId, 2L)))
        val ballot2Box = KioskBox(BallotContract.address, value = 200000000, registers = Array(r4voter2), tokens = Array((Contracts.ballotTokenId, 1L)))
      }

      // old update box
      val updateOutBox = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(Contracts.minStorageRent)
        .tokens(new ErgoToken(Contracts.updateNFT, 1))
        .contract(ctx.newContract(ScalaErgoConverters.getAddressFromString(UpdateContract.address).script))
        .build()

      val updateBoxIn = updateOutBox.convertToInputWith(dummyTxId, 0)

      // old bank box
      val currentBankContract = ctx.newContract(ScalaErgoConverters.getAddressFromString(BankContract.address).script)

      val bankBoxIn = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(bcReserveIn)
        .tokens(new ErgoToken(Contracts.scToken, scTokensIn), new ErgoToken(Contracts.rcToken, rcTokensIn), new ErgoToken(Contracts.bankNFT, 1))
        .contract(currentBankContract)
        .registers(KioskLong(scCircIn).getErgoValue, KioskLong(rcCircIn).getErgoValue)
        .build()
        .convertToInputWith(dummyTxId, 0)

      // value to vote for; hash of new bank box script
      val valueVotedFor = KioskCollByte(Blake2b256(NewBankContract.ergoTree.bytes))

      val ballot0BoxToCreate = Voters.ballot0Box.copy(
        registers = Array(
          Voters.ballot0Box.registers(0),
          KioskInt(0), // dummy value due to AOTC non-lazy eval bug
          KioskCollByte(updateBoxIn.getId.getBytes),
          valueVotedFor
        )
      )

      val ballot1BoxToCreate = Voters.ballot1Box.copy(
        registers = Array(
          Voters.ballot1Box.registers(0),
          KioskInt(0), // dummy value due to AOTC non-lazy eval bug
          KioskCollByte(updateBoxIn.getId.getBytes),
          valueVotedFor
        )
      )

      val ballot2BoxToCreate = Voters.ballot2Box.copy(
        registers = Array(
          Voters.ballot2Box.registers(0),
          KioskInt(0), // dummy value due to AOTC non-lazy eval bug
          KioskCollByte(updateBoxIn.getId.getBytes),
          valueVotedFor
        )
      )

      // create ballots
      val ballot0 = TxUtil.createTx(
        inputBoxes = Array(Voters.ballot0Box.toInBox(dummyTxId, 0), dummyFundingBox),
        dataInputs = Array(),
        boxesToCreate = Array(ballot0BoxToCreate),
        fee,
        changeAddress,
        proveDlogSecrets = Array[String](Voters.privateKey0),
        Array[DhtData](),
        false
      ).getOutputsToSpend.get(0)

      val ballot1 = TxUtil.createTx(
        inputBoxes = Array(Voters.ballot1Box.toInBox(dummyTxId, 0), dummyFundingBox),
        dataInputs = Array(),
        boxesToCreate = Array(ballot1BoxToCreate),
        fee,
        changeAddress,
        proveDlogSecrets = Array[String](Voters.privateKey1),
        Array[DhtData](),
        false
      ).getOutputsToSpend.get(0)

      val ballot2 = TxUtil.createTx(
        inputBoxes = Array(Voters.ballot2Box.toInBox(dummyTxId, 0), dummyFundingBox),
        dataInputs = Array(),
        boxesToCreate = Array(ballot2BoxToCreate),
        fee,
        changeAddress,
        proveDlogSecrets = Array[String](Voters.privateKey2),
        Array[DhtData](),
        false
      ).getOutputsToSpend.get(0)

      // voting should fail with wrong private key
      the[AssertionError] thrownBy {
        TxUtil.createTx(
          inputBoxes = Array(Voters.ballot2Box.toInBox(dummyTxId, 0), dummyFundingBox),
          dataInputs = Array(),
          boxesToCreate = Array(ballot2BoxToCreate),
          fee,
          changeAddress,
          proveDlogSecrets = Array[String](Voters.privateKey0),
          Array[DhtData](),
          false
        )
      } should have message "assertion failed: Tree root should be real but was UnprovenSchnorr(ProveDlog((6357df04d57e9d4d0564e217a0e7e6d201df72787b5f89203744e9384402378d,567526ec80c82cca6bbb6d5324e65c6cf9b39704ad91e3eca93f84bf481bf38a,1)),None,None,None,true,0)"

      // new update box
      val updateBoxOut = KioskBox(
        UpdateContract.address,
        value = updateBoxIn.getValue,
        registers = Array(),
        tokens = Array(Contracts.updateNFT -> 1L)
      )

      // new bank box
      val bankBoxOut = KioskBox(
        NewBankContract.address,
        value = bankBoxIn.getValue,
        registers = Array(KioskLong(scCircIn), KioskLong(rcCircIn)),
        tokens = Array(Contracts.scToken -> scTokensIn, Contracts.rcToken -> rcTokensIn, Contracts.bankNFT -> 1L)
      )

      // Should succeed with sufficient votes (5)
      TxUtil.createTx(
        inputBoxes = Array(updateBoxIn, bankBoxIn, ballot0, ballot1, ballot2, dummyFundingBox),
        dataInputs = Array(),
        boxesToCreate = Array(updateBoxOut, bankBoxOut, ballot0BoxToCreate, ballot1BoxToCreate, ballot2BoxToCreate),
        fee,
        changeAddress,
        proveDlogSecrets = Array[String](),
        Array[DhtData](),
        false
      )

      // should fail for invalid output bank box script
      the[Exception] thrownBy {
        TxUtil.createTx(
          inputBoxes = Array(updateBoxIn, bankBoxIn, ballot0, ballot1, ballot2, dummyFundingBox),
          dataInputs = Array(),
          boxesToCreate = Array(updateBoxOut, bankBoxOut.copy(address = BankContract.address), ballot0BoxToCreate, ballot1BoxToCreate, ballot2BoxToCreate),
          fee,
          changeAddress,
          proveDlogSecrets = Array[String](),
          Array[DhtData](),
          false
        )
      } should have message "Script reduced to false"

      // should fail for invalid input update box Id
      val invalidUpdateBoxIn = updateOutBox.convertToInputWith(dummyTxId, 1) // different outputIndex

      the[Exception] thrownBy {
        TxUtil.createTx(
          inputBoxes = Array(invalidUpdateBoxIn, bankBoxIn, ballot0, ballot1, ballot2, dummyFundingBox),
          dataInputs = Array(),
          boxesToCreate = Array(updateBoxOut, bankBoxOut, ballot0BoxToCreate, ballot1BoxToCreate, ballot2BoxToCreate),
          fee,
          changeAddress,
          proveDlogSecrets = Array[String](),
          Array[DhtData](),
          false
        )
      } should have message "Script reduced to false"

      // Should fail when having insufficient votes (4)
      the[Exception] thrownBy {
        TxUtil.createTx(
          inputBoxes = Array(updateBoxIn, bankBoxIn, ballot0, ballot1, dummyFundingBox),
          dataInputs = Array(),
          boxesToCreate = Array(updateBoxOut, bankBoxOut, ballot0BoxToCreate, ballot1BoxToCreate),
          fee,
          changeAddress,
          proveDlogSecrets = Array[String](),
          Array[DhtData](),
          false
        )
      } should have message "Script reduced to false"

      ///////////////////////////////////////////////
      ///////////////////////////////////////////////
      ///////////////////////////////////////////////
      // dummy custom input box for funding various transactions
//      val dummyFundingBox = ctx
//        .newTxBuilder()
//        .outBoxBuilder
//        .value(fee)
//        .registers(KioskCollByte(Array(1)).getErgoValue)
//        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
//        .build()
//        .convertToInputWith(dummyTxId, 0)

      // old bank box
//      val bankBoxIn = ctx
//        .newTxBuilder()
//        .outBoxBuilder
//        .value(bcReserveIn)
//        .tokens(
//          new ErgoToken(scToken, scTokensIn),
//          new ErgoToken(rcToken, rcTokensIn),
//          new ErgoToken(bankNFT, 1)
//        )
//        .contract(ctx.newContract(ScalaErgoConverters.getAddressFromString(BankContract.address).script))
//        .registers(KioskLong(scCircIn).getErgoValue, KioskLong(rcCircIn).getErgoValue)
//        .build()
//        .convertToInputWith(dummyTxId, 0)

      // new bank box
//      val bankBoxOut = KioskBox(
//        NewBankContract.address,
//        value = bankBoxIn.getValue,
//        registers = Array(KioskLong(scCircIn), KioskLong(rcCircIn)),
//        tokens = Array((scToken, scTokensIn), (rcToken, rcTokensIn), (bankNFT, 1L))
//      )
//
//      val updatedBankScriptHash = KioskCollByte(Blake2b256(NewBankContract.ergoTree.bytes))
//
//      // old update box
//      val updateBoxIn = ctx
//        .newTxBuilder()
//        .outBoxBuilder
//        .value(10000000)
//        .tokens(new ErgoToken(updateNFT, 1))
//        .contract(ctx.newContract(ScalaErgoConverters.getAddressFromString(UpdateContract.address).script))
//        .registers(updatedBankScriptHash.getErgoValue)
//        .build()
//        .convertToInputWith(dummyTxId, 0)
//
//      // new update box
//      val updateBoxOut = KioskBox(
//        UpdateContract.address,
//        value = updateBoxIn.getValue,
//        registers = Array(updatedBankScriptHash),
//        tokens = Array((updateNFT, 1L))
//      )
//
//      kiosk.tx.TxUtil.createTx(
//        inputBoxes = Array(updateBoxIn, bankBoxIn, dummyFundingBox),
//        dataInputs = Array(),
//        boxesToCreate = Array(updateBoxOut, bankBoxOut),
//        fee,
//        changeAddress,
//        Array[String](),
//        Array[DhtData](),
//        false
//      )
    }
  }
}
