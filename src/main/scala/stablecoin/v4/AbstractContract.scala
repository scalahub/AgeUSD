package stablecoin.v4

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}

private[v4] abstract class AbstractContract {
  import scala.collection.mutable.{Map => MMap}
  val env = MMap[String, kiosk.ergo.KioskType[_]]()
  val script: String
  lazy val ergoTree = kiosk.script.ScriptUtil.compile(env.toMap, script)
  lazy val address = getStringFromAddress(getAddressFromErgoTree(ergoTree))
}
