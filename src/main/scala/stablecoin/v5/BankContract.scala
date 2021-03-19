package stablecoin.v5

import kiosk.script.ScriptUtil._
import kiosk.ergo._
import stablecoin.v5.Contracts._

private[v5] object BankContract extends AbstractContract {
  lazy val minReserveRatioPercent = 400 // percent
  lazy val defaultMaxReserveRatioPercent = 800 // percent

  env.setLong("minReserveRatioPercent", minReserveRatioPercent)
  env.setLong("defaultMaxReserveRatioPercent", defaultMaxReserveRatioPercent)
  env.setCollByte("oraclePoolNFT", oraclePoolNFT.decodeHex)
  env.setCollByte("updateNFT", updateNFT.decodeHex)

  override val script = bankScript
}
