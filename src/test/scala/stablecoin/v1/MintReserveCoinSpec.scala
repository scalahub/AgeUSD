package stablecoin.v1


import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo.{DhtData, KioskBox, KioskLong}
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, ErgoToken, HttpClientTesting}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import stablecoin.v1.BankContract.minStorageRent

class MintReserveCoinSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {
  val ergoClient = createMockedErgoClient(MockData(Nil, Nil))

  val changeAddress = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"
  val dummyTxId = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val dummyScript = "{sigmaProp(1 < 2)}"

  // Token IDs
  val scToken = "2d554219a80c011cc51509e34fa4950965bb8e01de4d012536e766c9ca08bc2c"
  val rcToken = "bcd5db3a2872f279ef89edaa51a9344a6095ea1f03396874b695b5ba95ff602e"
  val bankNFT = "9f90c012e03bf99397e363fb1571b7999941e0862a217307e3467ee80cf53af7"

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

      /////////////////////////////////////////////////
      // Mint RC
      /////////////////////////////////////////////////
      val nanoErgsPerUsd = 357637000L
      val rcMintDelta = 700000L /// this value was too high in the mint action

      val scTokensIn = 99999999999L
      val rcTokensIn = 99995620000L
      val bcReserveIn = 2518217000L

      val scCircIn = 1L
      val rcCircIn = 4380000L

      val fakeInputBankBox = ctx
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

      val bankBoxIn = KioskBox(
        BankContract.address,
        value = bcReserveIn,
        registers = Array(KioskLong(scCircIn), KioskLong(rcCircIn)),
        tokens = Array((scToken, scTokensIn), (rcToken, rcTokensIn), (bankNFT, 1L))
      )

      val nanoErgsDelta = StableCoin.getDeltaBcForDeltaRC(
        bcReserveIn - minStorageRent,
        scCircIn,
        rcCircIn,
        rcMintDelta,
        nanoErgsPerUsd
      )
      val (bankBoxOut, receiptBox) = getOutBoxes(
        bankBoxIn,
        scCircDelta = 0,
        rcCircDelta = rcMintDelta,
        bcReserveDelta = nanoErgsDelta,
        Some(rcToken)
      )

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
    }
  }
}
/*
{
  "tx" : {
    "id" : "c16f7f07871f6c65796fac745fb7928a90cf4012f3722d0de0ac0462885d8b7a",
    "inputs" : [
      {
        "boxId" : "8d1f7954425144783228b77d4c10554b6149cace5c8eea4015fcd210c3c65636",
        "extension" : {

        }
      },
      {
        "boxId" : "81a0207b9cf6d5c111b3af81269bd0110c544d236bfbe9a7981de99418414470",
        "extension" : {

        }
      }
    ],
    "dataInputs" : [
      {
        "boxId" : "9e289deab858c3f14c7056e568cfa1026c01242d151a94b165268b650e9ac966"
      }
    ],
    "outputs" : [
      {
        "boxId" : "62637197009bfb717a807bee12802f0b5412eb747618e0057cb1127726b05853",
        "value" : 7428217000,
        "ergoTree" : "101f0400040004020402040004000402050005000580dac4090580dac409050005c00c05c80104000e20b662db51cf2dc39f110a021c2a31c74f0a1a18ffffbf73e8a051a7b8c0f09ebc0580dac40904040404050005feffffffffffffffff01050005e807050005e807050005a0060101050005c00c05a006d81ed601b2db6501fe730000d602b2a5730100d603c17202d604db6308a7d605b27204730200d6068c720502d607db63087202d608b27207730300d6098c720802d60a9472067209d60bb27204730400d60c8c720b02d60db27207730500d60e8c720d02d60f94720c720ed610e4c6a70505d611e4c672020505d612e4c6a70405d613e4c672020405d614b2a5730600d615e4c672140405d61695720a73077215d61795720a72157308d61899c1a77309d619e4c672140505d61a997203730ad61be4c672010405d61ca172189c7212721bd61d9c7213721bd61e9593721d730b730c9d9c721a730d721dd1ededed938cb2db63087201730e0001730fedededed9272037310edec720a720fefed720a720fed939a720672109a72097211939a720c72129a720e7213eded939a721272167213939a721072177211939a72187219721aeded938c720d018c720b01938c7208018c720501938cb27207731100018cb272047312000193721995720f9ca1721b95937212731373149d721c72127216d801d61f997218721c9c9593721f7315731695937210731773189d721f7210721795720f95917216731992721e731a731b95917217731c90721e731d92721e731e",
        "assets" : [
          {
            "tokenId" : "2d554219a80c011cc51509e34fa4950965bb8e01de4d012536e766c9ca08bc2c",
            "amount" : 99999999999
          },
          {
            "tokenId" : "bcd5db3a2872f279ef89edaa51a9344a6095ea1f03396874b695b5ba95ff602e",
            "amount" : 99985620000
          },
          {
            "tokenId" : "9f90c012e03bf99397e363fb1571b7999941e0862a217307e3467ee80cf53af7",
            "amount" : 1
          }
        ],
        "additionalRegisters" : {
          "R4" : "0502",
          "R5" : "05c0afdb0d"
        },
        "creationHeight" : 350512,
        "transactionId" : "c16f7f07871f6c65796fac745fb7928a90cf4012f3722d0de0ac0462885d8b7a",
        "index" : 0
      },
      {
        "boxId" : "68615ffc711e3dbffe0d2b0821bd9fd9324f97d961e83fc0408d5276a7e613de",
        "value" : 57659535574,
        "ergoTree" : "0008cd03f1102eb87a4166bf9fbd6247d087e92e1412b0e819dbb5fbc4e716091ec4e4ec",
        "assets" : [
          {
            "tokenId" : "bcd5db3a2872f279ef89edaa51a9344a6095ea1f03396874b695b5ba95ff602e",
            "amount" : 10000000
          }
        ],
        "additionalRegisters" : {
          "R5" : "05809ec5ca24",
          "R4" : "0580dac409"
        },
        "creationHeight" : 350512,
        "transactionId" : "c16f7f07871f6c65796fac745fb7928a90cf4012f3722d0de0ac0462885d8b7a",
        "index" : 1
      },
      {
        "boxId" : "0fbd03352d494c8c9411ecb3430baa94ef07027848a47e622df203584728389d",
        "value" : 2000000,
        "ergoTree" : "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304",
        "assets" : [
        ],
        "additionalRegisters" : {

        },
        "creationHeight" : 350512,
        "transactionId" : "c16f7f07871f6c65796fac745fb7928a90cf4012f3722d0de0ac0462885d8b7a",
        "index" : 2
      }
    ]
  }
}
 */
