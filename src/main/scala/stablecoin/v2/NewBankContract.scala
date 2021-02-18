package stablecoin.v2

import kiosk.script.ScriptUtil._
import kiosk.ergo._
import stablecoin.v2.Contracts._

private[v2] object NewBankContract extends AbstractContract {
  lazy val minReserveRatioPercent = 400 // percent
  lazy val maxReserveRatioPercent = 900 // percent  ********* THIS VALUE IS DIFFERENT *********

  env.setLong("minReserveRatioPercent", minReserveRatioPercent)
  env.setLong("maxReserveRatioPercent", maxReserveRatioPercent)
  env.setCollByte("oraclePoolNFT", oraclePoolNFT.decodeHex)
  env.setCollByte("updateNFT", updateNFT.decodeHex)

  override val script = bankScript
}
