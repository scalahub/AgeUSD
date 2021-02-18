package stablecoin.v4

import kiosk.script.ScriptUtil._
import kiosk.ergo._
import stablecoin.v4.Contracts._

private[v4] object BankContract extends AbstractContract {
  lazy val minReserveRatioPercent = 400 // percent
  lazy val defaultMaxReserveRatioPercent = 800 // percent

  env.setLong("minReserveRatioPercent", minReserveRatioPercent)
  env.setLong("defaultMaxReserveRatioPercent", defaultMaxReserveRatioPercent)
  env.setCollByte("oraclePoolNFT", oraclePoolNFT.decodeHex)

  override val script = bankScript
}
