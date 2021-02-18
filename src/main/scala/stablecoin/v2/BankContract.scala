package stablecoin.v2

import kiosk.script.ScriptUtil._
import kiosk.ergo._
import stablecoin.v2.Contracts._

private[v2] object BankContract extends AbstractContract {
  lazy val minReserveRatioPercent = 400 // percent
  lazy val maxReserveRatioPercent = 800 // percent

  env.setLong("minReserveRatioPercent", minReserveRatioPercent)
  env.setLong("maxReserveRatioPercent", maxReserveRatioPercent)
  env.setCollByte("oraclePoolNFT", oraclePoolNFT.decodeHex)
  env.setCollByte("updateNFT", updateNFT.decodeHex)

  override val script = bankScript
}
