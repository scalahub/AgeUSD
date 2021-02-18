package stablecoin.v2

import kiosk.script.ScriptUtil._
import kiosk.ergo._
import Contracts._

private[v2] object UpdateContract extends AbstractContract {

  env.setCollByte("ballotTokenId", ballotTokenId.decodeHex)
  env.setCollByte("bankNFT", bankNFT.decodeHex)

  override lazy val script = updateScript
}
