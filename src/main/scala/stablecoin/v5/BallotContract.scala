package stablecoin.v5

import kiosk.ergo._
import kiosk.script.ScriptUtil._
import stablecoin.v5.Contracts.{ballotScript, updateNFT}

private[v5] object BallotContract extends AbstractContract {
  env.setCollByte("updateNFT", updateNFT.decodeHex)
  override val script = ballotScript
}
