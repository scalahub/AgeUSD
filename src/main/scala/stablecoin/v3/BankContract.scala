package stablecoin.v3

import kiosk.script.ScriptUtil._
import kiosk.ergo._
import stablecoin.v3.Contracts._

private[v3] object BankContract extends AbstractContract {
  lazy val minReserveRatioPercent = 400 // percent
  lazy val defaultMaxReserveRatioPercent = 800 // percent

  env.setLong("minReserveRatioPercent", minReserveRatioPercent)
  env.setLong("defaultMaxReserveRatioPercent", defaultMaxReserveRatioPercent)
  env.setCollByte("oraclePoolNFT", oraclePoolNFT.decodeHex)
  env.setCollByte("updateNFT", updateNFT.decodeHex)

  override val script = bankScript
}
