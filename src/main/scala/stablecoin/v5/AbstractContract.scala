package stablecoin.v5

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}

private[v5] abstract class AbstractContract {
  import scala.collection.mutable.{Map => MMap}
  val env = MMap[String, kiosk.ergo.KioskType[_]]()
  val script: String
  lazy val ergoTree = kiosk.script.ScriptUtil.compile(env.toMap, script)
  lazy val address = getStringFromAddress(getAddressFromErgoTree(ergoTree))
}
